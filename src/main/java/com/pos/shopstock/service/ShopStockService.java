package com.pos.shopstock.service;

import com.pos.auth.entity.User;
import com.pos.auth.repository.UserRepository;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.item.entity.Item;
import com.pos.item.repository.ItemRepository;
import com.pos.shop.entity.Shop;
import com.pos.shop.service.ShopService;
import com.pos.shopstock.dto.ShopStockAdjustRequest;
import com.pos.shopstock.dto.ShopStockResponse;
import com.pos.shopstock.entity.ShopStock;
import com.pos.shopstock.repository.ShopStockRepository;
import com.pos.stock.entity.StockMovement;
import com.pos.stock.entity.StockMovement.MovementType;
import com.pos.stock.repository.StockMovementRepository;
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
public class ShopStockService {

    private final ShopStockRepository  shopStockRepository;
    private final ShopService          shopService;
    private final ItemRepository       itemRepository;
    private final UserRepository       userRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional(readOnly = true)
    public Page<ShopStockResponse> findByShop(Long shopId, boolean lowStockOnly,
                                               String search, Pageable pageable) {
        return shopStockRepository
                .findByShop(shopId, lowStockOnly, search, pageable)
                .map(ShopStockResponse::from);
    }

    @Transactional(readOnly = true)
    public ShopStockResponse findByShopAndItem(Long shopId, Long itemId) {
        return ShopStockResponse.from(getOrThrow(shopId, itemId));
    }

    /**
     * Initialise a zero-quantity shop-stock record for a newly transferred item.
     * Called internally by StockTransferService when completing a transfer.
     * No-op if the record already exists.
     */
    @Transactional
    public ShopStock getOrCreate(Shop shop, Item item) {
        return shopStockRepository.findByShopIdAndItemId(shop.getId(), item.getId())
                .orElseGet(() -> shopStockRepository.save(
                        ShopStock.builder().shop(shop).item(item).quantity(0).build()
                ));
    }

    /**
     * Manual stock adjustment for a shop (INVENTORY role).
     * Records a movement for the audit trail.
     */
    @Transactional
    public ShopStockResponse adjust(Long shopId, Long itemId, ShopStockAdjustRequest request) {
        ShopStock ss = getOrThrow(shopId, itemId);
        int before = ss.getQuantity();
        ss.setQuantity(request.quantity());

        User actor = resolveCurrentUser();
        recordMovement(ss, MovementType.ADJUSTMENT, request.quantity() - before,
                before, request.quantity(), "ADJ-SHOP-" + shopId, request.note(), actor);

        log.info("action=shop_stock_adjusted, shopId={}, itemId={}, before={}, after={}",
                shopId, itemId, before, request.quantity());
        return ShopStockResponse.from(ss);
    }

    // ── Internal helpers (called from TransferService / SaleService) ─────────

    /**
     * Add quantity to a shop's stock (called when transfer is completed).
     */
    @Transactional
    public void addStock(Shop shop, Item item, int quantity, String reference, User actor) {
        ShopStock ss = getOrCreate(shop, item);
        int before = ss.getQuantity();
        ss.add(quantity);
        recordMovement(ss, MovementType.TRANSFER_IN, quantity, before, ss.getQuantity(), reference, null, actor);
    }

    /**
     * Deduct quantity from a shop's stock (called when a sale is processed).
     */
    @Transactional
    public void deductForSale(Shop shop, Item item, int quantity, String reference) {
        ShopStock ss = getOrThrow(shop.getId(), item.getId());
        int before = ss.getQuantity();
        ss.deduct(quantity);  // throws if insufficient
        recordMovement(ss, MovementType.SALE, -quantity, before, ss.getQuantity(), reference, null, null);
    }

    /**
     * Deduct quantity from a shop's stock (called when a return is completed).
     */
    @Transactional
    public void deductForReturn(Shop shop, Item item, int quantity, String reference, User actor) {
        ShopStock ss = getOrThrow(shop.getId(), item.getId());
        int before = ss.getQuantity();
        ss.deduct(quantity);
        recordMovement(ss, MovementType.RETURN_OUT, -quantity, before, ss.getQuantity(), reference, null, actor);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    public ShopStock getOrThrow(Long shopId, Long itemId) {
        return shopStockRepository.findByShopIdAndItemId(shopId, itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shop stock not found for shop=" + shopId + ", item=" + itemId));
    }

    private void recordMovement(ShopStock ss, MovementType type, int quantity,
                                 int before, int after, String reference,
                                 String note, User actor) {
        stockMovementRepository.save(StockMovement.builder()
                .item(ss.getItem())
                .shop(ss.getShop())
                .type(type)
                .quantity(Math.abs(quantity))
                .balanceBefore(before)
                .balanceAfter(after)
                .reference(reference)
                .note(note)
                .createdBy(actor)
                .build());
    }

    private User resolveCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElse(null);
    }
}
