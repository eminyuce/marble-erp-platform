package com.ozerler.marble.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import com.ozerler.marble.model.enums.StockProductType;

class StockItemRepositoryQueryTest {

    @Test
    @DisplayName("searchItems uses @EntityGraph with customer, sourceBlock, and stockLocation to prevent lazy initialization issues")
    void searchItemsUsesEntityGraph() throws Exception {
        EntityGraph entityGraph = StockItemRepository.class
                .getMethod("searchItems",
                        StockProductType.class,
                        String.class,
                        Long.class,
                        boolean.class,
                        String.class,
                        Pageable.class)
                .getAnnotation(EntityGraph.class);

        Assertions.assertThat(entityGraph).isNotNull();
        Assertions.assertThat(entityGraph.attributePaths()).contains("customer", "sourceBlock", "stockLocation");
    }

    @Test
    @DisplayName("searchItems has valid explicit query and countQuery")
    void searchItemsQueryStructure() throws Exception {
        Query query = StockItemRepository.class
                .getMethod("searchItems",
                        StockProductType.class,
                        String.class,
                        Long.class,
                        boolean.class,
                        String.class,
                        Pageable.class)
                .getAnnotation(Query.class);

        Assertions.assertThat(query).isNotNull();
        Assertions.assertThat(query.value()).contains("SELECT s FROM StockItem s");
        Assertions.assertThat(query.countQuery()).contains("SELECT COUNT(s) FROM StockItem s");
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

        Assertions.assertThat(entityGraph).isNotNull();
        Assertions.assertThat(entityGraph.attributePaths()).contains("stockLocation");
        Assertions.assertThat(query).isNotNull();
        Assertions.assertThat(query.value()).contains("s.quantity > 0");
    }
}
