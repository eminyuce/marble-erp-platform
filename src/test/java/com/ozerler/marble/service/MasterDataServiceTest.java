package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.Supplier;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.MachineType;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.repository.BlockCustomerMarkRepository;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.MachineFuelEntryRepository;
import com.ozerler.marble.repository.MachineRepository;
import com.ozerler.marble.repository.PurchaseOrderRepository;
import com.ozerler.marble.repository.QuarryRepository;
import com.ozerler.marble.repository.SalesOrderRepository;
import com.ozerler.marble.repository.StockLocationRepository;
import com.ozerler.marble.repository.SupplierRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterDataServiceTest {

    @Mock
    private MachineRepository machineRepository;
    @Mock
    private StockLocationRepository stockLocationRepository;
    @Mock
    private QuarryRepository quarryRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private MachineFuelEntryRepository machineFuelEntryRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private CostTransactionRepository costTransactionRepository;
    @Mock
    private BlockCustomerMarkRepository blockCustomerMarkRepository;

    private MasterDataService masterDataService;
    private Locale previousLocale;

    @BeforeEach
    void setUp() {
        previousLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr"));
        masterDataService = new MasterDataService(
                machineRepository, stockLocationRepository, quarryRepository, customerRepository,
                supplierRepository, costCenterRepository, machineFuelEntryRepository, blockRepository,
                salesOrderRepository, purchaseOrderRepository, costTransactionRepository,
                blockCustomerMarkRepository);
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(previousLocale);
    }

    @Test
    @DisplayName("machine codes are uppercased with Locale.ROOT so Turkish i stays I")
    void saveMachine_NormalizesCodeWithRootLocale() {
        when(machineRepository.findByCode("MAK-I1")).thenReturn(Optional.empty());
        when(machineRepository.save(any(Machine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Machine saved = masterDataService.saveMachine(Machine.builder()
                .code("mak-i1")
                .name("Katrak")
                .businessUnit(BusinessUnit.FACTORY)
                .machineType(MachineType.GANGSAW)
                .build());

        assertThat(saved.getCode()).isEqualTo("MAK-I1");
    }

    @Test
    @DisplayName("duplicate machine codes are rejected")
    void saveMachine_DuplicateCode_Throws() {
        Machine existing = Machine.builder().id(9L).code("MK-01").name("Eski").build();
        when(machineRepository.findByCode("MK-01")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> masterDataService.saveMachine(Machine.builder()
                .code("mk-01")
                .name("Yeni")
                .businessUnit(BusinessUnit.FACTORY)
                .machineType(MachineType.GANGSAW)
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MK-01");
        verify(machineRepository, never()).save(any());
    }

    @Test
    @DisplayName("machines with fuel entries cannot be deleted")
    void deleteMachine_WithFuel_Throws() {
        when(machineRepository.findById(3L)).thenReturn(Optional.of(
                Machine.builder().id(3L).code("OCK-1").name("Loder").build()));
        when(machineFuelEntryRepository.existsByMachineId(3L)).thenReturn(true);

        assertThatThrownBy(() -> masterDataService.deleteMachine(3L))
                .isInstanceOf(IllegalStateException.class);
        verify(machineRepository, never()).delete(any());
    }

    @Test
    @DisplayName("seeded core stock locations cannot be deleted")
    void deleteStockLocation_CoreCode_Throws() {
        when(stockLocationRepository.findById(1L)).thenReturn(Optional.of(StockLocation.builder()
                .id(1L)
                .code(Constants.STOCK_LOCATION_PRODUCTION_YARD)
                .name("Üretim Sahası")
                .businessUnit(BusinessUnit.QUARRY)
                .locationType(StockLocationType.PRODUCTION_YARD)
                .build()));

        assertThatThrownBy(() -> masterDataService.deleteStockLocation(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(Constants.STOCK_LOCATION_PRODUCTION_YARD);
        verify(stockLocationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("customers with sales orders cannot be deleted")
    void deleteCustomer_WithOrders_Throws() {
        when(customerRepository.findById(4L)).thenReturn(Optional.of(
                Customer.builder().id(4L).customerCode("C-1").companyName("Firma").build()));
        when(salesOrderRepository.existsByCustomerId(4L)).thenReturn(true);

        assertThatThrownBy(() -> masterDataService.deleteCustomer(4L))
                .isInstanceOf(IllegalStateException.class);
        verify(customerRepository, never()).delete(any());
    }

    @Test
    @DisplayName("suppliers without purchase orders can be deleted")
    void deleteSupplier_WithoutOrders_Deletes() {
        Supplier supplier = Supplier.builder().id(5L).supplierCode("T-1").companyName("Tedarik").build();
        when(supplierRepository.findById(5L)).thenReturn(Optional.of(supplier));
        when(purchaseOrderRepository.existsBySupplierId(5L)).thenReturn(false);

        masterDataService.deleteSupplier(5L);

        verify(supplierRepository).delete(supplier);
    }

    @Test
    @DisplayName("cost centers with transactions cannot be deleted")
    void deleteCostCenter_WithTransactions_Throws() {
        when(costCenterRepository.findById(7L)).thenReturn(Optional.of(
                CostCenter.builder().id(7L).code("CC-001").name("Ocak").build()));
        when(costTransactionRepository.countByCostCenterId(7L)).thenReturn(2L);

        assertThatThrownBy(() -> masterDataService.deleteCostCenter(7L))
                .isInstanceOf(IllegalStateException.class);
        verify(costCenterRepository, never()).delete(any());
    }
}
