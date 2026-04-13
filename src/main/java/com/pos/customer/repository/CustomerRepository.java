package com.pos.customer.repository;

import com.pos.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long>,
        JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByCustomerNumber(String customerNumber);

    boolean existsByCustomerNumber(String customerNumber);
}
