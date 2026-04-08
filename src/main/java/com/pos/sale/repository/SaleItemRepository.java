package com.pos.sale.repository;

import com.pos.sale.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @Query("SELECT si FROM SaleItem si JOIN FETCH si.item WHERE si.sale.id = :saleId")
    List<SaleItem> findBySaleId(@Param("saleId") Long saleId);
}
