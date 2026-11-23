package com.ozerler.marble.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

class JoinSortQueryTest {

    @Test
    @DisplayName("cut, sales and purchase search queries join-fetch aliases and keep a non-fetch count")
    void pagedJoinSortQueriesAvoidDistinctAndEntityGraph() throws Exception {
        Query cuts = CutOrderRepository.class
                .getMethod("searchCutOrders", String.class, Pageable.class)
                .getAnnotation(Query.class);
        Assertions.assertThat(cuts.value()).contains("LEFT JOIN FETCH c.project project");
        Assertions.assertThat(cuts.value()).contains("LEFT JOIN FETCH c.location location");
        Assertions.assertThat(cuts.value()).doesNotContain("SELECT DISTINCT");
        Assertions.assertThat(cuts.countQuery()).doesNotContain("FETCH");

        Query sales = SalesOrderRepository.class
                .getMethod("searchSalesOrders", String.class, Pageable.class)
                .getAnnotation(Query.class);
        Assertions.assertThat(sales.value()).contains("LEFT JOIN FETCH so.customer customer");
        Assertions.assertThat(sales.countQuery()).doesNotContain("FETCH");

        Query purchases = PurchaseOrderRepository.class
                .getMethod("searchPurchaseOrders", String.class, Pageable.class)
                .getAnnotation(Query.class);
        Assertions.assertThat(purchases.value()).contains("LEFT JOIN FETCH po.supplier supplier");
        Assertions.assertThat(purchases.value()).contains("LEFT JOIN FETCH po.project project");
        Assertions.assertThat(purchases.countQuery()).doesNotContain("FETCH");
    }
}
