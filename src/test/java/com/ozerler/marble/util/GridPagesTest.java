package com.ozerler.marble.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GridPagesTest {

    @Test
    @DisplayName("blank sort field uses createdDate descending")
    void blankSortFieldUsesCreatedDate() {
        Pageable pageable = GridPages.of(1, 25, "", "desc");

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(25);
        assertThat(pageable.getSort()).containsExactly(Sort.Order.desc("createdDate"));
    }

    @Test
    @DisplayName("snake_case sort fields are converted to entity camelCase")
    void snakeCaseSortFieldIsCamelized() {
        assertThat(GridPages.toEntityProperty("order_no")).isEqualTo("orderNo");
        assertThat(GridPages.toEntityProperty("blockCode")).isEqualTo("blockCode");
    }

    @Test
    @DisplayName("unknown sort properties retry with createdDate")
    void unknownSortPropertyFallsBack() {
        AtomicInteger calls = new AtomicInteger();
        Page<String> fallbackPage = new PageImpl<>(List.of("ok"));

        Page<String> result = GridPages.execute(1, 10, "statusLabel", "asc", pageable -> {
            int call = calls.incrementAndGet();
            if (call == 1) {
                assertThat(pageable.getSort()).containsExactly(Sort.Order.asc("statusLabel"));
                throw new InvalidDataAccessApiUsageException("No property 'statusLabel' found");
            }
            assertThat(pageable.getSort()).containsExactly(Sort.Order.asc("createdDate"));
            return fallbackPage;
        });

        assertThat(result).isSameAs(fallbackPage);
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("non-sort failures are not retried")
    void unrelatedFailuresAreNotSwallowed() {
        assertThatThrownBy(() -> GridPages.execute(1, 10, "orderNo", "asc", pageable -> {
            throw new IllegalStateException("database down");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("database down");
    }
}
