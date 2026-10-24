package com.ozerler.marble.repository;

import com.ozerler.marble.model.CariTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CariTransactionRepository extends JpaRepository<CariTransaction, Long> {

    List<CariTransaction> findByCustomerIdOrderByTransactionDateAscIdAsc(Long customerId);

    List<CariTransaction> findByCustomerIdOrderByTransactionDateDescIdDesc(Long customerId);

    Page<CariTransaction> findByCustomerIdOrderByTransactionDateDescIdDesc(Long customerId, Pageable pageable);

    Optional<CariTransaction> findByInvoiceId(Long invoiceId);

    Optional<CariTransaction> findByCollectionId(Long collectionId);

    @Query("SELECT COALESCE(SUM(t.debitAmount), 0), COALESCE(SUM(t.creditAmount), 0) " +
           "FROM CariTransaction t WHERE t.customer.id = :customerId")
    List<Object[]> getDebitCreditTotalsByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT t.customer.id, COALESCE(SUM(t.debitAmount), 0), COALESCE(SUM(t.creditAmount), 0) " +
           "FROM CariTransaction t WHERE t.customer.id IN :customerIds GROUP BY t.customer.id")
    List<Object[]> sumTotalsByCustomerIds(@Param("customerIds") List<Long> customerIds);
}
