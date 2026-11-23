package com.ozerler.marble.service;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ozerler.marble.mapper.InvoiceMapper;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.InvoiceItem;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.StockMovement;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.InvoiceStatus;
import com.ozerler.marble.model.enums.InvoiceType;
import com.ozerler.marble.model.enums.QuarryCategory;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.repository.BlockCustomerMarkRepository;
import com.ozerler.marble.repository.BlockLocationMovementRepository;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.InvoiceItemRepository;
import com.ozerler.marble.repository.InvoiceRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.StockItemRepository;
import com.ozerler.marble.repository.StockLocationRepository;
import com.ozerler.marble.repository.StockMovementRepository;
import com.ozerler.marble.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceItemRepository invoiceItemRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private StockItemRepository stockItemRepository;
    @Mock
    private SlabRepository slabRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private StockLocationRepository stockLocationRepository;
    @Mock
    private BlockLocationMovementRepository movementRepository;
    @Mock
    private CostTransactionRepository costTransactionRepository;
    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private QuarryInventoryService quarryInventoryService;
    @Mock
    private FactoryInventoryService factoryInventoryService;
    @Mock
    private BlockCustomerMarkRepository blockCustomerMarkRepository;
    @Mock
    private CariTransactionService cariTransactionService;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(costCenterRepository.findAll(ArgumentMatchers.any(Pageable.class)))
                .thenReturn(Page.empty());
        invoiceService = new InvoiceService(
                invoiceRepository,
                invoiceItemRepository,
                customerRepository,
                supplierRepository,
                blockRepository,
                stockItemRepository,
                slabRepository,
                projectRepository,
                stockLocationRepository,
                movementRepository,
                costTransactionRepository,
                costCenterRepository,
                stockMovementRepository,
                quarryInventoryService,
                factoryInventoryService,
                blockCustomerMarkRepository,
                cariTransactionService,
                Mockito.mock(InvoiceMapper.class)
        );
    }

    @Test
    @DisplayName("createInvoice throws exception when stock quantity is insufficient to prevent negative stock")
    void createInvoiceInsufficientStockThrowsException() {
        StockItem item = StockItem.builder()
                .id(20L)
                .itemCode("SRF-01")
                .description("Matkap Ucu")
                .quantity(new BigDecimal("10"))
                .unit("adet")
                .build();

        Mockito.when(stockItemRepository.findById(20L)).thenReturn(Optional.of(item));

        Mockito.when(invoiceRepository.save(ArgumentMatchers.any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        List<InvoiceService.InvoiceItemForm> items = List.of(
                new InvoiceService.InvoiceItemForm("Matkap Ucu", "Ocak", new BigDecimal("15"), "adet", new BigDecimal("50.00"), null, null, 20L, null, null)
        );

        Assertions.assertThatThrownBy(() -> invoiceService.createInvoice(
                "FAT-001", LocalDate.now(), null, InvoiceType.SALES, BusinessUnit.QUARRY,
                null, null, null, null, new BigDecimal("20"), false, "Müşteri A", null, items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Yetersiz stok")
                .hasMessageContaining("Mevcut stok: 10 adet");
    }

    @Test
    @DisplayName("createInvoice transfers block to IN_TRANSIT when dispatched to Factory")
    void createInvoiceDispatchesBlockToFactoryInTransit() {
        Block block = Block.builder()
                .id(5L)
                .blockCode("BLK-100")
                .status(BlockStatus.PRODUCED)
                .estimatedTonnage(new BigDecimal("15"))
                .build();

        Mockito.when(blockRepository.findById(5L)).thenReturn(Optional.of(block));

        Mockito.when(invoiceRepository.save(ArgumentMatchers.any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        List<InvoiceService.InvoiceItemForm> items = List.of(
                new InvoiceService.InvoiceItemForm("Blok BLK-100", "Sevk", BigDecimal.ONE, "ton", new BigDecimal("1000.00"), 5L, null, null, null, null)
        );

        Invoice inv = invoiceService.createInvoice(
                "FAT-002", LocalDate.now(), null, InvoiceType.SALES, BusinessUnit.QUARRY,
                null, null, BusinessUnit.FACTORY, null, BigDecimal.ZERO, false, null, "Dahili Sevk", items);

        Assertions.assertThat(block.getStatus()).isEqualTo(BlockStatus.IN_TRANSIT);
        Assertions.assertThat(block.getCurrentLocation()).isNull();
        Mockito.verify(blockRepository).save(block);
        Mockito.verify(stockMovementRepository).save(ArgumentMatchers.any(StockMovement.class));
    }

    @Test
    @DisplayName("createInvoice with purchase and directExpense=false adds to quarry stock")
    void createInvoicePurchaseStockAddition() {

        Mockito.when(invoiceRepository.save(ArgumentMatchers.any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        List<InvoiceService.InvoiceItemForm> items = List.of(
                new InvoiceService.InvoiceItemForm("Mazot (Dizel)", "Depo Dolumu", new BigDecimal("5000"), "litre", new BigDecimal("42.00"), null, null, null, null, null)
        );

        Invoice inv = invoiceService.createInvoice(
                "ALF-001", LocalDate.now(), null, InvoiceType.PURCHASE, BusinessUnit.QUARRY,
                null, null, null, null, new BigDecimal("20"), false, "Tedarikçi Petrol", "Yakıt Alımı", items);

        Mockito.verify(quarryInventoryService).addFuelStock(Mockito.eq(new BigDecimal("5000.00")), Mockito.eq(new BigDecimal("42.00")), ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.verify(costTransactionRepository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("createInvoice with purchase and directExpense=true records operating expense with net amount")
    void createInvoicePurchaseDirectExpense() {
        CostCenter quarryCenter = CostCenter.builder()
                .id(1L)
                .code("CC-001")
                .name("Ocak Maliyet Merkezi")
                .businessUnit(BusinessUnit.QUARRY)
                .build();


        Mockito.when(invoiceRepository.save(ArgumentMatchers.any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(BusinessUnit.QUARRY))
                .thenReturn(Optional.of(quarryCenter));

        List<InvoiceService.InvoiceItemForm> items = List.of(
                new InvoiceService.InvoiceItemForm("Elektrik Faturası", "Mart 2026", BigDecimal.ONE, "adet", new BigDecimal("25000.00"), null, null, null, null, null)
        );

        Invoice inv = invoiceService.createInvoice(
                "ALF-002", LocalDate.now(), null, InvoiceType.PURCHASE, BusinessUnit.QUARRY,
                null, null, null, null, new BigDecimal("20"), true, "TEDAŞ", "Elektrik", items);

        // Subtotal (Net) is 25000, Total with 20% KDV is 30000. CostTransaction amount MUST be net (25000)
        Mockito.verify(costTransactionRepository).save(Mockito.argThat(tx ->
                tx.getAmount().compareTo(new BigDecimal("25000.00")) == 0
                && tx.getBusinessUnit() == BusinessUnit.QUARRY
        ));
        Mockito.verify(quarryInventoryService, Mockito.never()).addFuelStock(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    @DisplayName("createInvoice with DRAFT status does not change stock or process movements")
    void createInvoiceDraftStatusDoesNotProcessStock() {
        StockItem item = StockItem.builder()
                .id(20L)
                .itemCode("SRF-01")
                .description("Matkap Ucu")
                .quantity(new BigDecimal("10"))
                .unit("adet")
                .build();

        Mockito.when(invoiceRepository.save(ArgumentMatchers.any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        List<InvoiceService.InvoiceItemForm> items = List.of(
                new InvoiceService.InvoiceItemForm("Matkap Ucu", "Ocak", new BigDecimal("5"), "adet", new BigDecimal("50.00"), null, null, 20L, null, null)
        );

        Invoice inv = invoiceService.createInvoice(
                "FAT-DRF-001", LocalDate.now(), null, InvoiceType.SALES, BusinessUnit.QUARRY,
                null, null, null, null, new BigDecimal("20"), false, InvoiceStatus.DRAFT, "Müşteri A", null, items);

        Assertions.assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        Assertions.assertThat(inv.getStockProcessed()).isFalse();
        Mockito.verify(stockItemRepository, Mockito.never()).save(ArgumentMatchers.any());
        Mockito.verify(stockMovementRepository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("finalizeInvoice deducts stock and records movement idempotently")
    void finalizeInvoiceDeductsStock() {
        StockItem item = StockItem.builder()
                .id(20L)
                .itemCode("SRF-01")
                .description("Matkap Ucu")
                .quantity(new BigDecimal("10"))
                .unit("adet")
                .build();

        InvoiceItem invoiceItem = InvoiceItem.builder()
                .id(1L)
                .productName("Matkap Ucu")
                .quantity(new BigDecimal("3"))
                .unit("adet")
                .stockItem(item)
                .build();

        Invoice invoice = Invoice.builder()
                .id(100L)
                .invoiceNo("FAT-FIN-001")
                .invoiceType(InvoiceType.SALES)
                .department(BusinessUnit.QUARRY)
                .status(InvoiceStatus.DRAFT)
                .stockProcessed(false)
                .items(new ArrayList<>(List.of(invoiceItem)))
                .build();

        Mockito.when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        Mockito.when(invoiceRepository.save(ArgumentMatchers.any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice finalized = invoiceService.finalizeInvoice(100L);

        Assertions.assertThat(finalized.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        Assertions.assertThat(finalized.getStockProcessed()).isTrue();
        Assertions.assertThat(item.getQuantity()).isEqualByComparingTo("7");
        Mockito.verify(stockItemRepository).save(item);
        Mockito.verify(stockMovementRepository).save(ArgumentMatchers.any(StockMovement.class));

        // Idempotency: second finalize does not deduct stock again
        invoiceService.finalizeInvoice(100L);
        Assertions.assertThat(item.getQuantity()).isEqualByComparingTo("7");
    }

    @Test
    @DisplayName("cancelInvoice restores deducted stock and reverses movements idempotently")
    void cancelInvoiceRestoresStock() {
        StockItem item = StockItem.builder()
                .id(20L)
                .itemCode("SRF-01")
                .description("Matkap Ucu")
                .quantity(new BigDecimal("7"))
                .unit("adet")
                .build();

        InvoiceItem invoiceItem = InvoiceItem.builder()
                .id(1L)
                .productName("Matkap Ucu")
                .quantity(new BigDecimal("3"))
                .unit("adet")
                .stockItem(item)
                .build();

        Invoice invoice = Invoice.builder()
                .id(200L)
                .invoiceNo("FAT-CAN-001")
                .invoiceType(InvoiceType.SALES)
                .department(BusinessUnit.QUARRY)
                .status(InvoiceStatus.ISSUED)
                .stockProcessed(true)
                .items(new ArrayList<>(List.of(invoiceItem)))
                .build();

        Mockito.when(invoiceRepository.findById(200L)).thenReturn(Optional.of(invoice));
        Mockito.when(invoiceRepository.save(ArgumentMatchers.any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice cancelled = invoiceService.cancelInvoice(200L, "Müşteri vazgeçti");

        Assertions.assertThat(cancelled.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        Assertions.assertThat(cancelled.getStockProcessed()).isFalse();
        Assertions.assertThat(item.getQuantity()).isEqualByComparingTo("10"); // 7 + 3
        Mockito.verify(stockItemRepository).save(item);
        Mockito.verify(stockMovementRepository).save(ArgumentMatchers.any(StockMovement.class));

        // Idempotency: second cancel does not restore again
        invoiceService.cancelInvoice(200L, "Tekrar iptal");
        Assertions.assertThat(item.getQuantity()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("createInvoice saves quarryCategory correctly for purchase invoice")
    void createInvoicePurchaseWithQuarryCategoryPersistsCategory() {
        Mockito.when(invoiceRepository.save(ArgumentMatchers.any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        List<InvoiceService.InvoiceItemForm> items = List.of(
                new InvoiceService.InvoiceItemForm("Ocak Elektrik Faturası", "Ocak", new BigDecimal("1"), "adet", new BigDecimal("15000.00"), null, null, null, null, null)
        );

        Invoice inv = invoiceService.createInvoice(
                "FAT-ELK-001", LocalDate.now(), null, InvoiceType.PURCHASE, BusinessUnit.QUARRY,
                null, null, null, null, new BigDecimal("20"), false, InvoiceStatus.ISSUED, "Tedaş A.Ş.",
                null, items, QuarryCategory.ELEKTRIK);

        Assertions.assertThat(inv.getQuarryCategory()).isEqualTo(QuarryCategory.ELEKTRIK);
        Assertions.assertThat(inv.getInvoiceType()).isEqualTo(InvoiceType.PURCHASE);
    }

    @Test
    @DisplayName("getInvoiceById fetches invoice with details and initializes collections")
    void getInvoiceByIdSuccessInitializesCollections() {
        Customer customer = Customer.builder().id(10L).companyName("Müşteri A").build();
        InvoiceItem item = InvoiceItem.builder().id(1L).productName("Blok").build();
        Invoice invoice = Invoice.builder()
                .id(1L)
                .invoiceNo("FAT-001")
                .customer(customer)
                .items(new ArrayList<>(List.of(item)))
                .collections(new ArrayList<>())
                .build();

        Mockito.when(invoiceRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(invoice));

        Invoice result = invoiceService.getInvoiceById(1L);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getInvoiceNo()).isEqualTo("FAT-001");
        Assertions.assertThat(result.getItems()).hasSize(1);
        Mockito.verify(invoiceRepository).findByIdWithDetails(1L);
    }

    @Test
    @DisplayName("getInvoiceById falls back to findById if findByIdWithDetails is empty")
    void getInvoiceByIdFallbackToFindById() {
        Invoice invoice = Invoice.builder()
                .id(2L)
                .invoiceNo("FAT-002")
                .items(new ArrayList<>())
                .collections(new ArrayList<>())
                .build();

        Mockito.when(invoiceRepository.findByIdWithDetails(2L)).thenReturn(Optional.empty());
        Mockito.when(invoiceRepository.findById(2L)).thenReturn(Optional.of(invoice));

        Invoice result = invoiceService.getInvoiceById(2L);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getInvoiceNo()).isEqualTo("FAT-002");
        Mockito.verify(invoiceRepository).findByIdWithDetails(2L);
        Mockito.verify(invoiceRepository).findById(2L);
    }

    @Test
    @DisplayName("getInvoiceById throws IllegalArgumentException when invoice is not found")
    void getInvoiceByIdNotFoundThrowsException() {
        Mockito.when(invoiceRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());
        Mockito.when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> invoiceService.getInvoiceById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fatura bulunamadı: 999");
    }

    @Test
    @DisplayName("Sales invoice with m.tül unit calculates m2 and deducts calculated m2 from stock")
    void createInvoiceRunningMeterSalesDeductsCalculatedM2() {
        StockItem sized = StockItem.builder()
                .id(100L)
                .itemCode("EBT-M1")
                .description("Mermer Basamak")
                .widthCm(new BigDecimal("50"))
                .quantity(new BigDecimal("20.00"))
                .unit("m2")
                .status("AVAILABLE")
                .build();

        Mockito.when(stockItemRepository.findById(100L)).thenReturn(Optional.of(sized));
        Mockito.when(invoiceRepository.save(ArgumentMatchers.any(Invoice.class))).thenAnswer(inv -> {
            Invoice invObj = inv.getArgument(0);
            invObj.setId(101L);
            return invObj;
        });

        List<InvoiceService.InvoiceItemForm> items = List.of(
                new InvoiceService.InvoiceItemForm(
                        "Basamak", "50cm genişlik",
                        new BigDecimal("10"), // 10 m.tül
                        "m.tül",
                        new BigDecimal("100"),
                        null, null, 100L,
                        new BigDecimal("50"), // width = 50 cm -> 10 * 50 / 100 = 5.00 m2
                        null
                )
        );

        Invoice saved = invoiceService.createInvoice(
                "FAT-MT-01", LocalDate.now(), null, InvoiceType.SALES, BusinessUnit.WORKSHOP,
                null, null, null, null, new BigDecimal("20"), false, "Müşteri A", "Metretül Satış", items
        );

        Assertions.assertThat(saved).isNotNull();
        Assertions.assertThat(saved.getItems()).hasSize(1);
        InvoiceItem item = saved.getItems().get(0);
        Assertions.assertThat(item.getCalculatedM2()).isEqualByComparingTo("5.00");

        // Stock was 20.00, deducted 5.00 m2 -> should be 15.00 m2
        Assertions.assertThat(sized.getQuantity()).isEqualByComparingTo("15.00");
        Mockito.verify(stockItemRepository, Mockito.atLeastOnce()).save(sized);
    }

    @Test
    @DisplayName("Purchase invoice with m.tül unit calculates m2 and adds calculated m2 to stock")
    void createInvoiceRunningMeterPurchaseAddsCalculatedM2() {
        StockItem sized = StockItem.builder()
                .id(200L)
                .itemCode("EBT-P1")
                .description("Gelen Süpürgelik")
                .widthCm(new BigDecimal("10"))
                .quantity(new BigDecimal("5.00"))
                .unit("m2")
                .status("AVAILABLE")
                .build();

        Mockito.when(stockItemRepository.findById(200L)).thenReturn(Optional.of(sized));
        Mockito.when(invoiceRepository.save(ArgumentMatchers.any(Invoice.class))).thenAnswer(inv -> {
            Invoice invObj = inv.getArgument(0);
            invObj.setId(201L);
            return invObj;
        });

        List<InvoiceService.InvoiceItemForm> items = List.of(
                new InvoiceService.InvoiceItemForm(
                        "Süpürgelik", "10cm genişlik",
                        new BigDecimal("100"), // 100 m.tül
                        "m.tül",
                        new BigDecimal("50"),
                        null, null, 200L,
                        new BigDecimal("10"), // 10 cm width -> 100 * 10 / 100 = 10.00 m2
                        null
                )
        );

        Invoice saved = invoiceService.createInvoice(
                "ALIS-MT-01", LocalDate.now(), null, InvoiceType.PURCHASE, BusinessUnit.FACTORY,
                null, 1L, null, null, new BigDecimal("20"), false, "Tedarikçi A", "Metretül Alış", items
        );

        Assertions.assertThat(saved).isNotNull();
        Assertions.assertThat(saved.getItems()).hasSize(1);
        InvoiceItem item = saved.getItems().get(0);
        Assertions.assertThat(item.getCalculatedM2()).isEqualByComparingTo("10.00");

        // Stock was 5.00, added 10.00 m2 -> should be 15.00 m2
        Assertions.assertThat(sized.getQuantity()).isEqualByComparingTo("15.00");
        Mockito.verify(stockItemRepository, Mockito.atLeastOnce()).save(sized);
    }

    @Test
    @DisplayName("Internal transfer invoice creates counterpart purchase invoice and updates site project cost")
    void createInvoiceInternalTransferCreatesCounterpartPurchaseInvoiceAndUpdatesSiteCost() {
        Project project = Project.builder()
                .id(50L)
                .projectCode("PRJ-050")
                .name("Şantiye Plaza")
                .actualCost(new BigDecimal("10000.00"))
                .build();

        StockLocation siteYard = StockLocation.builder()
                .id(1L)
                .code("SNT-STK-01")
                .name("Şantiye Stoğu")
                .locationType(StockLocationType.SITE_STOCK)
                .active(true)
                .build();

        StockItem item = StockItem.builder()
                .id(500L)
                .itemCode("PLK-01")
                .description("Mermer Plaka")
                .quantity(new BigDecimal("50.00"))
                .unit("m2")
                .build();

        Mockito.when(projectRepository.findById(50L)).thenReturn(Optional.of(project));
        Mockito.when(stockItemRepository.findById(500L)).thenReturn(Optional.of(item));
        Mockito.when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.SITE_STOCK)).thenReturn(Optional.of(siteYard));
        Mockito.when(invoiceRepository.save(ArgumentMatchers.any(Invoice.class))).thenAnswer(inv -> {
            Invoice invObj = inv.getArgument(0);
            if (invObj.getId() == null) {
                invObj.setId(999L);
            }
            return invObj;
        });

        List<InvoiceService.InvoiceItemForm> items = List.of(
                new InvoiceService.InvoiceItemForm(
                        "Döşeme Taşı", "60x60",
                        new BigDecimal("20"),
                        "m2",
                        new BigDecimal("250.00"), // 20 * 250 = 5000 TL
                        null, null, 500L,
                        null, null
                )
        );

        Invoice sales = invoiceService.createInvoice(
                "FAT-TRF-01", LocalDate.now(), null,
                InvoiceType.SALES, BusinessUnit.FACTORY,
                null, null, BusinessUnit.SITE, 50L,
                BigDecimal.ZERO, false, null, "Fabrikadan Şantiyeye Sevk", items
        );

        Assertions.assertThat(sales).isNotNull();
        Assertions.assertThat(sales.getTargetDepartment()).isEqualTo(BusinessUnit.SITE);

        // Project cost should be updated by totalAmount (5000 TL + 10000 TL = 15000 TL)
        Assertions.assertThat(project.getActualCost()).isEqualByComparingTo("15000.00");
        Mockito.verify(projectRepository, Mockito.atLeastOnce()).save(project);

        // Verify that counterpart purchase invoice was saved with PURCHASE type and SITE department
        Mockito.verify(invoiceRepository, Mockito.times(1)).save(Mockito.argThat(inv ->
                inv.getInvoiceType() == InvoiceType.PURCHASE
                && inv.getDepartment() == BusinessUnit.SITE
                && inv.getTargetDepartment() == BusinessUnit.FACTORY
        ));
    }
}
