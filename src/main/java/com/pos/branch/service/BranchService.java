package com.pos.branch.service;

import com.pos.branch.dto.BranchRequest;
import com.pos.branch.dto.BranchResponse;
import com.pos.branch.entity.Branch;
import com.pos.branch.repository.BranchRepository;
import com.pos.common.exception.BusinessException;
import com.pos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchService {

    private final BranchRepository branchRepository;

    // ── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BranchResponse> getAllBranches() {
        return branchRepository.findAll()
                .stream()
                .map(BranchResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchResponse getBranchById(Long id) {
        return BranchResponse.from(findOrThrow(id));
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public BranchResponse createBranch(BranchRequest request) {
        if (branchRepository.existsByBranchName(request.branchName())) {
            throw new BusinessException("BRANCH_NAME_TAKEN", "Branch name already exists: " + request.branchName());
        }

        Branch branch = Branch.builder()
                .branchName(request.branchName())
                .mobile(request.mobile())
                .receiptWidthMm(request.receiptWidthMm() != null ? request.receiptWidthMm() : 58)
                .receiptHeightMm(request.receiptHeightMm())
                .build();

        Branch saved = branchRepository.save(branch);
        log.info("Branch created: id={}, name={}", saved.getId(), saved.getBranchName());
        return BranchResponse.from(saved);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public BranchResponse updateBranch(Long id, BranchRequest request) {
        Branch branch = findOrThrow(id);

        if (!branch.getBranchName().equals(request.branchName())
                && branchRepository.existsByBranchName(request.branchName())) {
            throw new BusinessException("BRANCH_NAME_TAKEN", "Branch name already exists: " + request.branchName());
        }

        branch.setBranchName(request.branchName());
        if (request.mobile() != null)            branch.setMobile(request.mobile());
        if (request.receiptWidthMm() != null)    branch.setReceiptWidthMm(request.receiptWidthMm());
        // null is a valid value for height (means auto) — always update if included
        branch.setReceiptHeightMm(request.receiptHeightMm());
        Branch saved = branchRepository.save(branch);
        log.info("Branch updated: id={}, name={}, mobile={}, width={}mm",
                saved.getId(), saved.getBranchName(), saved.getMobile(), saved.getReceiptWidthMm());
        return BranchResponse.from(saved);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void deleteBranch(Long id) {
        Branch branch = findOrThrow(id);
        branchRepository.delete(branch);
        log.info("Branch deleted: id={}, name={}", id, branch.getBranchName());
    }

    // ── Public helper (used by UserService) ──────────────────────────────────

    @Transactional(readOnly = true)
    public Branch findOrThrow(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
    }
}
