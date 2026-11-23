package com.ozerler.marble.util;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class GridPagesTest {

    @Test
    @DisplayName("blank sort field uses createdDate descending")
    void blankSortFieldUsesCreatedDate() {
        Pageable pageable = GridPages.of(1, 25, "", "desc");

        Assertions.assertThat(pageable.getPageNumber()).isZero();
        Assertions.assertThat(pageable.getPageSize()).isEqualTo(25);
        Assertions.assertThat(pageable.getSort()).containsExactly(Sort.Order.desc("createdDate"));
    }

    @Test
    @DisplayName("snake_case sort fields are converted to entity camelCase")
    void snakeCaseSortFieldIsCamelized() {
        Assertions.assertThat(GridPages.toEntityProperty("order_no")).isEqualTo("orderNo");
        Assertions.assertThat(GridPages.toEntityProperty("blockCode")).isEqualTo("blockCode");
    }

    @Test
    @DisplayName("DTO statusLabel and snake_case customer_name map onto entity properties")
    void gridColumnNamesMapToEntityProperties() {
        Assertions.assertThat(GridPages.of(1, 25, "statusLabel", "asc").getSort())
                .containsExactly(Sort.Order.asc("status"));
        Assertions.assertThat(GridPages.of(1, 25, "customer_name", "desc", GridPages.SALES_ORDER_SORTS).getSort())
                .containsExactly(Sort.Order.desc("customer.companyName"));
        Assertions.assertThat(GridPages.of(1, 25, "quarryName", "asc", GridPages.BLOCK_SORTS).getSort())
                .containsExactly(Sort.Order.asc("quarry.name"));
        Assertions.assertThat(GridPages.of(1, 25, "supplierTypeLabel", "asc", GridPages.SUPPLIER_SORTS).getSort())
                .containsExactly(Sort.Order.asc("supplierType"));
        Assertions.assertThat(GridPages.of(1, 25, "businessUnitLabel", "desc", GridPages.MACHINE_SORTS).getSort())
                .containsExactly(Sort.Order.desc("businessUnit"));
    }

    @Test
    @DisplayName("blank search becomes null so JPQL null-checks skip LIKE")
    void blankSearchNormalizesToNull() {
        Assertions.assertThat(GridPages.normalizeSearch(null)).isNull();
        Assertions.assertThat(GridPages.normalizeSearch("  ")).isNull();
        Assertions.assertThat(GridPages.normalizeSearch(" SIP-1 ")).isEqualTo("SIP-1");
    }

    @Test
    @DisplayName("unknown sort properties retry with createdDate")
    void unknownSortPropertyFallsBack() {
        AtomicInteger calls = new AtomicInteger();
        Page<String> fallbackPage = new PageImpl<>(List.of("ok"));

        Page<String> result = GridPages.execute(1, 10, "notAColumn", "asc", pageable -> {
            int call = calls.incrementAndGet();
            if (call == 1) {
                Assertions.assertThat(pageable.getSort()).containsExactly(Sort.Order.asc("notAColumn"));
                throw new InvalidDataAccessApiUsageException("No property 'notAColumn' found");
            }
            Assertions.assertThat(pageable.getSort()).containsExactly(Sort.Order.asc("createdDate"));
            return fallbackPage;
        });

        Assertions.assertThat(result).isSameAs(fallbackPage);
        Assertions.assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("non-sort failures are not retried")
    void unrelatedFailuresAreNotSwallowed() {
        Assertions.assertThatThrownBy(() -> GridPages.execute(1, 10, "orderNo", "asc", pageable -> {
            throw new IllegalStateException("database down");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("database down");
    }
}
