package com.ozerler.marble.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.StockLocationType;

class BlockRepositoryQueryTest {

    @Test
    @DisplayName("findByBlockCodeWithQuarry uses @Query so Spring Data does not parse WithQuarry as a String property")
    void findByBlockCodeWithQuarryUsesExplicitQuery() throws Exception {
        Query query = BlockRepository.class
                .getMethod("findByBlockCodeWithQuarry", String.class)
                .getAnnotation(Query.class);

        Assertions.assertThat(query).isNotNull();
        Assertions.assertThat(query.value()).containsIgnoringCase("JOIN FETCH b.quarry");
        Assertions.assertThat(query.value()).contains("b.blockCode");
    }

    @Test
    @DisplayName("searchBlocks left-joins optional customer and location so unsold quarry blocks remain visible")
    void searchBlocksUsesLeftJoinsForOptionalAssociations() throws Exception {
        Query query = BlockRepository.class
                .getMethod("searchBlocks", String.class,
                        StockLocationType.class,
                        BlockStatus.class,
                        boolean.class,
                        Pageable.class)
                .getAnnotation(Query.class);

        Assertions.assertThat(query).isNotNull();
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH b.soldCustomer");
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH b.currentLocation");
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH b.quarry");
        Assertions.assertThat(query.value()).doesNotContain("SELECT DISTINCT");
        Assertions.assertThat(query.value()).doesNotContain("b.soldCustomer.companyName");
        Assertions.assertThat(query.countQuery()).containsIgnoringCase("LEFT JOIN b.soldCustomer");
        Assertions.assertThat(query.countQuery()).doesNotContain("FETCH");
        Assertions.assertThat(query.countQuery()).doesNotContain("COUNT(DISTINCT");
    }

    @Test
    @DisplayName("sumGridMetricsByQuarry groups filtered blocks so grid footer totals match the current view")
    void sumGridMetricsByQuarryGroupsByQuarry() throws Exception {
        Query query = BlockRepository.class
                .getMethod("sumGridMetricsByQuarry", String.class,
                        StockLocationType.class,
                        BlockStatus.class,
                        boolean.class)
                .getAnnotation(Query.class);

        Assertions.assertThat(query).isNotNull();
        Assertions.assertThat(query.value()).containsIgnoringCase("GROUP BY b.quarry.id");
        Assertions.assertThat(query.value()).containsIgnoringCase("SUM(b.transportCost)");
        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN b.soldCustomer");
    }

    @Test
    @DisplayName("searchBlocksWithStockFilter uses the same join-fetch aliases and no DISTINCT")
    void searchBlocksWithStockFilterAvoidsDistinctOrderByConflict() throws Exception {
        Query query = BlockRepository.class
                .getMethod("searchBlocksWithStockFilter", String.class,
                        StockLocationType.class,
                        BlockStatus.class,
                        String.class,
                        Pageable.class)
                .getAnnotation(Query.class);

        Assertions.assertThat(query.value()).containsIgnoringCase("LEFT JOIN FETCH b.quarry q");
        Assertions.assertThat(query.value()).doesNotContain("SELECT DISTINCT");
        Assertions.assertThat(query.countQuery()).doesNotContain("FETCH");
        Assertions.assertThat(query.countQuery()).doesNotContain("COUNT(DISTINCT");
    }
}
