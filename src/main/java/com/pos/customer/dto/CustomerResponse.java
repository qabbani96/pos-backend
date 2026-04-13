package com.pos.customer.dto;

import com.pos.customer.entity.Customer;

public record CustomerResponse(
        Long   id,
        String customerName,
        String customerNumber
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCustomerName(),
                customer.getCustomerNumber()
        );
    }
}
