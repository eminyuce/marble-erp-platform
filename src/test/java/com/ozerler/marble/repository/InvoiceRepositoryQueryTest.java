package com.ozerler.marble.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceRepositoryQueryTest {

    @Test
    @DisplayName("findByIdWithDetails uses @Query with LEFT JOIN FETCH for customer, supplier, project, items, and related inventory references")
    void findByIdWithDetailsUsesExplicitLeftJoinFetch() throws Exception {
        Query query = InvoiceRepository.class
                .getMethod("findByIdWithDetails", Long.class)
                .getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value()).containsIgnoringCase("SELECT DISTINCT i FROM Invoice i");
        assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH i.customer");
        assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH i.supplier");
        assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH i.project");
        assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH i.items it");
        assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH it.block");
        assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH it.slab");
        assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH it.stockItem");
        assertThat(query.value()).contains("WHERE i.id = :id");
    }
}
