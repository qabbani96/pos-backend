package com.pos.transfer.service;

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
import com.pos.stock.entity.StockMovement.MovementType;
import com.pos.stock.repository.StockMovementRepository;
import com.pos.stock.repository.StockRepository;
import com.pos.stock.entity.StockMovement;
import com.pos.transfer.dto.StockTransferRequest;
import com.pos.transfer.dto.StockTransferResponse;
import com.pos.transfer.entity.StockTransfer;
import com.pos.transfer.entity.StockTransfer.TransferStatus;
import com.pos.transfer.entity.StockTransferItem;
import com.pos.transfer.repository.StockTransferRepository;
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
public class StockTransferService {

    private final StockTransferRepository transferRepository;
    private final StockRepository         stockRepository;
    private final StockMovementRepository movementRepository;
    private final ItemRepository          itemRepository;
    private final ShopService             shopService;
    private final ShopStockService        shopStockService;
    private final UserRepository          userRepository;

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<StockTransferResponse> findAll(Long shopId, String status, Pageable pageable) {
        TransferStatus statusEnum = status != null ? TransferStatus.valueOf(status.toUpperCase()) : null;
        return transferRepository.findAll(shopId, statusEnum, pageable)
                .map(t -> {
                    // Items are lazily loaded — force within transaction
                    t.getItems().size();
                    return StockTransferResponse.from(t);
                });
    }

    @Transactional(readOnly = true)
    public StockTransferResponse findById(Long id) {
        return StockTransferResponse.from(getWithItemsOrThrow(id));
    }

    // ── Create ───────────────────────────────────────────────────────────────

    /**
     * Creates a PENDING transfer. No stock is moved yet.
     * INVENTORY role creates the order; completing it deducts central and adds shop stock.
     */
    @Transactional
    public StockTransferResponse create(StockTransferRequest request) {
        Shop shop = shopService.getOrThrow(request.shopId());
        User actor = resolveCurrentUser();

        // Resolve all items in one query
        List<Long> itemIds = request.items().stream().map(i -> i.itemId()).toList();
        Map<Long, Item> itemMap = itemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, i -> i));

        validateItemsExist(itemIds, itemMap);

        StockTransfer transfer = StockTransfer.builder()
                .shop(shop)
                .note(request.note())
                .createdBy(actor)
                .build();

        List<StockTransferItem> lines = new ArrayList<>();
        for (var req : request.items()) {
            lines.add(StockTransferItem.builder()
                    .transfer(transfer)
                    .item(itemMap.get(req.itemId()))
                    .quantity(req.quantity())
                    .build());
        }
        transfer.setItems(lines);

        StockTransfer saved = transferRepository.save(transfer);
        log.info("action=transfer_created, id={}, shopId={}, items={}", saved.getId(), shop.getId(), lines.size());
        return StockTransferResponse.from(saved);
    }

    // ── Complete ─────────────────────────────────────────────────────────────

    /**
     * Completes a PENDING transfer:
     * 1. Validates central stock is sufficient for every line
     * 2. Deducts from central stock (stock table)
     * 3. Adds to shop stock (shop_stock table)
     * 4. Records TRANSFER_OUT + TRANSFER_IN movements
     */
    @Transactional
    public StockTransferResponse complete(Long id) {
        StockTransfer transfer = getWithItemsOrThrow(id);
        guardStatus(transfer, TransferStatus.PENDING, "complete");

        User actor = resolveCurrentUser();

        // Pre-validate all central stock levels before touching anything
        for (StockTransferItem line : transfer.getItems()) {
            Stock centralStock = stockRepository.findByItemId(line.getItem().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Central stock not found for item: " + line.getItem().getId()));
            if (centralStock.getQuantity() < line.getQuantity()) {
                throw new BusinessException("INSUFFICIENT_CENTRAL_STOCK",
                        "Insufficient central stock for '" + line.getItem().getName()
                        + "'. Available: " + centralStock.getQuantity()
                        + ", Requested: " + line.getQuantity());
            }
        }

        String ref = "TRANSFER-" + id;

        for (StockTransferItem line : transfer.getItems()) {
            Item item = line.getItem();
            int qty = line.getQuantity();

            // Deduct from central
            Stock centralStock = stockRepository.findByItemId(item.getId()).orElseThrow();
            int centralBefore = centralStock.getQuantity();
            centralStock.deduct(qty);
            movementRepository.save(StockMovement.builder()
                    .item(item)
                    .shop(null)   // central movement
                    .type(MovementType.TRANSFER_OUT)
                    .quantity(qty)
                    .balanceBefore(centralBefore)
                    .balanceAfter(centralStock.getQuantity())
                    .reference(ref)
                    .createdBy(actor)
                    .build());

            // Add to shop
            shopStockService.addStock(transfer.getShop(), item, qty, ref, actor);
        }

        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setCompletedAt(LocalDateTime.now());

        log.info("action=transfer_completed, id={}, shopId={}", id, transfer.getShop().getId());
        return StockTransferResponse.from(transfer);
    }

    // ── Cancel ───────────────────────────────────────────────────────────────

    @Transactional
    public StockTransferResponse cancel(Long id) {
        StockTransfer transfer = getWithItemsOrThrow(id);
        guardStatus(transfer, TransferStatus.PENDING, "cancel");
        transfer.setStatus(TransferStatus.CANCELLED);
        log.info("action=transfer_cancelled, id={}", id);
        return StockTransferResponse.from(transfer);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private StockTransfer getWithItemsOrThrow(Long id) {
        return transferRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + id));
    }

    private void guardStatus(StockTransfer transfer, TransferStatus required, String action) {
        if (transfer.getStatus() != required) {
            throw new BusinessException("INVALID_TRANSFER_STATUS",
                    "Cannot " + action + " a transfer with status: " + transfer.getStatus());
        }
    }

    private void validateItemsExist(List<Long> ids, Map<Long, Item> map) {
        for (Long id : ids) {
            if (!map.containsKey(id)) {
                throw new ResourceNotFoundException("Item not found: " + id);
            }
        }
    }

    private User resolveCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }
}
