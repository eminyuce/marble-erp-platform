package com.ozerler.marble.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.SupplierDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.Supplier;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.MachineType;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.model.enums.SupplierType;
import com.ozerler.marble.repository.BlockCustomerMarkRepository;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CollectionRecordRepository;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.InvoiceRepository;
import com.ozerler.marble.repository.MachineFuelEntryRepository;
import com.ozerler.marble.repository.MachineRepository;
import com.ozerler.marble.repository.PurchaseOrderRepository;
import com.ozerler.marble.repository.QuarryRepository;
import com.ozerler.marble.repository.SalesOrderRepository;
import com.ozerler.marble.repository.StockLocationRepository;
import com.ozerler.marble.repository.SupplierRepository;

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
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private CollectionRecordRepository collectionRecordRepository;

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
                blockCustomerMarkRepository, invoiceRepository, collectionRecordRepository,
                Mockito.mock(com.ozerler.marble.mapper.CustomerMapper.class),
                Mockito.mock(com.ozerler.marble.mapper.SupplierMapper.class),
                Mockito.mock(com.ozerler.marble.mapper.MachineMapper.class),
                Mockito.mock(com.ozerler.marble.mapper.QuarryMapper.class),
                Mockito.mock(com.ozerler.marble.mapper.StockLocationMapper.class),
                Mockito.mock(com.ozerler.marble.mapper.CostCenterMapper.class));
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(previousLocale);
    }

    @Test
    @DisplayName("machine codes are uppercased with Locale.ROOT so Turkish i stays I")
    void saveMachine_NormalizesCodeWithRootLocale() {
        Mockito.when(machineRepository.findByCode("MAK-I1")).thenReturn(Optional.empty());
        Mockito.when(machineRepository.save(ArgumentMatchers.any(Machine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Machine saved = masterDataService.saveMachine(Machine.builder()
                .code("mak-i1")
                .name("Katrak")
                .businessUnit(BusinessUnit.FACTORY)
                .machineType(MachineType.GANGSAW)
                .build());

        Assertions.assertThat(saved.getCode()).isEqualTo("MAK-I1");
    }

    @Test
    @DisplayName("duplicate machine codes are rejected")
    void saveMachine_DuplicateCode_Throws() {
        Machine existing = Machine.builder().id(9L).code("MK-01").name("Eski").build();
        Mockito.when(machineRepository.findByCode("MK-01")).thenReturn(Optional.of(existing));

        Assertions.assertThatThrownBy(() -> masterDataService.saveMachine(Machine.builder()
                .code("mk-01")
                .name("Yeni")
                .businessUnit(BusinessUnit.FACTORY)
                .machineType(MachineType.GANGSAW)
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MK-01");
        Mockito.verify(machineRepository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("machines with fuel entries cannot be deleted")
    void deleteMachine_WithFuel_Throws() {
        Mockito.when(machineRepository.findById(3L)).thenReturn(Optional.of(
                Machine.builder().id(3L).code("OCK-1").name("Loder").build()));
        Mockito.when(machineFuelEntryRepository.existsByMachineId(3L)).thenReturn(true);

        Assertions.assertThatThrownBy(() -> masterDataService.deleteMachine(3L))
                .isInstanceOf(IllegalStateException.class);
        Mockito.verify(machineRepository, Mockito.never()).delete(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("seeded core stock locations cannot be deleted")
    void deleteStockLocation_CoreCode_Throws() {
        Mockito.when(stockLocationRepository.findById(1L)).thenReturn(Optional.of(StockLocation.builder()
                .id(1L)
                .code(Constants.STOCK_LOCATION_PRODUCTION_YARD)
                .name("Üretim Sahası")
                .businessUnit(BusinessUnit.QUARRY)
                .locationType(StockLocationType.PRODUCTION_YARD)
                .build()));

        Assertions.assertThatThrownBy(() -> masterDataService.deleteStockLocation(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(Constants.STOCK_LOCATION_PRODUCTION_YARD);
        Mockito.verify(stockLocationRepository, Mockito.never()).delete(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("customers with sales orders cannot be deleted")
    void deleteCustomer_WithOrders_Throws() {
        Mockito.when(customerRepository.findById(4L)).thenReturn(Optional.of(
                Customer.builder().id(4L).customerCode("C-1").companyName("Firma").build()));
        Mockito.when(salesOrderRepository.existsByCustomerId(4L)).thenReturn(true);

        Assertions.assertThatThrownBy(() -> masterDataService.deleteCustomer(4L))
                .isInstanceOf(IllegalStateException.class);
        Mockito.verify(customerRepository, Mockito.never()).delete(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("customers with sold quarry blocks cannot be deleted")
    void deleteCustomer_WithSoldBlock_Throws() {
        Mockito.when(customerRepository.findById(8L)).thenReturn(Optional.of(
                Customer.builder().id(8L).customerCode("C-8").companyName("Alıcı").build()));
        Mockito.when(salesOrderRepository.existsByCustomerId(8L)).thenReturn(false);
        Mockito.when(blockCustomerMarkRepository.existsByCustomerId(8L)).thenReturn(false);
        Mockito.when(blockRepository.existsBySoldCustomer_Id(8L)).thenReturn(true);

        Assertions.assertThatThrownBy(() -> masterDataService.deleteCustomer(8L))
                .isInstanceOf(IllegalStateException.class);
        Mockito.verify(customerRepository, Mockito.never()).delete(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("suppliers without purchase orders can be deleted")
    void deleteSupplier_WithoutOrders_Deletes() {
        Supplier supplier = Supplier.builder().id(5L).supplierCode("T-1").companyName("Tedarik").build();
        Mockito.when(supplierRepository.findById(5L)).thenReturn(Optional.of(supplier));
        Mockito.when(purchaseOrderRepository.existsBySupplierId(5L)).thenReturn(false);

        masterDataService.deleteSupplier(5L);

        Mockito.verify(supplierRepository).delete(supplier);
    }

    @Test
    @DisplayName("supplier grid paging maps entities to Tabulator DTOs")
    void getSuppliersPaged_MapsDtos() {
        Supplier supplier = Supplier.builder()
                .id(11L)
                .supplierCode("SUPP-1")
                .companyName("Kaya Elmas")
                .supplierType(SupplierType.CONSUMABLE)
                .build();
        Mockito.when(supplierRepository.search(ArgumentMatchers.isNull(), ArgumentMatchers.isNull(), ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(supplier), PageRequest.of(0, 25), 1));

        TabulatorResponse<SupplierDto> response =
                masterDataService.getSuppliersPaged(1, 25, null, null, "companyName", "asc");

        Assertions.assertThat(response.getData()).hasSize(1);
        Assertions.assertThat(response.getData().get(0).getSupplierCode()).isEqualTo("SUPP-1");
        Assertions.assertThat(response.getData().get(0).getCompanyName()).isEqualTo("Kaya Elmas");
        Assertions.assertThat(response.getTotal()).isEqualTo(1);
        Assertions.assertThat(response.getLast_page()).isEqualTo(1);
    }

    @Test
    @DisplayName("cost centers with transactions cannot be deleted")
    void deleteCostCenter_WithTransactions_Throws() {
        Mockito.when(costCenterRepository.findById(7L)).thenReturn(Optional.of(
                CostCenter.builder().id(7L).code("CC-001").name("Ocak").build()));
        Mockito.when(costTransactionRepository.countByCostCenterId(7L)).thenReturn(2L);

        Assertions.assertThatThrownBy(() -> masterDataService.deleteCostCenter(7L))
                .isInstanceOf(IllegalStateException.class);
        Mockito.verify(costCenterRepository, Mockito.never()).delete(ArgumentMatchers.any());
    }
}
