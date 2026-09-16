package com.ozerler.marble.service;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.repository.BlockLocationMovementRepository;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.QuarryRepository;
import com.ozerler.marble.repository.StockLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    private QuarryBlockService quarryBlockService;

    @BeforeEach
    void setUp() {
        quarryBlockService = new QuarryBlockService(
                blockRepository, quarryRepository, null, stockLocationRepository, movementRepository,
                null, expenseService, costCenterRepository, customerRepository);
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
                null, 0, BigDecimal.ONE, null, null))
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

        Block sold = quarryBlockService.sellBlockExternally(4L, 2L);

        assertThat(sold.getStatus()).isEqualTo(BlockStatus.SOLD);
        assertThat(sold.getSoldCustomer().getCompanyName()).isEqualTo("Mermer A.Ş.");
        verify(movementRepository).save(any());
    }
}
