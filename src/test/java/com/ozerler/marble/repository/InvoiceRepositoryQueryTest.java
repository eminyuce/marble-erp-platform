package com.ozerler.marble.repository;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.InvoiceStatus;
import com.ozerler.marble.model.enums.InvoiceType;

class InvoiceRepositoryQueryTest {

    @Test
    @DisplayName(
            "findByIdWithDetails uses @Query with LEFT JOIN FETCH for customer, supplier, project, items, and related "
                    + "inventory references")
    void findByIdWithDetailsUsesExplicitLeftJoinFetch() throws Exception {
        Query query = InvoiceRepository.class
                .getMethod("findByIdWithDetails", Long.class)
                .getAnnotation(Query.class);

        Assertions.assertThat(query).isNotNull();
        Assertions.assertThat(query.value()).containsIgnoringCase("SELECT DISTINCT i FROM Invoice i");
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH i.customer");
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH i.supplier");
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH i.project");
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH i.items it");
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH it.block");
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH it.slab");
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH it.stockItem");
        Assertions.assertThat(query.value()).contains("WHERE i.id = :id");
    }

    @Test
    @DisplayName(
            "findAllWithCustomer fetches the customer so collection forms can read companyName after the session "
                    + "closes")
    void findAllWithCustomerFetchesCustomer() throws Exception {
        Query query = InvoiceRepository.class
                .getMethod("findAllWithCustomer")
                .getAnnotation(Query.class);

        Assertions.assertThat(query).isNotNull();
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH i.customer");
    }

    @Test
    @DisplayName("findByIdWithCustomer fetches the customer for the preselected collection invoice")
    void findByIdWithCustomerFetchesCustomer() throws Exception {
        Query query = InvoiceRepository.class
                .getMethod("findByIdWithCustomer", Long.class)
                .getAnnotation(Query.class);

        Assertions.assertThat(query).isNotNull();
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH i.customer");
        Assertions.assertThat(query.value()).contains("WHERE i.id = :id");
    }

    @Test
    @DisplayName(
            "searchInvoices fetches customer, supplier, and project so list grid serialization does not fail outside "
                    + "session")
    void searchInvoicesFetchesCustomerSupplierAndProject() throws Exception {
        Query query = InvoiceRepository.class
                .getMethod("searchInvoices",
                        InvoiceType.class,
                        BusinessUnit.class,
                        InvoiceStatus.class,
                        Long.class,
                        LocalDate.class,
                        LocalDate.class,
                        String.class,
                        Pageable.class)
                .getAnnotation(Query.class);

        Assertions.assertThat(query).isNotNull();
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH i.customer");
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH i.supplier");
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH i.project");
    }
}
