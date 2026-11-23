package com.ozerler.marble.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.ozerler.marble.util.GridPages;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GridSortPostgresTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private CutOrderRepository cutOrderRepository;
    @Autowired
    private SalesOrderRepository salesOrderRepository;
    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired
    private ProductionOrderRepository productionOrderRepository;
    @Autowired
    private SlabRepository slabRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private MachineRepository machineRepository;
    @Autowired
    private StockLocationRepository stockLocationRepository;
    @Autowired
    private QuarryRepository quarryRepository;
    @Autowired
    private CostCenterRepository costCenterRepository;
    @Autowired
    private CostTransactionRepository costTransactionRepository;

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
    @DisplayName("Every grid sort key runs ascending and descending on PostgreSQL")
    void everyGridSortKeyRunsOnPostgres() {
        List<String> failures = new ArrayList<>();
        sort(failures, "blocks", GridPages.BLOCK_SORTS,
                page -> blockRepository.searchBlocksWithStockFilter(null, null, null, "ALL", page));
        for (String filter : List.of("ALL", "SOLD", "IN_TRANSIT", "MARKED", "DISPATCHED", "IN_STOCK")) {
            sort(failures, "blocks-" + filter, Map.of("quarryName", "quarry.name"),
                    page -> blockRepository.searchBlocksWithStockFilter(null, null, null, filter, page));
        }
        sort(failures, "production", GridPages.PRODUCTION_ORDER_SORTS,
                page -> productionOrderRepository.searchOrders(null, page));
        sort(failures, "slabs", GridPages.SLAB_SORTS, page -> slabRepository.searchSlabs(null, page));
        sort(failures, "workshop", GridPages.CUT_ORDER_SORTS,
                page -> cutOrderRepository.searchCutOrders(null, page));
        sort(failures, "projects", GridPages.PROJECT_SORTS,
                page -> projectRepository.searchProjects(null, page));
        sort(failures, "sales", GridPages.SALES_ORDER_SORTS,
                page -> salesOrderRepository.searchSalesOrders(null, page));
        sort(failures, "procurement", GridPages.PURCHASE_ORDER_SORTS,
                page -> purchaseOrderRepository.searchPurchaseOrders(null, page));
        sort(failures, "users", GridPages.USER_SORTS,
                page -> userRepository.searchActiveUsers(null, null, null, page));
        sort(failures, "suppliers", GridPages.SUPPLIER_SORTS,
                page -> supplierRepository.search(null, null, page));
        sort(failures, "customers", GridPages.CUSTOMER_SORTS,
                page -> customerRepository.search(null, null, page));
        sort(failures, "machines", GridPages.MACHINE_SORTS,
                page -> machineRepository.search(null, null, page));
        sort(failures, "stock-locations", GridPages.STOCK_LOCATION_SORTS,
                page -> stockLocationRepository.search(null, null, page));
        sort(failures, "quarries", GridPages.QUARRY_SORTS, page -> quarryRepository.search(null, page));
        sort(failures, "cost-centers", GridPages.COST_CENTER_SORTS,
                page -> costCenterRepository.search(null, null, page));
        sort(failures, "expenses", GridPages.EXPENSE_SORTS,
                page -> costTransactionRepository.searchExpenses(null, null, null, null, null, null, null, page));
        Assertions.assertThat(failures).isEmpty();
    }

    private static void sort(List<String> failures, String grid, Map<String, String> keys,
                             Function<Pageable, Page<?>> query) {
        for (String key : keys.keySet()) {
            for (String direction : List.of("asc", "desc")) {
                try {
                    Page<?> page = query.apply(GridPages.of(1, 20, key, direction, keys));
                    if (page == null) {
                        failures.add(grid + " sort=" + key + " dir=" + direction + " returned null");
                    }
                } catch (RuntimeException ex) {
                    failures.add(grid + " sort=" + key + " dir=" + direction + " -> " + ex.getMessage());
                }
            }
        }
    }
}
