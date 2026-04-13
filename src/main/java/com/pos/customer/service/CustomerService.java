package com.pos.customer.service;

import com.pos.common.exception.BusinessException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.customer.dto.CustomerRequest;
import com.pos.customer.dto.CustomerResponse;
import com.pos.customer.entity.Customer;
import com.pos.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAll(String search, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("customerName"));

        if (search != null && !search.isBlank()) {
            String like = "%" + search.toLowerCase() + "%";
            Specification<Customer> spec = (root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("customerName")),   like),
                    cb.like(cb.lower(root.get("customerNumber")), like)
            );
            return customerRepository.findAll(spec, pageable).map(CustomerResponse::from);
        }

        return customerRepository.findAll(pageable).map(CustomerResponse::from);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findByPhone(String phone) {
        return customerRepository.findByCustomerNumber(phone.trim())
                .map(CustomerResponse::from)
                .orElse(null);   // null signals "not found" — controller returns 200 with null body
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByCustomerNumber(request.customerNumber().trim())) {
            throw new BusinessException("DUPLICATE_PHONE",
                    "Customer with phone number already exists: " + request.customerNumber());
        }
        Customer saved = customerRepository.save(
                Customer.builder()
                        .customerName(request.customerName().trim())
                        .customerNumber(request.customerNumber().trim())
                        .build());
        log.info("action=customer_created, id={}", saved.getId());
        return CustomerResponse.from(saved);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = findOrThrow(id);

        if (!customer.getCustomerNumber().equals(request.customerNumber().trim())
                && customerRepository.existsByCustomerNumber(request.customerNumber().trim())) {
            throw new BusinessException("DUPLICATE_PHONE",
                    "Phone number already used: " + request.customerNumber());
        }

        customer.setCustomerName(request.customerName().trim());
        customer.setCustomerNumber(request.customerNumber().trim());
        log.info("action=customer_updated, id={}", id);
        return CustomerResponse.from(customer);
    }

    @Transactional
    public void delete(Long id) {
        customerRepository.delete(findOrThrow(id));
        log.info("action=customer_deleted, id={}", id);
    }

    public Customer findOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }
}
