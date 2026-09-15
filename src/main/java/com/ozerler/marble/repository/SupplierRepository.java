package com.ozerler.marble.repository;

import com.ozerler.marble.model.Supplier;
import com.ozerler.marble.model.enums.SupplierType;
import org.springframework.data.domain.Page;
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

    @Query("SELECT s FROM Supplier s WHERE (:search IS NULL "
            + "OR LOWER(s.supplierCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
            + "OR LOWER(s.companyName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
            + "OR LOWER(COALESCE(s.contactPerson, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) "
            + "AND (:type IS NULL OR s.supplierType = :type)")
    Page<Supplier> search(@Param("search") String search, @Param("type") SupplierType type, Pageable pageable);
}
