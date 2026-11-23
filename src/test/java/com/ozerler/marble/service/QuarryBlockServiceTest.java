package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

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
    @Mock
    private QuarryInventoryService quarryInventoryService;

    private QuarryBlockService quarryBlockService;

    @BeforeEach
    void setUp() {
        quarryBlockService = new QuarryBlockService(
                blockRepository, null, null, quarryRepository, null, stockLocationRepository, movementRepository,
                costAnalysisService, blockCostCalculationService, expenseService, costCenterRepository, customerRepository,
                slabRepository, factoryWorkOrderRepository, productionOrderRepository,
                blockCustomerMarkRepository, costTransactionRepository, shipmentItemRepository,
                fileStorageService, quarryInventoryService, null);
    }

    @Test
    @DisplayName("getBlockWithDetails returns the block loaded with its quarry")
    void getBlockWithDetails_ReturnsBlock() {
        Block block = Block.builder()
                .id(5L)
                .blockCode("BLK-001")
                .quarry(Quarry.builder().id(1L).name("Ana Ocak").build())
                .build();
        Mockito.when(blockRepository.findByIdWithQuarry(5L)).thenReturn(Optional.of(block));

        Block result = quarryBlockService.getBlockWithDetails(5L);

        Assertions.assertThat(result.getBlockCode()).isEqualTo("BLK-001");
        Assertions.assertThat(result.getQuarry().getName()).isEqualTo("Ana Ocak");
        Mockito.verify(blockRepository).findByIdWithQuarry(5L);
    }

    @Test
    @DisplayName("getBlockWithDetails throws when the block is missing")
    void getBlockWithDetails_MissingBlock_Throws() {
        Mockito.when(blockRepository.findByIdWithQuarry(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> quarryBlockService.getBlockWithDetails(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getBlockWithDetails rejects a null id")
    void getBlockWithDetails_NullId_Throws() {
        Assertions.assertThatThrownBy(() -> quarryBlockService.getBlockWithDetails(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("duplicate block codes are rejected on create")
    void registerBlock_DuplicateCode_Throws() {
        Mockito.when(quarryRepository.findById(1L)).thenReturn(Optional.of(Quarry.builder().id(1L).name("Ocak").build()));
        Mockito.when(blockRepository.existsByBlockCodeIgnoreCase("BLK-1")).thenReturn(true);

        Assertions.assertThatThrownBy(() -> quarryBlockService.registerBlock(
                1L, "BLK-1", null, 100, 100, 100, BigDecimal.TEN, "Beyaz", null,
                null, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BLK-1");
        Mockito.verify(blockRepository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("factory transfer leaves quarry stock, enters factory yard and posts nakliye")
    void transferToFactory_MovesToFactoryYardAndPostsExpense() {
        Quarry quarry = Quarry.builder().id(1L).name("Ocak").specificGravity(new BigDecimal("2.70")).build();
        StockLocation dispatch = StockLocation.builder()
                .id(2L).code("OCAK-SEVK").name("Stok Sahası")
                .locationType(StockLocationType.DISPATCH_YARD).businessUnit(BusinessUnit.QUARRY).build();
        StockLocation factoryYard = StockLocation.builder()
                .id(3L).code("FAB-BLOK").name("Fabrika Blok Sahası")
                .locationType(StockLocationType.FACTORY_BLOCK_YARD).businessUnit(BusinessUnit.FACTORY).build();
        Block block = Block.builder()
                .id(8L)
                .blockCode("BLK-008")
                .quarry(quarry)
                .status(BlockStatus.PRODUCED)
                .currentLocation(dispatch)
                .widthCm(100)
                .lengthCm(100)
                .heightCm(100)
                .actualWeightKg(new BigDecimal("2700"))
                .extractionCost(BigDecimal.TEN)
                .transportCost(BigDecimal.ZERO)
                .build();
        CostCenter factoryTransport = CostCenter.builder()
                .id(6L).code("CC-006").name("Nakliye").businessUnit(BusinessUnit.FACTORY).build();

        Mockito.when(blockRepository.findById(8L)).thenReturn(Optional.of(block));
        Mockito.when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.FACTORY_BLOCK_YARD))
                .thenReturn(Optional.of(factoryYard));
        Mockito.when(blockRepository.save(ArgumentMatchers.any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(costCenterRepository.findByCode("CC-006")).thenReturn(Optional.of(factoryTransport));

        Block saved = quarryBlockService.transferToFactory(8L, new BigDecimal("1500"));

        Assertions.assertThat(saved.getStatus()).isEqualTo(BlockStatus.IN_TRANSIT);
        Assertions.assertThat(saved.getCurrentLocation()).isNull();
        Assertions.assertThat(saved.getTransportCost()).isEqualByComparingTo("1500");
        ArgumentCaptor<ExpenseService.ExpenseDraft> expense = ArgumentCaptor.forClass(ExpenseService.ExpenseDraft.class);
        Mockito.verify(expenseService).recordExpense(expense.capture());
        Assertions.assertThat(expense.getValue().businessUnit()).isEqualTo(BusinessUnit.FACTORY);
        Assertions.assertThat(expense.getValue().amount()).isEqualByComparingTo("1500");
        Assertions.assertThat(expense.getValue().centerId()).isEqualTo(6L);
        Mockito.verify(movementRepository).save(ArgumentMatchers.any());

        // Fabrika kullanıcısı Stoğa Al dediğinde
        Block received = quarryBlockService.receiveBlockAtFactory(8L);
        Assertions.assertThat(received.getStatus()).isEqualTo(BlockStatus.AT_FACTORY);
        Assertions.assertThat(received.getCurrentLocation().getLocationType()).isEqualTo(StockLocationType.FACTORY_BLOCK_YARD);
    }

    @Test
    @DisplayName("external sale stores the customer on the sold block")
    void sellBlockExternally_SetsCustomer() {
        StockLocation dispatch = StockLocation.builder()
                .id(2L).code("OCAK-SEVK").name("Stok Sahası")
                .locationType(StockLocationType.DISPATCH_YARD).businessUnit(BusinessUnit.QUARRY).build();
        Block block = Block.builder()
                .id(4L)
                .blockCode("BLK-004")
                .status(BlockStatus.PRODUCED)
                .currentLocation(dispatch)
                .build();
        Customer customer = Customer.builder().id(2L).companyName("Mermer A.Ş.").build();
        Mockito.when(blockRepository.findById(4L)).thenReturn(Optional.of(block));
        Mockito.when(customerRepository.findById(2L)).thenReturn(Optional.of(customer));
        Mockito.when(blockRepository.save(ArgumentMatchers.any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Block sold = quarryBlockService.sellBlockExternally(4L, 2L, new BigDecimal("750000"), java.time.LocalDate.now(), "Test satış");

        Assertions.assertThat(sold.getStatus()).isEqualTo(BlockStatus.SOLD);
        Assertions.assertThat(sold.getSoldCustomer().getCompanyName()).isEqualTo("Mermer A.Ş.");
        Assertions.assertThat(sold.getSalePrice()).isEqualByComparingTo("750000");
        Mockito.verify(movementRepository).save(ArgumentMatchers.any());
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

        Mockito.when(quarryRepository.findById(1L)).thenReturn(Optional.of(quarry));
        Mockito.when(blockRepository.existsByBlockCodeIgnoreCase("BLK-AUTO-01")).thenReturn(false);
        Mockito.when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.PRODUCTION_YARD))
                .thenReturn(Optional.of(production));
        Mockito.when(blockRepository.save(ArgumentMatchers.any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // En: 200 cm, Boy: 250 cm, Yükseklik: 150 cm -> Hacim: 7.500 m³ -> 7.5 * 2.70 = 20.25 ton = 20250.00 kg
        Block saved = quarryBlockService.registerBlock(
                1L, "BLK-AUTO-01", null, 200, 250, 150, null, "Beyaz Mermer", "Açık",
                QualityGrade.A, 0, "Notlar", null);

        Assertions.assertThat(saved).isNotNull();
        Assertions.assertThat(saved.getVolumeM3()).isEqualByComparingTo(new BigDecimal("7.500"));
        Assertions.assertThat(saved.getTheoreticalWeightKg()).isEqualByComparingTo(new BigDecimal("20250.00"));
        Assertions.assertThat(saved.getActualWeightKg()).isEqualByComparingTo(BigDecimal.ZERO);
        Assertions.assertThat(saved.getApproximateTonnage()).isEqualByComparingTo(new BigDecimal("20.25"));
        Mockito.verify(blockRepository).save(ArgumentMatchers.any(Block.class));
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

        Mockito.when(blockRepository.findById(10L)).thenReturn(Optional.of(block));
        Mockito.when(slabRepository.existsByBlockId(10L)).thenReturn(false);
        Mockito.when(factoryWorkOrderRepository.findFirstByBlockIdOrderByIdDesc(10L)).thenReturn(Optional.empty());
        Mockito.when(productionOrderRepository.findByBlockId(10L)).thenReturn(List.of());
        Mockito.when(blockCustomerMarkRepository.existsByBlockId(10L)).thenReturn(false);
        Mockito.when(costTransactionRepository.findByBlockId(10L)).thenReturn(List.of());
        Mockito.when(shipmentItemRepository.existsByBlockId(10L)).thenReturn(false);

        boolean canDelete = quarryBlockService.canDeleteBlock(10L);

        Assertions.assertThat(canDelete).isTrue();
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

        Mockito.when(blockRepository.findById(11L)).thenReturn(Optional.of(block));
        Mockito.when(slabRepository.existsByBlockId(11L)).thenReturn(true);

        boolean canDelete = quarryBlockService.canDeleteBlock(11L);

        Assertions.assertThat(canDelete).isFalse();
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

        Mockito.when(blockRepository.findById(12L)).thenReturn(Optional.of(block));
        Mockito.when(slabRepository.existsByBlockId(12L)).thenReturn(false);
        Mockito.when(factoryWorkOrderRepository.findFirstByBlockIdOrderByIdDesc(12L)).thenReturn(Optional.empty());
        Mockito.when(productionOrderRepository.findByBlockId(12L)).thenReturn(List.of());
        Mockito.when(blockCustomerMarkRepository.existsByBlockId(12L)).thenReturn(false);
        Mockito.when(costTransactionRepository.findByBlockId(12L)).thenReturn(List.of());
        Mockito.when(shipmentItemRepository.existsByBlockId(12L)).thenReturn(false);
        Mockito.when(movementRepository.findByBlockIdOrderByCreatedDateDesc(12L)).thenReturn(List.of(mv));

        quarryBlockService.deleteBlock(12L);

        Mockito.verify(movementRepository).deleteAll(List.of(mv));
        Mockito.verify(blockRepository).delete(block);
    }

    @Test
    @DisplayName("deleteBlock throws IllegalStateException when block is not deletable")
    void deleteBlock_WhenUsed_ThrowsIllegalStateException() {
        Block block = Block.builder()
                .id(13L)
                .blockCode("BLK-DELETE-04")
                .status(BlockStatus.AT_FACTORY)
                .build();

        Mockito.when(blockRepository.findById(13L)).thenReturn(Optional.of(block));

        Assertions.assertThatThrownBy(() -> quarryBlockService.deleteBlock(13L))
                .isInstanceOf(IllegalStateException.class);

        Mockito.verify(blockRepository, Mockito.never()).delete(ArgumentMatchers.any());
        Mockito.verify(movementRepository, Mockito.never()).deleteAll(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("registerBlock with locationType assigns selected location and records initial movement")
    void registerBlock_WithLocationType_AssignsSelectedLocationAndRecordsMovement() {
        Quarry quarry = Quarry.builder().id(1L).specificGravity(new BigDecimal("2.70")).build();
        StockLocation dispatchYard = StockLocation.builder()
                .id(2L).code("OCAK-SEVK").name("Stok Sahası")
                .locationType(StockLocationType.DISPATCH_YARD).businessUnit(BusinessUnit.QUARRY).build();

        Mockito.when(blockRepository.existsByBlockCodeIgnoreCase("BLK-REG-01")).thenReturn(false);
        Mockito.when(quarryRepository.findById(1L)).thenReturn(Optional.of(quarry));
        Mockito.when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.DISPATCH_YARD))
                .thenReturn(Optional.of(dispatchYard));
        Mockito.when(blockRepository.save(ArgumentMatchers.any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Block result = quarryBlockService.registerBlock(
                1L, "BLK-REG-01", null, 200, 200, 150,
                null, "Beyaz", "Açık", QualityGrade.A, 0,
                "Not", null, StockLocationType.DISPATCH_YARD);

        Assertions.assertThat(result.getCurrentLocation()).isEqualTo(dispatchYard);
        Mockito.verify(movementRepository).save(ArgumentMatchers.any(BlockLocationMovement.class));
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

        Mockito.when(blockRepository.findById(20L)).thenReturn(Optional.of(block));
        Mockito.when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.DISPATCH_YARD))
                .thenReturn(Optional.of(dispatchYard));
        Mockito.when(blockRepository.save(ArgumentMatchers.any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Block updated = quarryBlockService.updateBlock(
                20L, 1L, "BLK-UPD-01", null, 200, 200, 150,
                null, "Beyaz", "Açık", QualityGrade.A, 0,
                BigDecimal.valueOf(1000), "Not", null, StockLocationType.DISPATCH_YARD);

        Assertions.assertThat(updated.getCurrentLocation()).isEqualTo(dispatchYard);
        ArgumentCaptor<BlockLocationMovement> captor = ArgumentCaptor.forClass(BlockLocationMovement.class);
        Mockito.verify(movementRepository).save(captor.capture());
        Assertions.assertThat(captor.getValue().getFromLocation()).isEqualTo(prodYard);
        Assertions.assertThat(captor.getValue().getToLocation()).isEqualTo(dispatchYard);
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

        Mockito.when(blockRepository.findById(blockId)).thenReturn(Optional.of(block));

        quarryBlockService.deleteBlock(blockId);

        Mockito.verify(fileStorageService).deleteAllFilesForEntity("BLOCK", blockId);
        Mockito.verify(blockRepository).delete(block);
    }

    @Test
    @DisplayName("registerBlock with fileIds should attach files to newly created block")
    void registerBlock_AttachesFileIds() {
        Quarry quarry = Quarry.builder().id(1L).code("Q1").name("Quarry 1").specificGravity(new BigDecimal("2.70")).build();
        StockLocation prodYard = StockLocation.builder()
                .id(1L).code("OCAK-URETIM").name("Üretim Sahası")
                .locationType(StockLocationType.PRODUCTION_YARD).build();

        Mockito.when(quarryRepository.findById(1L)).thenReturn(Optional.of(quarry));
        Mockito.when(blockRepository.existsByBlockCodeIgnoreCase("BLK-NEW-FILES")).thenReturn(false);
        Mockito.when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.PRODUCTION_YARD))
                .thenReturn(Optional.of(prodYard));
        Mockito.when(blockRepository.save(ArgumentMatchers.any(Block.class))).thenAnswer(invocation -> {
            Block b = invocation.getArgument(0);
            b.setId(99L);
            return b;
        });

        List<Long> fileIds = List.of(101L, 102L);
        Block saved = quarryBlockService.registerBlock(
                1L, "BLK-NEW-FILES", null, 150, 250, 140,
                null, "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                "Not", null, StockLocationType.PRODUCTION_YARD, fileIds);

        Assertions.assertThat(saved).isNotNull();
        Mockito.verify(fileStorageService).attachFilesToEntity(fileIds, "BLOCK", 99L);
    }

    @Test
    @DisplayName("registerBlock stores lowercase block codes as uppercase")
    void registerBlock_LowercaseCode_PersistsUppercase() {
        Quarry quarry = Quarry.builder().id(1L).specificGravity(new BigDecimal("2.70")).build();
        StockLocation production = StockLocation.builder()
                .id(1L).code("OCAK-URETIM").name("Üretim Sahası")
                .locationType(StockLocationType.PRODUCTION_YARD).businessUnit(BusinessUnit.QUARRY).build();

        Mockito.when(quarryRepository.findById(1L)).thenReturn(Optional.of(quarry));
        Mockito.when(blockRepository.existsByBlockCodeIgnoreCase("BLK-LOWER-01")).thenReturn(false);
        Mockito.when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.PRODUCTION_YARD))
                .thenReturn(Optional.of(production));
        Mockito.when(blockRepository.save(ArgumentMatchers.any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Block saved = quarryBlockService.registerBlock(
                1L, "blk-lower-01", null, 100, 100, 100, BigDecimal.TEN, "Beyaz", null,
                QualityGrade.A, 0, null, null);

        Assertions.assertThat(saved.getBlockCode()).isEqualTo("BLK-LOWER-01");
    }

    @Test
    @DisplayName("normalizeBlockCode uppercases with Locale.ROOT so Turkish i becomes I")
    void normalizeBlockCode_UsesRootLocale() {
        Assertions.assertThat(QuarryBlockService.normalizeBlockCode(" blk-i1 ")).isEqualTo("BLK-I1");
    }

    @Test
    @DisplayName("getBlocksPaged puts filtered tonnage, m2, extraction and total cost into grid meta")
    void getBlocksPaged_IncludesFooterTotalsInMeta() {
        Mockito.when(blockRepository.searchBlocksWithStockFilter(ArgumentMatchers.isNull(), ArgumentMatchers.isNull(), ArgumentMatchers.isNull(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of()));
        Mockito.when(blockCostCalculationService.expensePerTonByQuarry(ArgumentMatchers.any()))
                .thenReturn(Map.of(1L, new BigDecimal("100")));
        List<Object[]> metricRows = List.<Object[]>of(new Object[]{
                1L, new BigDecimal("10"), new BigDecimal("4.50"), new BigDecimal("250")
        });
        Mockito.when(blockRepository.sumGridMetricsWithStockFilterByQuarry(ArgumentMatchers.isNull(), ArgumentMatchers.isNull(), ArgumentMatchers.isNull(), ArgumentMatchers.any()))
                .thenReturn(metricRows);

        var response = quarryBlockService.getBlocksPaged(1, 25, null, null, null, null, null);

        Assertions.assertThat((BigDecimal) response.getMeta().get("totalTonnage")).isEqualByComparingTo("10");
        Assertions.assertThat((BigDecimal) response.getMeta().get("totalSurfaceM2")).isEqualByComparingTo("4.50");
        Assertions.assertThat((BigDecimal) response.getMeta().get("totalExtractionCost")).isEqualByComparingTo("1000.00");
        Assertions.assertThat((BigDecimal) response.getMeta().get("totalCost")).isEqualByComparingTo("1250.00");
    }

    @Test
    @DisplayName("sell from production yard is rejected")
    void sellBlockExternally_NotInDispatchYard_Throws() {
        StockLocation production = StockLocation.builder()
                .locationType(StockLocationType.PRODUCTION_YARD).businessUnit(BusinessUnit.QUARRY).build();
        Block block = Block.builder().id(20L).status(BlockStatus.PRODUCED).currentLocation(production).build();
        Mockito.when(blockRepository.findById(20L)).thenReturn(Optional.of(block));

        Assertions.assertThatThrownBy(() -> quarryBlockService.sellBlockExternally(
                20L, 2L, new BigDecimal("10"), java.time.LocalDate.now(), null))
                .isInstanceOf(IllegalArgumentException.class);
        Mockito.verify(blockRepository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("factory transfer from production yard is rejected")
    void transferToFactory_NotInDispatchYard_Throws() {
        StockLocation production = StockLocation.builder()
                .locationType(StockLocationType.PRODUCTION_YARD).businessUnit(BusinessUnit.QUARRY).build();
        Block block = Block.builder().id(21L).status(BlockStatus.PRODUCED).currentLocation(production).build();
        Mockito.when(blockRepository.findById(21L)).thenReturn(Optional.of(block));

        Assertions.assertThatThrownBy(() -> quarryBlockService.transferToFactory(21L, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
        Mockito.verify(blockRepository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("block codes increment per section and year")
    void generateStandardBlockCode_UsesSectionYearSequence() {
        Mockito.when(blockRepository.countByQuarrySectionAndYear(ArgumentMatchers.eq(1L), ArgumentMatchers.eq("A-BLOK"), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(0L);
        Mockito.when(blockRepository.existsByBlockCode("A-BLOK-2026-001")).thenReturn(false);
        Mockito.when(blockRepository.existsByBlockCodeIgnoreCase("A-BLOK-2026-001")).thenReturn(false);

        String first = quarryBlockService.generateStandardBlockCode(1L, "a-blok");
        Assertions.assertThat(first).isEqualTo("A-BLOK-" + java.time.LocalDate.now().getYear() + "-001");

        Mockito.when(blockRepository.countByQuarrySectionAndYear(ArgumentMatchers.eq(1L), ArgumentMatchers.eq("A-BLOK"), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(1L);
        Mockito.when(blockRepository.existsByBlockCode(ArgumentMatchers.any())).thenReturn(false);
        Mockito.when(blockRepository.existsByBlockCodeIgnoreCase(ArgumentMatchers.any())).thenReturn(false);
        String second = quarryBlockService.generateStandardBlockCode(1L, "A-BLOK");
        Assertions.assertThat(second).isEqualTo("A-BLOK-" + java.time.LocalDate.now().getYear() + "-002");
    }
}
