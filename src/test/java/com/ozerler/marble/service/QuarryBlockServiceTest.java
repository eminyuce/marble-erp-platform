package com.ozerler.marble.service;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.BlockLocationMovement;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.repository.BlockCustomerMarkRepository;
import com.ozerler.marble.repository.BlockLocationMovementRepository;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.FactoryWorkOrderRepository;
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.QuarryRepository;
import com.ozerler.marble.repository.ShipmentItemRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.StockLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuarryBlockServiceTest {

    @Mock
    private BlockRepository blockRepository;
    @Mock
    private QuarryRepository quarryRepository;
    @Mock
    private StockLocationRepository stockLocationRepository;
    @Mock
    private BlockLocationMovementRepository movementRepository;
    @Mock
    private ExpenseService expenseService;
    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SlabRepository slabRepository;
    @Mock
    private FactoryWorkOrderRepository factoryWorkOrderRepository;
    @Mock
    private ProductionOrderRepository productionOrderRepository;
    @Mock
    private BlockCustomerMarkRepository blockCustomerMarkRepository;
    @Mock
    private CostTransactionRepository costTransactionRepository;
    @Mock
    private ShipmentItemRepository shipmentItemRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private CostAnalysisService costAnalysisService;
    @Mock
    private BlockCostCalculationService blockCostCalculationService;

    private QuarryBlockService quarryBlockService;

    @BeforeEach
    void setUp() {
        quarryBlockService = new QuarryBlockService(
                blockRepository, quarryRepository, null, stockLocationRepository, movementRepository,
                costAnalysisService, blockCostCalculationService, expenseService, costCenterRepository, customerRepository,
                slabRepository, factoryWorkOrderRepository, productionOrderRepository,
                blockCustomerMarkRepository, costTransactionRepository, shipmentItemRepository,
                fileStorageService);
    }

    @Test
    @DisplayName("getBlockWithDetails returns the block loaded with its quarry")
    void getBlockWithDetails_ReturnsBlock() {
        Block block = Block.builder()
                .id(5L)
                .blockCode("BLK-001")
                .quarry(Quarry.builder().id(1L).name("Ana Ocak").build())
                .build();
        when(blockRepository.findByIdWithQuarry(5L)).thenReturn(Optional.of(block));

        Block result = quarryBlockService.getBlockWithDetails(5L);

        assertThat(result.getBlockCode()).isEqualTo("BLK-001");
        assertThat(result.getQuarry().getName()).isEqualTo("Ana Ocak");
        verify(blockRepository).findByIdWithQuarry(5L);
    }

    @Test
    @DisplayName("getBlockWithDetails throws when the block is missing")
    void getBlockWithDetails_MissingBlock_Throws() {
        when(blockRepository.findByIdWithQuarry(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quarryBlockService.getBlockWithDetails(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getBlockWithDetails rejects a null id")
    void getBlockWithDetails_NullId_Throws() {
        assertThatThrownBy(() -> quarryBlockService.getBlockWithDetails(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("duplicate block codes are rejected on create")
    void registerBlock_DuplicateCode_Throws() {
        when(quarryRepository.findById(1L)).thenReturn(Optional.of(Quarry.builder().id(1L).name("Ocak").build()));
        when(blockRepository.existsByBlockCode("BLK-1")).thenReturn(true);

        assertThatThrownBy(() -> quarryBlockService.registerBlock(
                1L, "BLK-1", null, 100, 100, 100, BigDecimal.TEN, "Beyaz", null,
                null, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BLK-1");
        verify(blockRepository, never()).save(any());
    }

    @Test
    @DisplayName("factory transfer leaves quarry stock, enters factory yard and posts nakliye")
    void transferToFactory_MovesToFactoryYardAndPostsExpense() {
        Quarry quarry = Quarry.builder().id(1L).name("Ocak").specificGravity(new BigDecimal("2.70")).build();
        StockLocation production = StockLocation.builder()
                .id(1L).code("OCAK-URETIM").name("Üretim Sahası")
                .locationType(StockLocationType.PRODUCTION_YARD).businessUnit(BusinessUnit.QUARRY).build();
        StockLocation factoryYard = StockLocation.builder()
                .id(3L).code("FAB-BLOK").name("Fabrika Blok Sahası")
                .locationType(StockLocationType.FACTORY_BLOCK_YARD).businessUnit(BusinessUnit.FACTORY).build();
        Block block = Block.builder()
                .id(8L)
                .blockCode("BLK-008")
                .quarry(quarry)
                .status(BlockStatus.PRODUCED)
                .currentLocation(production)
                .widthCm(100)
                .lengthCm(100)
                .heightCm(100)
                .actualWeightKg(new BigDecimal("2700"))
                .extractionCost(BigDecimal.TEN)
                .transportCost(BigDecimal.ZERO)
                .build();
        CostCenter factoryTransport = CostCenter.builder()
                .id(6L).code("CC-006").name("Nakliye").businessUnit(BusinessUnit.FACTORY).build();

        when(blockRepository.findById(8L)).thenReturn(Optional.of(block));
        when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.FACTORY_BLOCK_YARD))
                .thenReturn(Optional.of(factoryYard));
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(costCenterRepository.findByCode("CC-006")).thenReturn(Optional.of(factoryTransport));

        Block saved = quarryBlockService.transferToFactory(8L, new BigDecimal("1500"));

        assertThat(saved.getStatus()).isEqualTo(BlockStatus.AT_FACTORY);
        assertThat(saved.getCurrentLocation().getLocationType()).isEqualTo(StockLocationType.FACTORY_BLOCK_YARD);
        assertThat(saved.getTransportCost()).isEqualByComparingTo("1500");
        ArgumentCaptor<ExpenseService.ExpenseDraft> expense = ArgumentCaptor.forClass(ExpenseService.ExpenseDraft.class);
        verify(expenseService).recordExpense(expense.capture());
        assertThat(expense.getValue().businessUnit()).isEqualTo(BusinessUnit.FACTORY);
        assertThat(expense.getValue().amount()).isEqualByComparingTo("1500");
        assertThat(expense.getValue().centerId()).isEqualTo(6L);
        verify(movementRepository).save(any());
    }

    @Test
    @DisplayName("external sale stores the customer on the sold block")
    void sellBlockExternally_SetsCustomer() {
        Block block = Block.builder()
                .id(4L)
                .blockCode("BLK-004")
                .status(BlockStatus.PRODUCED)
                .build();
        Customer customer = Customer.builder().id(2L).companyName("Mermer A.Ş.").build();
        when(blockRepository.findById(4L)).thenReturn(Optional.of(block));
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Block sold = quarryBlockService.sellBlockExternally(4L, 2L, new BigDecimal("750000"), java.time.LocalDate.now(), "Test satış");

        assertThat(sold.getStatus()).isEqualTo(BlockStatus.SOLD);
        assertThat(sold.getSoldCustomer().getCompanyName()).isEqualTo("Mermer A.Ş.");
        assertThat(sold.getSalePrice()).isEqualByComparingTo("750000");
        verify(movementRepository).save(any());
    }

    @Test
    @DisplayName("registering a block without actual weighbridge weight calculates theoretical weight from dimensions and quarry specific gravity")
    void registerBlock_WithoutActualWeight_CalculatesTheoreticalMetrics() {
        Quarry quarry = Quarry.builder()
                .id(1L)
                .name("Afyon Ocağı")
                .specificGravity(new BigDecimal("2.70"))
                .build();
        StockLocation production = StockLocation.builder()
                .id(1L)
                .code("OCAK-URETIM")
                .name("Üretim Sahası")
                .locationType(StockLocationType.PRODUCTION_YARD)
                .businessUnit(BusinessUnit.QUARRY)
                .build();

        when(quarryRepository.findById(1L)).thenReturn(Optional.of(quarry));
        when(blockRepository.existsByBlockCode("BLK-AUTO-01")).thenReturn(false);
        when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.PRODUCTION_YARD))
                .thenReturn(Optional.of(production));
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // En: 200 cm, Boy: 250 cm, Yükseklik: 150 cm -> Hacim: 7.500 m³ -> 7.5 * 2.70 = 20.25 ton = 20250.00 kg
        Block saved = quarryBlockService.registerBlock(
                1L, "BLK-AUTO-01", null, 200, 250, 150, null, "Beyaz Mermer", "Açık",
                QualityGrade.A, 0, "Notlar", null);

        assertThat(saved).isNotNull();
        assertThat(saved.getVolumeM3()).isEqualByComparingTo(new BigDecimal("7.500"));
        assertThat(saved.getTheoreticalWeightKg()).isEqualByComparingTo(new BigDecimal("20250.00"));
        assertThat(saved.getActualWeightKg()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getApproximateTonnage()).isEqualByComparingTo(new BigDecimal("20.25"));
        verify(blockRepository).save(any(Block.class));
    }

    @Test
    @DisplayName("canDeleteBlock returns true when block is PRODUCED, unsold, has no transport cost and is unused downstream")
    void canDeleteBlock_WhenUnused_ReturnsTrue() {
        Block block = Block.builder()
                .id(10L)
                .blockCode("BLK-DELETE-01")
                .status(BlockStatus.PRODUCED)
                .transportCost(BigDecimal.ZERO)
                .build();

        when(blockRepository.findById(10L)).thenReturn(Optional.of(block));
        when(slabRepository.existsByBlockId(10L)).thenReturn(false);
        when(factoryWorkOrderRepository.findFirstByBlockIdOrderByIdDesc(10L)).thenReturn(Optional.empty());
        when(productionOrderRepository.findByBlockId(10L)).thenReturn(List.of());
        when(blockCustomerMarkRepository.existsByBlockId(10L)).thenReturn(false);
        when(costTransactionRepository.findByBlockId(10L)).thenReturn(List.of());
        when(shipmentItemRepository.existsByBlockId(10L)).thenReturn(false);

        boolean canDelete = quarryBlockService.canDeleteBlock(10L);

        assertThat(canDelete).isTrue();
    }

    @Test
    @DisplayName("canDeleteBlock returns false when block has slabs cut from it")
    void canDeleteBlock_WhenUsedInSlabs_ReturnsFalse() {
        Block block = Block.builder()
                .id(11L)
                .blockCode("BLK-DELETE-02")
                .status(BlockStatus.PRODUCED)
                .transportCost(BigDecimal.ZERO)
                .build();

        when(blockRepository.findById(11L)).thenReturn(Optional.of(block));
        when(slabRepository.existsByBlockId(11L)).thenReturn(true);

        boolean canDelete = quarryBlockService.canDeleteBlock(11L);

        assertThat(canDelete).isFalse();
    }

    @Test
    @DisplayName("deleteBlock deletes block and its initial location movements when completely unused")
    void deleteBlock_WhenUnused_DeletesSuccessfully() {
        Block block = Block.builder()
                .id(12L)
                .blockCode("BLK-DELETE-03")
                .status(BlockStatus.PRODUCED)
                .transportCost(BigDecimal.ZERO)
                .build();
        BlockLocationMovement mv = BlockLocationMovement.builder().id(101L).block(block).build();

        when(blockRepository.findById(12L)).thenReturn(Optional.of(block));
        when(slabRepository.existsByBlockId(12L)).thenReturn(false);
        when(factoryWorkOrderRepository.findFirstByBlockIdOrderByIdDesc(12L)).thenReturn(Optional.empty());
        when(productionOrderRepository.findByBlockId(12L)).thenReturn(List.of());
        when(blockCustomerMarkRepository.existsByBlockId(12L)).thenReturn(false);
        when(costTransactionRepository.findByBlockId(12L)).thenReturn(List.of());
        when(shipmentItemRepository.existsByBlockId(12L)).thenReturn(false);
        when(movementRepository.findByBlockIdOrderByCreatedDateDesc(12L)).thenReturn(List.of(mv));

        quarryBlockService.deleteBlock(12L);

        verify(movementRepository).deleteAll(List.of(mv));
        verify(blockRepository).delete(block);
    }

    @Test
    @DisplayName("deleteBlock throws IllegalStateException when block is not deletable")
    void deleteBlock_WhenUsed_ThrowsIllegalStateException() {
        Block block = Block.builder()
                .id(13L)
                .blockCode("BLK-DELETE-04")
                .status(BlockStatus.AT_FACTORY)
                .build();

        when(blockRepository.findById(13L)).thenReturn(Optional.of(block));

        assertThatThrownBy(() -> quarryBlockService.deleteBlock(13L))
                .isInstanceOf(IllegalStateException.class);

        verify(blockRepository, never()).delete(any());
        verify(movementRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("registerBlock with locationType assigns selected location and records initial movement")
    void registerBlock_WithLocationType_AssignsSelectedLocationAndRecordsMovement() {
        Quarry quarry = Quarry.builder().id(1L).specificGravity(new BigDecimal("2.70")).build();
        StockLocation dispatchYard = StockLocation.builder()
                .id(2L).code("OCAK-SEVK").name("Stok Sahası")
                .locationType(StockLocationType.DISPATCH_YARD).businessUnit(BusinessUnit.QUARRY).build();

        when(blockRepository.existsByBlockCode("BLK-REG-01")).thenReturn(false);
        when(quarryRepository.findById(1L)).thenReturn(Optional.of(quarry));
        when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.DISPATCH_YARD))
                .thenReturn(Optional.of(dispatchYard));
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Block result = quarryBlockService.registerBlock(
                1L, "BLK-REG-01", null, 200, 200, 150,
                null, "Beyaz", "Açık", QualityGrade.A, 0,
                "Not", null, StockLocationType.DISPATCH_YARD);

        assertThat(result.getCurrentLocation()).isEqualTo(dispatchYard);
        verify(movementRepository).save(any(BlockLocationMovement.class));
    }

    @Test
    @DisplayName("updateBlock with locationType change updates location and records movement")
    void updateBlock_WithLocationTypeChange_UpdatesLocationAndRecordsMovement() {
        Quarry quarry = Quarry.builder().id(1L).specificGravity(new BigDecimal("2.70")).build();
        StockLocation prodYard = StockLocation.builder()
                .id(1L).code("OCAK-URETIM").name("Üretim Sahası")
                .locationType(StockLocationType.PRODUCTION_YARD).businessUnit(BusinessUnit.QUARRY).build();
        StockLocation dispatchYard = StockLocation.builder()
                .id(2L).code("OCAK-SEVK").name("Stok Sahası")
                .locationType(StockLocationType.DISPATCH_YARD).businessUnit(BusinessUnit.QUARRY).build();
        Block block = Block.builder()
                .id(20L)
                .blockCode("BLK-UPD-01")
                .quarry(quarry)
                .currentLocation(prodYard)
                .status(BlockStatus.PRODUCED)
                .build();

        when(blockRepository.findById(20L)).thenReturn(Optional.of(block));
        when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.DISPATCH_YARD))
                .thenReturn(Optional.of(dispatchYard));
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Block updated = quarryBlockService.updateBlock(
                20L, 1L, "BLK-UPD-01", null, 200, 200, 150,
                null, "Beyaz", "Açık", QualityGrade.A, 0,
                BigDecimal.valueOf(1000), "Not", null, StockLocationType.DISPATCH_YARD);

        assertThat(updated.getCurrentLocation()).isEqualTo(dispatchYard);
        ArgumentCaptor<BlockLocationMovement> captor = ArgumentCaptor.forClass(BlockLocationMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getFromLocation()).isEqualTo(prodYard);
        assertThat(captor.getValue().getToLocation()).isEqualTo(dispatchYard);
    }

    @Test
    @DisplayName("deleteBlock should clean up all associated files from storage and disk")
    void deleteBlock_CleansUpAttachedFiles() {
        Long blockId = 50L;
        Block block = Block.builder()
                .id(blockId)
                .blockCode("BLK-DEL-01")
                .status(BlockStatus.PRODUCED)
                .transportCost(BigDecimal.ZERO)
                .build();

        when(blockRepository.findById(blockId)).thenReturn(Optional.of(block));

        quarryBlockService.deleteBlock(blockId);

        verify(fileStorageService).deleteAllFilesForEntity("BLOCK", blockId);
        verify(blockRepository).delete(block);
    }

    @Test
    @DisplayName("registerBlock with fileIds should attach files to newly created block")
    void registerBlock_AttachesFileIds() {
        Quarry quarry = Quarry.builder().id(1L).code("Q1").name("Quarry 1").specificGravity(new BigDecimal("2.70")).build();
        StockLocation prodYard = StockLocation.builder()
                .id(1L).code("OCAK-URETIM").name("Üretim Sahası")
                .locationType(StockLocationType.PRODUCTION_YARD).build();

        when(quarryRepository.findById(1L)).thenReturn(Optional.of(quarry));
        when(blockRepository.existsByBlockCode("BLK-NEW-FILES")).thenReturn(false);
        when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.PRODUCTION_YARD))
                .thenReturn(Optional.of(prodYard));
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> {
            Block b = invocation.getArgument(0);
            b.setId(99L);
            return b;
        });

        List<Long> fileIds = List.of(101L, 102L);
        Block saved = quarryBlockService.registerBlock(
                1L, "BLK-NEW-FILES", null, 150, 250, 140,
                null, "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                "Not", null, StockLocationType.PRODUCTION_YARD, fileIds);

        assertThat(saved).isNotNull();
        verify(fileStorageService).attachFilesToEntity(fileIds, "BLOCK", 99L);
    }
}
