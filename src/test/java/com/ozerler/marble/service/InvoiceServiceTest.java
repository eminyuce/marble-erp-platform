package com.ozerler.marble.service;

import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.*;
import com.ozerler.marble.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
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
                quarryInventoryService
        );
    }

    @Test
    @DisplayName("createInvoice throws exception when stock quantity is insufficient to prevent negative stock")
    void createInvoice_InsufficientStock_ThrowsException() {
        StockItem item = StockItem.builder()
                .id(20L)
                .itemCode("SRF-01")
                .description("Matkap Ucu")
                .quantity(new BigDecimal("10"))
                .unit("adet")
                .build();

        when(stockItemRepository.findById(20L)).thenReturn(Optional.of(item));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        List<InvoiceService.InvoiceItemForm> items = List.of(
                new InvoiceService.InvoiceItemForm("Matkap Ucu", "Ocak", new BigDecimal("15"), "adet", new BigDecimal("50.00"), null, null, 20L, null, null)
        );

        assertThatThrownBy(() -> invoiceService.createInvoice(
                "FAT-001", LocalDate.now(), null, InvoiceType.SALES, BusinessUnit.QUARRY,
                null, null, null, null, new BigDecimal("20"), false, "Müşteri A", null, items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Yetersiz stok")
                .hasMessageContaining("Mevcut stok: 10 adet");
    }

    @Test
    @DisplayName("createInvoice transfers block to IN_TRANSIT when dispatched to Factory")
    void createInvoice_DispatchesBlockToFactoryInTransit() {
        Block block = Block.builder()
                .id(5L)
                .blockCode("BLK-100")
                .status(BlockStatus.PRODUCED)
                .estimatedTonnage(new BigDecimal("15"))
                .build();

        when(blockRepository.findById(5L)).thenReturn(Optional.of(block));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        List<InvoiceService.InvoiceItemForm> items = List.of(
                new InvoiceService.InvoiceItemForm("Blok BLK-100", "Sevk", BigDecimal.ONE, "ton", new BigDecimal("1000.00"), 5L, null, null, null, null)
        );

        Invoice inv = invoiceService.createInvoice(
                "FAT-002", LocalDate.now(), null, InvoiceType.SALES, BusinessUnit.QUARRY,
                null, null, BusinessUnit.FACTORY, null, BigDecimal.ZERO, false, null, "Dahili Sevk", items);

        assertThat(block.getStatus()).isEqualTo(BlockStatus.IN_TRANSIT);
        assertThat(block.getCurrentLocation()).isNull();
        verify(blockRepository).save(block);
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("createInvoice with purchase and directExpense=false adds to quarry stock")
    void createInvoice_PurchaseStockAddition() {

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        List<InvoiceService.InvoiceItemForm> items = List.of(
                new InvoiceService.InvoiceItemForm("Mazot (Dizel)", "Depo Dolumu", new BigDecimal("5000"), "litre", new BigDecimal("42.00"), null, null, null, null, null)
        );

        Invoice inv = invoiceService.createInvoice(
                "ALF-001", LocalDate.now(), null, InvoiceType.PURCHASE, BusinessUnit.QUARRY,
                null, null, null, null, new BigDecimal("20"), false, "Tedarikçi Petrol", "Yakıt Alımı", items);

        verify(quarryInventoryService).addFuelStock(eq(new BigDecimal("5000")), eq(new BigDecimal("42.00")), any(), any());
        verify(costTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createInvoice with purchase and directExpense=true records operating expense with net amount")
    void createInvoice_PurchaseDirectExpense() {
        CostCenter quarryCenter = CostCenter.builder()
                .id(1L)
                .code("CC-001")
                .name("Ocak Maliyet Merkezi")
                .businessUnit(BusinessUnit.QUARRY)
                .build();


        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(BusinessUnit.QUARRY))
                .thenReturn(Optional.of(quarryCenter));

        List<InvoiceService.InvoiceItemForm> items = List.of(
                new InvoiceService.InvoiceItemForm("Elektrik Faturası", "Mart 2026", BigDecimal.ONE, "adet", new BigDecimal("25000.00"), null, null, null, null, null)
        );

        Invoice inv = invoiceService.createInvoice(
                "ALF-002", LocalDate.now(), null, InvoiceType.PURCHASE, BusinessUnit.QUARRY,
                null, null, null, null, new BigDecimal("20"), true, "TEDAŞ", "Elektrik", items);

        // Subtotal (Net) is 25000, Total with 20% KDV is 30000. CostTransaction amount MUST be net (25000)
        verify(costTransactionRepository).save(argThat(tx ->
                tx.getAmount().compareTo(new BigDecimal("25000.00")) == 0
                && tx.getBusinessUnit() == BusinessUnit.QUARRY
        ));
        verify(quarryInventoryService, never()).addFuelStock(any(), any(), any(), any());
    }

    @Test
    @DisplayName("createInvoice with DRAFT status does not change stock or process movements")
    void createInvoice_DraftStatus_DoesNotProcessStock() {
        StockItem item = StockItem.builder()
                .id(20L)
                .itemCode("SRF-01")
                .description("Matkap Ucu")
                .quantity(new BigDecimal("10"))
                .unit("adet")
                .build();

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        List<InvoiceService.InvoiceItemForm> items = List.of(
                new InvoiceService.InvoiceItemForm("Matkap Ucu", "Ocak", new BigDecimal("5"), "adet", new BigDecimal("50.00"), null, null, 20L, null, null)
        );

        Invoice inv = invoiceService.createInvoice(
                "FAT-DRF-001", LocalDate.now(), null, InvoiceType.SALES, BusinessUnit.QUARRY,
                null, null, null, null, new BigDecimal("20"), false, InvoiceStatus.DRAFT, "Müşteri A", null, items);

        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(inv.getStockProcessed()).isFalse();
        verify(stockItemRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("finalizeInvoice deducts stock and records movement idempotently")
    void finalizeInvoice_DeductsStock() {
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

        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice finalized = invoiceService.finalizeInvoice(100L);

        assertThat(finalized.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(finalized.getStockProcessed()).isTrue();
        assertThat(item.getQuantity()).isEqualByComparingTo("7");
        verify(stockItemRepository).save(item);
        verify(stockMovementRepository).save(any(StockMovement.class));

        // Idempotency: second finalize does not deduct stock again
        invoiceService.finalizeInvoice(100L);
        assertThat(item.getQuantity()).isEqualByComparingTo("7");
    }

    @Test
    @DisplayName("cancelInvoice restores deducted stock and reverses movements idempotently")
    void cancelInvoice_RestoresStock() {
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

        when(invoiceRepository.findById(200L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice cancelled = invoiceService.cancelInvoice(200L, "Müşteri vazgeçti");

        assertThat(cancelled.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(cancelled.getStockProcessed()).isFalse();
        assertThat(item.getQuantity()).isEqualByComparingTo("10"); // 7 + 3
        verify(stockItemRepository).save(item);
        verify(stockMovementRepository).save(any(StockMovement.class));

        // Idempotency: second cancel does not restore again
        invoiceService.cancelInvoice(200L, "Tekrar iptal");
        assertThat(item.getQuantity()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("createInvoice saves quarryCategory correctly for purchase invoice")
    void createInvoice_PurchaseWithQuarryCategory_PersistsCategory() {
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        List<InvoiceService.InvoiceItemForm> items = List.of(
                new InvoiceService.InvoiceItemForm("Ocak Elektrik Faturası", "Ocak", new BigDecimal("1"), "adet", new BigDecimal("15000.00"), null, null, null, null, null)
        );

        Invoice inv = invoiceService.createInvoice(
                "FAT-ELK-001", LocalDate.now(), null, InvoiceType.PURCHASE, BusinessUnit.QUARRY,
                null, null, null, null, new BigDecimal("20"), false, InvoiceStatus.ISSUED, "Tedaş A.Ş.",
                null, items, QuarryCategory.ELEKTRIK);

        assertThat(inv.getQuarryCategory()).isEqualTo(QuarryCategory.ELEKTRIK);
        assertThat(inv.getInvoiceType()).isEqualTo(InvoiceType.PURCHASE);
    }
}
