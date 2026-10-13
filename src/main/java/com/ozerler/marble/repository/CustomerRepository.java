package com.ozerler.marble.repository;

import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.enums.CustomerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerCode(String customerCode);

    Optional<Customer> findByCompanyNameIgnoreCase(String companyName);

    List<Customer> findAllByOrderByCompanyNameAsc();

    @Query("SELECT c FROM Customer c WHERE LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Customer> searchByCodeOrName(@Param("query") String query, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE (:search IS NULL "
            + "OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
            + "OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
            + "OR LOWER(COALESCE(c.contactPerson, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) "
            + "AND (:type IS NULL OR c.customerType = :type) "
            + "AND (:unit IS NULL OR c.businessUnit = :unit)")
    Page<Customer> search(@Param("search") String search,
                          @Param("type") CustomerType type,
                          @Param("unit") com.ozerler.marble.model.enums.BusinessUnit unit,
                          Pageable pageable);

    default Page<Customer> search(String search, CustomerType type, Pageable pageable) {
        return search(search, type, null, pageable);
    }
}
