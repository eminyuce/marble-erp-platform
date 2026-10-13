package com.ozerler.marble.controller.erp;

import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.StockItemRepository;
import com.ozerler.marble.repository.SupplierRepository;
import com.ozerler.marble.service.InvoiceService;
import com.ozerler.marble.service.QuarryInventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerAvailableStockTest {

    @Mock
    private InvoiceService invoiceService;

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
    private QuarryInventoryService quarryInventoryService;

    @InjectMocks
    private InvoiceController controller;

    @Test
    @DisplayName("sized stock is filtered by the already loaded stock location business unit")
    void getAvailableStock_FiltersSizedItemsByLoadedLocation() {
        StockItem factoryItem = sizedItem("EB-FAB", BusinessUnit.FACTORY);
        StockItem workshopItem = sizedItem("EB-ATL", BusinessUnit.WORKSHOP);
        StockItem unlocatedItem = StockItem.builder()
                .itemCode("EB-YOK")
                .quantity(BigDecimal.ONE)
                .stoneType("Traverten")
                .build();

        when(stockItemRepository.findPositiveQuantityWithLocation())
                .thenReturn(List.of(factoryItem, workshopItem, unlocatedItem));

        List<Map<String, Object>> result = controller.getAvailableStock(
                BusinessUnit.FACTORY, "SIZED", null, null, false);

        assertThat(result).extracting(row -> row.get("code"))
                .containsExactly("EB-FAB", "EB-YOK");
    }

    private static StockItem sizedItem(String itemCode, BusinessUnit businessUnit) {
        StockLocation location = StockLocation.builder()
                .code(businessUnit.name())
                .name(businessUnit.name())
                .businessUnit(businessUnit)
                .build();
        return StockItem.builder()
                .itemCode(itemCode)
                .quantity(BigDecimal.TEN)
                .stoneType("Bej")
                .stockLocation(location)
                .build();
    }
}
