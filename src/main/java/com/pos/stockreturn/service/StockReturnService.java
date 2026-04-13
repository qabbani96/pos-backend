package com.pos.stockreturn.service;

import com.pos.auth.entity.User;
import com.pos.auth.repository.UserRepository;
import com.pos.common.exception.BusinessException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.item.entity.Item;
import com.pos.item.repository.ItemRepository;
import com.pos.shop.entity.Shop;
import com.pos.shop.service.ShopService;
import com.pos.shopstock.service.ShopStockService;
import com.pos.stock.entity.Stock;
import com.pos.stock.entity.StockMovement;
import com.pos.stock.entity.StockMovement.MovementType;
import com.pos.stock.repository.StockMovementRepository;
import com.pos.stock.repository.StockRepository;
import com.pos.stockreturn.dto.StockReturnRequest;
import com.pos.stockreturn.dto.StockReturnResponse;
import com.pos.stockreturn.entity.StockReturn;
import com.pos.stockreturn.entity.StockReturn.ReturnStatus;
import com.pos.stockreturn.entity.StockReturnItem;
import com.pos.stockreturn.repository.StockReturnRepository;
import com.pos.transfer.dto.TransferItemRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockReturnService {

    private final StockReturnRepository   returnRepository;
    private final StockRepository         stockRepository;
    private final StockMovementRepository movementRepository;
    private final ItemRepository          itemRepository;
    private final ShopService             shopService;
    private final ShopStockService        shopStockService;
    private final UserRepository          userRepository;

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<StockReturnResponse> findAll(Long shopId, String status, Pageable pageable) {
        ReturnStatus statusEnum = status != null ? ReturnStatus.valueOf(status.toUpperCase()) : null;
        return returnRepository.findAll(shopId, statusEnum, pageable)
                .map(r -> {
                    r.getItems().size();  // force lazy load
                    return StockReturnResponse.from(r);
                });
    }

    @Transactional(readOnly = true)
    public StockReturnResponse findById(Long id) {
        return StockReturnResponse.from(getWithItemsOrThrow(id));
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public StockReturnResponse create(StockReturnRequest request) {
        Shop shop = shopService.getOrThrow(request.shopId());
        User actor = resolveCurrentUser();

        List<Long> itemIds = request.items().stream().map(TransferItemRequest::itemId).toList();
        Map<Long, Item> itemMap = itemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, i -> i));

        for (Long itemId : itemIds) {
            if (!itemMap.containsKey(itemId)) {
                throw new ResourceNotFoundException("Item not found: " + itemId);
            }
        }

        StockReturn stockReturn = StockReturn.builder()
                .shop(shop)
                .note(request.note())
                .createdBy(actor)
                .build();

        List<StockReturnItem> lines = new ArrayList<>();
        for (TransferItemRequest req : request.items()) {
            lines.add(StockReturnItem.builder()
                    .stockReturn(stockReturn)
                    .item(itemMap.get(req.itemId()))
                    .quantity(req.quantity())
                    .build());
        }
        stockReturn.setItems(lines);

        StockReturn saved = returnRepository.save(stockReturn);
        log.info("action=return_created, id={}, shopId={}", saved.getId(), shop.getId());
        return StockReturnResponse.from(saved);
    }

    // ── Complete ─────────────────────────────────────────────────────────────

    /**
     * Completes a return:
     * 1. Validates shop stock has enough for each line
     * 2. Deducts from shop stock
     * 3. Adds to central stock
     * 4. Records RETURN_OUT + RETURN_IN movements
     */
    @Transactional
    public StockReturnResponse complete(Long id) {
        StockReturn stockReturn = getWithItemsOrThrow(id);
        guardStatus(stockReturn, ReturnStatus.PENDING, "complete");

        User actor = resolveCurrentUser();
        String ref = "RETURN-" + id;

        // Pre-validate shop stock levels
        for (StockReturnItem line : stockReturn.getItems()) {
            var shopStock = shopStockService.getOrThrow(stockReturn.getShop().getId(), line.getItem().getId());
            if (shopStock.getQuantity() < line.getQuantity()) {
                throw new BusinessException("INSUFFICIENT_SHOP_STOCK",
                        "Insufficient shop stock for '" + line.getItem().getName()
                        + "'. Available: " + shopStock.getQuantity()
                        + ", Requested: " + line.getQuantity());
            }
        }

        for (StockReturnItem line : stockReturn.getItems()) {
            Item item = line.getItem();
            int qty   = line.getQuantity();

            // Deduct from shop
            shopStockService.deductForReturn(stockReturn.getShop(), item, qty, ref, actor);

            // Add to central
            Stock central = stockRepository.findByItemId(item.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Central stock not found: " + item.getId()));
            int centralBefore = central.getQuantity();
            central.add(qty);
            movementRepository.save(StockMovement.builder()
                    .item(item)
                    .shop(null)
                    .type(MovementType.RETURN_IN)
                    .quantity(qty)
                    .balanceBefore(centralBefore)
                    .balanceAfter(central.getQuantity())
                    .reference(ref)
                    .createdBy(actor)
                    .build());
        }

        stockReturn.setStatus(ReturnStatus.COMPLETED);
        stockReturn.setCompletedAt(LocalDateTime.now());

        log.info("action=return_completed, id={}, shopId={}", id, stockReturn.getShop().getId());
        return StockReturnResponse.from(stockReturn);
    }

    // ── Cancel ───────────────────────────────────────────────────────────────

    @Transactional
    public StockReturnResponse cancel(Long id) {
        StockReturn stockReturn = getWithItemsOrThrow(id);
        guardStatus(stockReturn, ReturnStatus.PENDING, "cancel");
        stockReturn.setStatus(ReturnStatus.CANCELLED);
        log.info("action=return_cancelled, id={}", id);
        return StockReturnResponse.from(stockReturn);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private StockReturn getWithItemsOrThrow(Long id) {
        return returnRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found: " + id));
    }

    private void guardStatus(StockReturn r, ReturnStatus required, String action) {
        if (r.getStatus() != required) {
            throw new BusinessException("INVALID_RETURN_STATUS",
                    "Cannot " + action + " a return with status: " + r.getStatus());
        }
    }

    private User resolveCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }
}
