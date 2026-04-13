package com.pos.branch.repository;

import com.pos.branch.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    boolean existsByBranchName(String branchName);
}
