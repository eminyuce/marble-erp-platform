package com.ozerler.marble.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.util.GridPages;
import com.ozerler.marble.web.GridColumnFilter;
import com.ozerler.marble.web.GridColumnFilterContext;
import com.ozerler.marble.web.GridColumnFilters;
import com.ozerler.marble.web.GridFilterOperator;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GridColumnFilterPostgresTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    private BlockRepository blockRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.session.jdbc.initialize-schema", () -> "never");
    }

    @Test
    @DisplayName("LIKE wildcards in the user value are treated literally on PostgreSQL")
    void likeWildcardsAreLiteralOnPostgres() {
        Quarry quarry = persist(Quarry.builder().code("Q-LIKE").name("Wildcard Ocağı").location("Burdur").build());
        persist(block(quarry, "FLT%WILD", "Bej"));
        persist(block(quarry, "FLT-OTHER", "Gri"));
        entityManager.flush();

        Map<String, String> aliases = GridPages.BLOCK_SORTS;
        GridColumnFilters filters = newFilters("blockCode", GridFilterOperator.CONTAINS, "BLK%W");
        try (GridColumnFilterContext ignored = GridColumnFilterContext.open(aliases, filters)) {
            Page<Block> page = blockRepository.searchBlocksWithStockFilter(
                    null, null, null, "ALL", PageRequest.of(0, 20));
            Assertions.assertThat(page.getContent())
                    .extracting(Block::getBlockCode)
                    .contains("FLT%WILD").doesNotContain("FLT-OTHER");
        }

        filters = newFilters("quarryName", GridFilterOperator.EQUALS, "Wildcard Ocağı");
        try (GridColumnFilterContext ignored = GridColumnFilterContext.open(aliases, filters)) {
            Page<Block> page = blockRepository.searchBlocksWithStockFilter(
                    null, null, null, "ALL", PageRequest.of(0, 20));
            Assertions.assertThat(page.getContent())
                    .extracting(Block::getBlockCode)
                    .contains("FLT%WILD", "FLT-OTHER");
        }

        filters = newFilters("blockCode", GridFilterOperator.NOT_CONTAINS, "OTHER");
        try (GridColumnFilterContext ignored = GridColumnFilterContext.open(aliases, filters)) {
            Page<Block> page = blockRepository.searchBlocksWithStockFilter(
                    null, null, null, "ALL", PageRequest.of(0, 20));
            Assertions.assertThat(page.getContent())
                    .extracting(Block::getBlockCode)
                    .contains("FLT%WILD")
                    .doesNotContain("FLT-OTHER");
        }

        filters = newFilters("blockCode", GridFilterOperator.STARTS_WITH, "FLT-");
        try (GridColumnFilterContext ignored = GridColumnFilterContext.open(aliases, filters)) {
            Page<Block> page = blockRepository.searchBlocksWithStockFilter(
                    null, null, null, "ALL", PageRequest.of(0, 20));
            Assertions.assertThat(page.getContent())
                    .extracting(Block::getBlockCode)
                    .contains("FLT-OTHER").doesNotContain("FLT%WILD");
        }
    }

    private static GridColumnFilters newFilters(String key, GridFilterOperator operator, String value) {
        return GridColumnFilters.of(new GridColumnFilter(key, operator, value));
    }

    private Block block(Quarry quarry, String code, String stone) {
        return Block.builder().quarry(quarry).blockCode(code).extractionDate(LocalDate.of(2026, 11, 2))
                .widthCm(100).lengthCm(200).heightCm(150).volumeM3(new BigDecimal("3.000"))
                .theoreticalWeightKg(new BigDecimal("8000.00")).actualWeightKg(new BigDecimal("7900.00"))
                .stoneType(stone).build();
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
