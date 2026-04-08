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
import com.pos.stock.service.StockService;
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

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final StockService stockService;

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SaleResponse findById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + id));
        // Eagerly load items for the response
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
     *  1. Resolve all items and validate they are active
     *  2. Validate stock for every line item up front (fail fast)
     *  3. Create the Sale + SaleItem records (with name/price snapshots)
     *  4. Deduct stock and record movements via StockService
     *
     * The entire method runs in one transaction — any failure rolls back everything.
     */
    @Transactional
    public SaleResponse processSale(SaleRequest request) {
        User cashier = resolveCurrentUser();

        // Step 1 — resolve items (single query per unique itemId)
        Map<Long, Item> itemMap = resolveItems(request.items());

        // Step 2 — stock pre-validation (fail before writing anything)
        validateStock(request.items(), itemMap);

        // Step 3 — build sale
        Sale sale = Sale.builder()
                .cashier(cashier)
                .note(request.note())
                .status(Sale.SaleStatus.COMPLETED)
                .totalAmount(BigDecimal.ZERO)   // computed below
                .build();

        List<SaleItem> saleItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (SaleItemRequest line : request.items()) {
            Item item = itemMap.get(line.itemId());
            BigDecimal subtotal = item.getPrice()
                    .multiply(BigDecimal.valueOf(line.quantity()));

            SaleItem saleItem = SaleItem.builder()
                    .sale(sale)
                    .item(item)
                    .itemName(item.getName())           // snapshot
                    .unitPrice(item.getPrice())         // snapshot
                    .quantity(line.quantity())
                    .subtotal(subtotal)
                    .build();

            saleItems.add(saleItem);
            total = total.add(subtotal);
        }

        sale.setTotalAmount(total);
        sale.setItems(saleItems);
        Sale saved = saleRepository.save(sale);  // cascades to sale_items

        // Step 4 — deduct stock (inside same transaction)
        String saleRef = "SALE-" + saved.getId();
        for (SaleItemRequest line : request.items()) {
            stockService.deductForSale(itemMap.get(line.itemId()), line.quantity(), saleRef);
        }

        log.info("action=sale_completed, saleId={}, cashier={}, total={}, items={}",
                saved.getId(), cashier.getUsername(), total, saleItems.size());

        return SaleResponse.from(saved);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Loads all items in a single IN query and validates each is active.
     */
    private Map<Long, Item> resolveItems(List<SaleItemRequest> lines) {
        List<Long> ids = lines.stream().map(SaleItemRequest::itemId).toList();
        Map<Long, Item> itemMap = itemRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Item::getId, i -> i));

        for (Long id : ids) {
            if (!itemMap.containsKey(id)) {
                throw new ResourceNotFoundException("Item not found: " + id);
            }
            if (!Boolean.TRUE.equals(itemMap.get(id).getActive())) {
                throw new BusinessException("ITEM_INACTIVE",
                        "Item is not active: " + itemMap.get(id).getName());
            }
        }
        return itemMap;
    }

    /**
     * Validates stock for all lines before any write occurs.
     * This avoids partial stock deductions when the cart has multiple items.
     */
    private void validateStock(List<SaleItemRequest> lines, Map<Long, Item> itemMap) {
        for (SaleItemRequest line : lines) {
            Item item = itemMap.get(line.itemId());
            var stockResponse = stockService.findByItemId(item.getId());
            if (stockResponse.quantity() < line.quantity()) {
                throw new BusinessException("INSUFFICIENT_STOCK",
                        "Insufficient stock for '" + item.getName()
                        + "'. Available: " + stockResponse.quantity()
                        + ", Requested: " + line.quantity());
            }
        }
    }

    private User resolveCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Cashier not found: " + username));
    }
}
