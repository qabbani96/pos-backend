package com.pos.item.service;

import com.pos.common.exception.BusinessException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.item.dto.ItemRequest;
import com.pos.item.dto.ItemResponse;
import com.pos.item.entity.Category;
import com.pos.item.entity.Item;
import com.pos.item.entity.Item.BarcodeType;
import com.pos.item.repository.CategoryRepository;
import com.pos.item.repository.ItemRepository;
import com.pos.barcode.service.BarcodeService;
import com.pos.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final StockService stockService;
    private final BarcodeService barcodeService;

    @Transactional(readOnly = true)
    public Page<ItemResponse> findAll(String search, Long categoryId,
                                      boolean activeOnly, Pageable pageable) {
        Page<Item> page = activeOnly
                ? itemRepository.findAllActive(search, categoryId, pageable)
                : itemRepository.findAll(search, categoryId, pageable);
        return page.map(ItemResponse::from);
    }

    @Transactional(readOnly = true)
    public ItemResponse findById(Long id) {
        return ItemResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public ItemResponse findByBarcode(String barcode) {
        Item item = itemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found for barcode: " + barcode));
        return ItemResponse.from(item);
    }

    @Transactional
    public ItemResponse create(ItemRequest request) {
        if (itemRepository.existsBySku(request.sku())) {
            throw new BusinessException("DUPLICATE_SKU", "SKU already exists: " + request.sku());
        }
        if (request.barcode() != null && !request.barcode().isBlank()
                && itemRepository.existsByBarcode(request.barcode())) {
            throw new BusinessException("DUPLICATE_BARCODE", "Barcode already exists: " + request.barcode());
        }

        Item item = buildItem(new Item(), request);
        Item saved = itemRepository.save(item);
        stockService.initStock(saved);           // auto-create zero-stock record
        barcodeService.assignBarcode(saved.getId()); // auto-assign barcode if not provided
        log.info("action=item_created, id={}, sku={}", saved.getId(), saved.getSku());
        return ItemResponse.from(saved);
    }

    @Transactional
    public ItemResponse update(Long id, ItemRequest request) {
        Item item = getOrThrow(id);

        // SKU uniqueness check (only if changed)
        if (!item.getSku().equals(request.sku()) && itemRepository.existsBySku(request.sku())) {
            throw new BusinessException("DUPLICATE_SKU", "SKU already exists: " + request.sku());
        }

        // Barcode uniqueness check (only if changed)
        if (request.barcode() != null && !request.barcode().isBlank()
                && !request.barcode().equals(item.getBarcode())
                && itemRepository.existsByBarcode(request.barcode())) {
            throw new BusinessException("DUPLICATE_BARCODE", "Barcode already exists: " + request.barcode());
        }

        buildItem(item, request);
        log.info("action=item_updated, id={}", id);
        return ItemResponse.from(item);
    }

    @Transactional
    public void deactivate(Long id) {
        Item item = getOrThrow(id);
        item.setActive(false);
        log.info("action=item_deactivated, id={}", id);
    }

    @Transactional
    public void activate(Long id) {
        Item item = getOrThrow(id);
        item.setActive(true);
        log.info("action=item_activated, id={}", id);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Item buildItem(Item item, ItemRequest request) {
        item.setSku(request.sku().trim());
        item.setName(request.name().trim());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setCostPrice(request.costPrice());
        item.setBarcode(request.barcode());
        item.setBarcodeType(request.barcodeType() != null ? request.barcodeType() : BarcodeType.CODE128);
        item.setImageUrl(request.imageUrl());

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));
            item.setCategory(category);
        } else {
            item.setCategory(null);
        }

        return item;
    }

    private Item getOrThrow(Long id) {
        return itemRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + id));
    }
}
