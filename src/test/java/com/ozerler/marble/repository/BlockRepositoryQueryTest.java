package com.ozerler.marble.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;

class BlockRepositoryQueryTest {

    @Test
    @DisplayName("findByBlockCodeWithQuarry uses @Query so Spring Data does not parse WithQuarry as a String property")
    void findByBlockCodeWithQuarryUsesExplicitQuery() throws Exception {
        Query query = BlockRepository.class
                .getMethod("findByBlockCodeWithQuarry", String.class)
                .getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value()).containsIgnoringCase("JOIN FETCH b.quarry");
        assertThat(query.value()).contains("b.blockCode");
    }

    @Test
    @DisplayName("searchBlocks left-joins optional customer and location so unsold quarry blocks remain visible")
    void searchBlocksUsesLeftJoinsForOptionalAssociations() throws Exception {
        Query query = BlockRepository.class
                .getMethod("searchBlocks", String.class,
                        com.ozerler.marble.model.enums.StockLocationType.class,
                        com.ozerler.marble.model.enums.BlockStatus.class,
                        boolean.class,
                        org.springframework.data.domain.Pageable.class)
                .getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value()).containsIgnoringCase("LEFT JOIN b.soldCustomer");
        assertThat(query.value()).containsIgnoringCase("LEFT JOIN b.currentLocation");
        assertThat(query.value()).doesNotContain("b.soldCustomer.companyName");
        assertThat(query.countQuery()).containsIgnoringCase("LEFT JOIN b.soldCustomer");
    }
}
