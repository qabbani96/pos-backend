package com.pos.sale.service;

import com.pos.auth.entity.User;
import com.pos.auth.repository.UserRepository;
import com.pos.common.exception.BusinessException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.item.entity.Item;
import com.pos.item.repository.ItemRepository;
import com.pos.sale.dto.SaleItemRequest;
import com.pos.sale.dto.SaleRequest;
import com.pos.sale.dto.SaleResponse;
import com.pos.sale.entity.Sale;
import com.pos.sale.entity.SaleItem;
import com.pos.sale.repository.SaleItemRepository;
import com.pos.sale.repository.SaleRepository;
import com.pos.shop.entity.Shop;
import com.pos.shopstock.entity.ShopStock;
import com.pos.shopstock.repository.ShopStockRepository;
import com.pos.shopstock.service.ShopStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository       saleRepository;
    private final SaleItemRepository   saleItemRepository;
    private final ItemRepository       itemRepository;
    private final UserRepository       userRepository;
    private final ShopStockRepository  shopStockRepository;
    private final ShopStockService     shopStockService;

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SaleResponse findById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + id));
        sale.setItems(saleItemRepository.findBySaleId(id));
        return SaleResponse.from(sale);
    }

    @Transactional(readOnly = true)
    public Page<SaleResponse> findAll(Long cashierId, LocalDateTime from,
                                      LocalDateTime to, Pageable pageable) {
        return saleRepository.findAll(cashierId, from, to, pageable)
                .map(sale -> {
                    sale.setItems(saleItemRepository.findBySaleId(sale.getId()));
                    return SaleResponse.from(sale);
                });
    }

    // ── Core sale processing ─────────────────────────────────────────────────

    /**
     * Processes a sale atomically:
     *  1. Identify cashier and their shop
     *  2. Resolve all items and validate they are active
     *  3. Validate shop stock for every line item up front (fail fast)
     *  4. Create the Sale + SaleItem records (with name/price snapshots)
     *  5. Deduct from ShopStock and record SALE movements
     *
     * The entire method runs in one transaction — any failure rolls back everything.
     */
    @Transactional
    public SaleResponse processSale(SaleRequest request) {
        User cashier = resolveCurrentUser();
        Shop shop    = resolveShop(cashier);

        // Step 1 — resolve items (single IN query)
        Map<Long, Item> itemMap = resolveItems(request.items());

        // Step 2 — validate shop stock for all lines before writing anything
        validateShopStock(shop, request.items(), itemMap);

        // Step 3 — build sale
        Sale sale = Sale.builder()
                .cashier(cashier)
                .shop(shop)
                .note(request.note())
                .status(Sale.SaleStatus.COMPLETED)
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<SaleItem> saleItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (SaleItemRequest line : request.items()) {
            Item item = itemMap.get(line.itemId());
            BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(line.quantity()));

            saleItems.add(SaleItem.builder()
                    .sale(sale)
                    .item(item)
                    .itemName(item.getName())
                    .unitPrice(item.getPrice())
                    .quantity(line.quantity())
                    .subtotal(subtotal)
                    .build());

            total = total.add(subtotal);
        }

        sale.setTotalAmount(total);
        sale.setItems(saleItems);
        Sale saved = saleRepository.save(sale);  // cascades to sale_items

        // Step 4 — deduct from shop stock (inside same transaction)
        String saleRef = "SALE-" + saved.getId();
        for (SaleItemRequest line : request.items()) {
            shopStockService.deductForSale(shop, itemMap.get(line.itemId()), line.quantity(), saleRef);
        }

        log.info("action=sale_completed, saleId={}, shop={}, cashier={}, total={}, items={}",
                saved.getId(), shop != null ? shop.getName() : "none",
                cashier.getUsername(), total, saleItems.size());

        return SaleResponse.from(saved);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Map<Long, Item> resolveItems(List<SaleItemRequest> lines) {
        List<Long> ids = lines.stream().map(SaleItemRequest::itemId).toList();
        Map<Long, Item> itemMap = itemRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Item::getId, i -> i));

        for (Long id : ids) {
            if (!itemMap.containsKey(id)) {
                throw new ResourceNotFoundException("Item not found: " + id);
            }
            if (!Boolean.TRUE.equals(itemMap.get(id).getActive())) {
                throw new BusinessException("ITEM_INACTIVE", "Item is not active: " + itemMap.get(id).getName());
            }
        }
        return itemMap;
    }

    /**
     * Validates stock against the shop's ShopStock records.
     * Throws BusinessException on first insufficient item.
     */
    private void validateShopStock(Shop shop, List<SaleItemRequest> lines, Map<Long, Item> itemMap) {
        if (shop == null) {
            // Fallback — cashier has no shop assigned (legacy / misconfiguration)
            // We'll still allow the sale but skip stock validation.
            // In production, every CASHIER must have a shop.
            return;
        }

        for (SaleItemRequest line : lines) {
            Item item = itemMap.get(line.itemId());
            ShopStock shopStock = shopStockRepository
                    .findByShopIdAndItemId(shop.getId(), item.getId())
                    .orElseThrow(() -> new BusinessException("NO_SHOP_STOCK",
                            "Item '" + item.getName() + "' has not been transferred to shop: " + shop.getName()));

            if (shopStock.getQuantity() < line.quantity()) {
                throw new BusinessException("INSUFFICIENT_SHOP_STOCK",
                        "Insufficient shop stock for '" + item.getName()
                        + "'. Available: " + shopStock.getQuantity()
                        + ", Requested: " + line.quantity());
            }
        }
    }

    private User resolveCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Cashier not found: " + username));
    }

    /**
     * Returns the shop assigned to the cashier.
     * Returns null if the cashier has no shop (legacy/admin sales).
     */
    private Shop resolveShop(User cashier) {
        return cashier.getShop();
    }
}
