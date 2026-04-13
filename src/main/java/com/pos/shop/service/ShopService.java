package com.pos.shop.service;

import com.pos.branch.entity.Branch;
import com.pos.branch.repository.BranchRepository;
import com.pos.common.exception.BusinessException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.shop.dto.ShopRequest;
import com.pos.shop.dto.ShopResponse;
import com.pos.shop.entity.Shop;
import com.pos.shop.repository.ShopRepository;
import com.pos.shopstock.repository.ShopStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository      shopRepository;
    private final BranchRepository    branchRepository;
    private final ShopStockRepository shopStockRepository;

    @Transactional(readOnly = true)
    public List<ShopResponse> findAll(boolean activeOnly) {
        List<Shop> shops = activeOnly
                ? shopRepository.findAllActiveWithBranch()
                : shopRepository.findAll();
        return shops.stream().map(ShopResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ShopResponse findById(Long id) {
        return ShopResponse.from(getOrThrow(id));
    }

    @Transactional
    public ShopResponse create(ShopRequest request) {
        if (shopRepository.existsByName(request.name().trim())) {
            throw new BusinessException("DUPLICATE_SHOP_NAME", "Shop name already exists: " + request.name());
        }

        Shop shop = Shop.builder()
                .name(request.name().trim())
                .branch(resolveBranch(request.branchId()))
                .build();

        Shop saved = shopRepository.save(shop);
        log.info("action=shop_created, id={}, name={}", saved.getId(), saved.getName());
        return ShopResponse.from(saved);
    }

    @Transactional
    public ShopResponse update(Long id, ShopRequest request) {
        Shop shop = getOrThrow(id);

        if (!shop.getName().equals(request.name().trim()) && shopRepository.existsByName(request.name().trim())) {
            throw new BusinessException("DUPLICATE_SHOP_NAME", "Shop name already exists: " + request.name());
        }

        shop.setName(request.name().trim());
        shop.setBranch(resolveBranch(request.branchId()));

        log.info("action=shop_updated, id={}", id);
        return ShopResponse.from(shop);
    }

    @Transactional
    public void deactivate(Long id) {
        Shop shop = getOrThrow(id);
        shop.setActive(false);
        log.info("action=shop_deactivated, id={}", id);
    }

    @Transactional
    public void activate(Long id) {
        Shop shop = getOrThrow(id);
        shop.setActive(true);
        log.info("action=shop_activated, id={}", id);
    }

    @Transactional
    public void delete(Long id) {
        Shop shop = getOrThrow(id);

        if (shopRepository.hasAssignedUsers(id)) {
            throw new BusinessException(
                    "SHOP_HAS_USERS",
                    "Cannot delete shop '" + shop.getName() + "': it has users assigned. "
                    + "Reassign or remove those users first.");
        }

        if (shopStockRepository.existsByShopId(id)) {
            throw new BusinessException(
                    "SHOP_HAS_STOCK",
                    "Cannot delete shop '" + shop.getName() + "': it still has stock entries. "
                    + "Transfer or clear stock before deleting.");
        }

        shopRepository.delete(shop);
        log.info("action=shop_deleted, id={}, name={}", id, shop.getName());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    public Shop getOrThrow(Long id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found: " + id));
    }

    private Branch resolveBranch(Long branchId) {
        if (branchId == null) return null;
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
    }
}
