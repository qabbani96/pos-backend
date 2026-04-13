package com.pos.stock.service;

import com.pos.auth.entity.User;
import com.pos.auth.repository.UserRepository;
import com.pos.common.exception.BusinessException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.item.entity.Item;
import com.pos.item.repository.ItemRepository;
import com.pos.stock.dto.StockAdjustRequest;
import com.pos.stock.dto.StockMovementResponse;
import com.pos.stock.dto.StockResponse;
import com.pos.stock.dto.StockSummary;
import com.pos.stock.entity.Stock;
import com.pos.stock.entity.StockMovement;
import com.pos.stock.entity.StockMovement.MovementType;
import com.pos.stock.repository.StockMovementRepository;
import com.pos.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<StockResponse> findAll(boolean lowStockOnly, Pageable pageable) {
        return stockRepository.findAll(lowStockOnly, pageable).map(StockResponse::from);
    }

    @Transactional(readOnly = true)
    public StockResponse findByItemId(Long itemId) {
        return StockResponse.from(getStockOrThrow(itemId));
    }

    @Transactional(readOnly = true)
    public StockSummary getSummary() {
        long total = stockRepository.count();
        long low = stockRepository.countLowStock();
        return new StockSummary(total, low);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getMovements(Long itemId, Pageable pageable) {
        // Verify item exists
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item not found: " + itemId);
        }
        return movementRepository.findByItemId(itemId, pageable).map(StockMovementResponse::from);
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    /**
     * Creates an initial zero-stock record when a new item is saved.
     * Called from ItemService inside the same transaction.
     */
    @Transactional
    public void initStock(Item item) {
        Stock stock = Stock.builder()
                .item(item)
                .quantity(0)
                .minQuantity(5)
                .build();
        stockRepository.save(stock);
        log.info("action=stock_initialized, itemId={}", item.getId());
    }

    /**
     * Manual stock adjustment by an admin.
     * IN  → adds quantity
     * OUT → deducts quantity (validates sufficient stock)
     * ADJUSTMENT → sets absolute quantity (e.g. after physical count)
     */
    @Transactional
    public StockResponse adjust(Long itemId, StockAdjustRequest request) {
        Stock stock = getStockOrThrow(itemId);
        User actor = resolveCurrentUser();

        int before = stock.getQuantity();

        switch (request.type()) {
            case IN -> stock.add(request.quantity());
            case OUT -> {
                if (stock.getQuantity() < request.quantity()) {
                    throw new BusinessException(
                            "INSUFFICIENT_STOCK",
                            "Cannot deduct " + request.quantity()
                            + " units. Available: " + stock.getQuantity());
                }
                stock.deduct(request.quantity());
            }
            case ADJUSTMENT -> {
                if (request.quantity() < 0) {
                    throw new BusinessException(
                            "INVALID_QUANTITY",
                            "Adjustment quantity must be >= 0");
                }
                stock.setQuantity(request.quantity());
            }
            default -> throw new BusinessException("INVALID_TYPE",
                    "Central stock only supports IN, OUT, ADJUSTMENT. Use ShopStock for shop-level operations.");
        }

        recordMovement(stock.getItem(), request.type(), request.quantity(),
                before, stock.getQuantity(), request.reference(), request.note(), actor);

        log.info("action=stock_adjusted, itemId={}, type={}, before={}, after={}, by={}",
                itemId, request.type(), before, stock.getQuantity(),
                actor != null ? actor.getUsername() : "system");

        return StockResponse.from(stock);
    }

    /**
     * Updates the minimum stock threshold for low-stock alerting.
     */
    @Transactional
    public StockResponse updateMinQuantity(Long itemId, int minQuantity) {
        if (minQuantity < 0) {
            throw new BusinessException("INVALID_MIN_QUANTITY", "Min quantity must be >= 0");
        }
        Stock stock = getStockOrThrow(itemId);
        stock.setMinQuantity(minQuantity);
        log.info("action=min_quantity_updated, itemId={}, minQty={}", itemId, minQuantity);
        return StockResponse.from(stock);
    }

    // ── Internal helpers (package-private for SaleService) ───────────────────

    // ── Private ──────────────────────────────────────────────────────────────

    private void recordMovement(Item item, MovementType type, int quantity,
                                int before, int after,
                                String reference, String note, User actor) {
        StockMovement movement = StockMovement.builder()
                .item(item)
                .shop(null)  // central warehouse movements have no shop
                .type(type)
                .quantity(quantity)
                .balanceBefore(before)
                .balanceAfter(after)
                .reference(reference)
                .note(note)
                .createdBy(actor)
                .build();
        movementRepository.save(movement);
    }

    private Stock getStockOrThrow(Long itemId) {
        return stockRepository.findByItemId(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock record not found for item: " + itemId));
    }

    private User resolveCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }
}
