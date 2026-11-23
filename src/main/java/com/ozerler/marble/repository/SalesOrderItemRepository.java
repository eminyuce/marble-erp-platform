package com.ozerler.marble.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.SalesOrderItem;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {

    @EntityGraph(attributePaths = {"salesOrder", "salesOrder.customer", "block", "slab"})
    @Query("SELECT soi FROM SalesOrderItem soi WHERE soi.id = :id")
    Optional<SalesOrderItem> findByIdWithDetails(@Param("id") Long id);
}
