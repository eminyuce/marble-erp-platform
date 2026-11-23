package com.ozerler.marble.repository;

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.ozerler.marble.model.StockMovement;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockMovementType;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class StockMovementRepositoryPostgresTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    private StockMovementRepository repository;

    @Autowired
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
    @DisplayName("all-null filters do not send untyped parameters to PostgreSQL")
    void searchMovementsAllNullFiltersReturnsRows() {
        repository.save(StockMovement.builder()
                .movementCode("MV-NULL-FILTER")
                .movementDate(LocalDateTime.of(2026, 11, 23, 9, 0))
                .movementType(StockMovementType.TRANSFER)
                .sourceDepartment(BusinessUnit.QUARRY)
                .itemDescription("Null filter probe")
                .build());

        var page = repository.searchMovements(
                null, null, null, null, null, null, PageRequest.of(0, 25));

        Assertions.assertThat(page.getTotalElements()).isPositive();
        Assertions.assertThat(page.getContent())
                .extracting(StockMovement::getMovementCode)
                .contains("MV-NULL-FILTER");
    }

    @Test
    @DisplayName("a typed source filter still matches when the other filters are null")
    void searchMovementsOnlySourceDepartmentMatchesQuarry() {
        repository.save(StockMovement.builder()
                .movementCode("MV-QUARRY-ONLY")
                .movementDate(LocalDateTime.of(2026, 11, 23, 10, 0))
                .movementType(StockMovementType.OUTGOING)
                .sourceDepartment(BusinessUnit.QUARRY)
                .itemDescription("Quarry filter probe")
                .build());

        var page = repository.searchMovements(
                null, BusinessUnit.QUARRY, null, null, null, null, PageRequest.of(0, 25));

        Assertions.assertThat(page.getContent())
                .extracting(StockMovement::getMovementCode)
                .contains("MV-QUARRY-ONLY");
        Assertions.assertThat(page.getContent())
                .allMatch(movement -> movement.getSourceDepartment() == BusinessUnit.QUARRY);
    }

    @Test
    @DisplayName("a second update of a stock movement with a stale version is rejected")
    void secondUpdateWithStaleVersionIsRejected() {
        StockMovement saved = repository.saveAndFlush(StockMovement.builder()
                .movementCode("MV-LOCK")
                .movementDate(LocalDateTime.of(2026, 11, 23, 11, 0))
                .movementType(StockMovementType.TRANSFER)
                .sourceDepartment(BusinessUnit.FACTORY)
                .itemDescription("lock probe")
                .build());
        StockMovement first = repository.findById(saved.getId()).orElseThrow();
        entityManager.detach(first);
        StockMovement second = repository.findById(saved.getId()).orElseThrow();
        entityManager.detach(second);
        first.setItemDescription("first clerk");
        repository.saveAndFlush(first);
        second.setItemDescription("second clerk");
        Assertions.assertThatThrownBy(() -> repository.saveAndFlush(second))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
