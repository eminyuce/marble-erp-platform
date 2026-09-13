package com.ozerler.marble.repository;

import com.ozerler.marble.model.Supplier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findBySupplierCode(String supplierCode);

    List<Supplier> findAllByOrderByCompanyNameAsc();

    @Query("SELECT s FROM Supplier s WHERE LOWER(s.supplierCode) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(s.companyName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Supplier> searchByCodeOrName(@Param("query") String query, Pageable pageable);
}
