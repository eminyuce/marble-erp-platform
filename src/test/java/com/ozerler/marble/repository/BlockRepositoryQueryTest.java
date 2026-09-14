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
}
