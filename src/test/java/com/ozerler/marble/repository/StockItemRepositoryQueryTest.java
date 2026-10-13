package com.ozerler.marble.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;

class StockItemRepositoryQueryTest {

    @Test
    @DisplayName("searchItems uses @EntityGraph with customer, sourceBlock, and stockLocation to prevent lazy initialization issues")
    void searchItemsUsesEntityGraph() throws Exception {
        EntityGraph entityGraph = StockItemRepository.class
                .getMethod("searchItems",
                        com.ozerler.marble.model.enums.StockProductType.class,
                        String.class,
                        Long.class,
                        boolean.class,
                        String.class,
                        Pageable.class)
                .getAnnotation(EntityGraph.class);

        assertThat(entityGraph).isNotNull();
        assertThat(entityGraph.attributePaths()).contains("customer", "sourceBlock", "stockLocation");
    }

    @Test
    @DisplayName("searchItems has valid explicit query and countQuery")
    void searchItemsQueryStructure() throws Exception {
        Query query = StockItemRepository.class
                .getMethod("searchItems",
                        com.ozerler.marble.model.enums.StockProductType.class,
                        String.class,
                        Long.class,
                        boolean.class,
                        String.class,
                        Pageable.class)
                .getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value()).contains("SELECT s FROM StockItem s");
        assertThat(query.countQuery()).contains("SELECT COUNT(s) FROM StockItem s");
    }

    @Test
    @DisplayName("findPositiveQuantityWithLocation loads stockLocation so business unit can be read outside the session")
    void findPositiveQuantityWithLocationFetchesStockLocation() throws Exception {
        EntityGraph entityGraph = StockItemRepository.class
                .getMethod("findPositiveQuantityWithLocation")
                .getAnnotation(EntityGraph.class);
        Query query = StockItemRepository.class
                .getMethod("findPositiveQuantityWithLocation")
                .getAnnotation(Query.class);

        assertThat(entityGraph).isNotNull();
        assertThat(entityGraph.attributePaths()).contains("stockLocation");
        assertThat(query).isNotNull();
        assertThat(query.value()).contains("s.quantity > 0");
    }
}
