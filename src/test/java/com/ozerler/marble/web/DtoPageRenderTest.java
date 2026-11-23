package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.ServletException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.BlockCustomerMark;
import com.ozerler.marble.model.BlockLocationMovement;
import com.ozerler.marble.model.CariTransaction;
import com.ozerler.marble.model.CheckRecord;
import com.ozerler.marble.model.CollectionRecord;
import com.ozerler.marble.model.ConstructionSiteStonePlan;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.CutItem;
import com.ozerler.marble.model.CutOrder;
import com.ozerler.marble.model.FactoryOperation;
import com.ozerler.marble.model.FactoryWorkOrder;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.InvoiceItem;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.MachineFuelEntry;
import com.ozerler.marble.model.OperationWorkOrder;
import com.ozerler.marble.model.OperationWorkOrderItem;
import com.ozerler.marble.model.Pallet;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.ProjectLocation;
import com.ozerler.marble.model.PurchaseOrder;
import com.ozerler.marble.model.PurchaseOrderItem;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.SalesOrder;
import com.ozerler.marble.model.SalesOrderItem;
import com.ozerler.marble.model.SiteConsumption;
import com.ozerler.marble.model.SiteInstallation;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.StockMovement;
import com.ozerler.marble.model.Supplier;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ConsumptionType;
import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.model.enums.MachineType;
import com.ozerler.marble.model.enums.ProcessType;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.model.enums.StockMovementType;
import com.ozerler.marble.model.enums.StockProductType;

/**
 * Renders every screen that now receives view DTOs instead of JPA entities, with a populated
 * graph so each template walks its nested property paths. The persistence context is cleared
 * after seeding, so the services load fresh rows exactly as in a real request.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "ADMIN")
class DtoPageRenderTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 2);

    @Autowired
    private WebApplicationContext context;

    @PersistenceContext
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private Long blockId;
    private Long slabId;
    private Long invoiceId;
    private Long salesOrderId;
    private Long productionOrderId;
    private Long cutOrderId;
    private Long projectId;
    private Long customerId;
    private Long workOrderId;
    private Long purchaseOrderId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        seed();
        entityManager.flush();
        entityManager.clear();
    }

    private void seed() {
        Customer customer = persist(Customer.builder().customerCode("C-RND").companyName("Render Müşteri").build());
        customerId = customer.getId();
        Supplier supplier = persist(Supplier.builder().supplierCode("S-RND").companyName("Render Tedarik").build());
        Quarry quarry = persist(Quarry.builder().code("Q-RND").name("Render Ocağı").location("Burdur").build());
        StockLocation yard = persist(StockLocation.builder().code("YRD-RND").name("Render Saha")
                .businessUnit(BusinessUnit.QUARRY).locationType(StockLocationType.PRODUCTION_YARD).build());
        StockLocation dispatch = persist(StockLocation.builder().code("DSP-RND").name("Sevk Saha")
                .businessUnit(BusinessUnit.QUARRY).locationType(StockLocationType.DISPATCH_YARD).build());
        Block block = persist(Block.builder().quarry(quarry).blockCode("BLK-RND").extractionDate(DAY)
                .widthCm(100).lengthCm(200).heightCm(150).volumeM3(new BigDecimal("3.000"))
                .theoreticalWeightKg(new BigDecimal("8000.00")).actualWeightKg(new BigDecimal("7900.00"))
                .weightDeviationPct(new BigDecimal("-1.25")).stoneType("Burdur Bej")
                .currentLocation(yard).assignedCustomer(customer).build());
        blockId = block.getId();
        persist(Block.builder().quarry(quarry).blockCode("BLK-FAC").extractionDate(DAY)
                .widthCm(100).lengthCm(200).heightCm(150).volumeM3(new BigDecimal("3.000"))
                .theoreticalWeightKg(new BigDecimal("8000.00")).actualWeightKg(new BigDecimal("7900.00"))
                .weightDeviationPct(BigDecimal.ZERO).stoneType("Fabrika Bej").status(BlockStatus.AT_FACTORY).build());
        persist(Block.builder().quarry(quarry).blockCode("BLK-DSP").extractionDate(DAY)
                .widthCm(100).lengthCm(200).heightCm(150).volumeM3(new BigDecimal("3.000"))
                .theoreticalWeightKg(new BigDecimal("8000.00")).actualWeightKg(new BigDecimal("7900.00"))
                .weightDeviationPct(BigDecimal.ZERO).stoneType("Yolda Bej").status(BlockStatus.DISPATCHED).build());
        persist(BlockCustomerMark.builder().block(block).customer(customer).markedAt(DAY)
                .offerPrice(new BigDecimal("1000")).build());
        persist(BlockLocationMovement.builder().block(block).fromLocation(yard).toLocation(dispatch)
                .description("Render taşıma").build());

        ProductionOrder productionOrder = persist(ProductionOrder.builder().orderNo("PO-RND").block(block)
                .machineName("Katrak").processType(ProcessType.GANGSAW).startTime(LocalDateTime.of(2026, 11, 2, 8, 0))
                .build());
        productionOrderId = productionOrder.getId();
        Slab slab = persist(Slab.builder().slabCode("SLB-RND").productionOrder(productionOrder).block(block)
                .thicknessCm(new BigDecimal("2")).widthCm(new BigDecimal("100")).lengthCm(new BigDecimal("200"))
                .surfaceAreaM2(new BigDecimal("2.00")).customer(customer).build());
        slabId = slab.getId();
        persist(StockItem.builder().itemCode("STK-RND").productType(StockProductType.SIZED)
                .description("Render ebatlı").quantity(new BigDecimal("4")).sourceBlock(block)
                .stockLocation(yard).build());
        persist(Pallet.builder().palletCode("PAL-RND").warehouseLocation("A1").build());

        CutOrder cutOrder = persist(CutOrder.builder().cutOrderNo("CUT-RND").machineName("Köprü").build());
        cutOrderId = cutOrder.getId();
        persist(CutItem.builder().itemCode("CI-RND").cutOrder(cutOrder).sourceSlab(slab)
                .widthCm(new BigDecimal("30")).lengthCm(new BigDecimal("60")).thicknessCm(new BigDecimal("2"))
                .areaM2(new BigDecimal("0.18")).build());

        Project project = persist(Project.builder().projectCode("PRJ-RND").name("Render Projesi")
                .customerName("Render Müşteri").customer(customer).build());
        projectId = project.getId();
        ProjectLocation location = persist(ProjectLocation.builder().project(project).locationName("Lobi Duvar")
                .plannedAreaM2(new BigDecimal("10")).installedAreaM2(new BigDecimal("4")).build());
        persist(SiteConsumption.builder().location(location).project(project)
                .consumptionType(ConsumptionType.values()[0]).itemName("Render Yapıştırıcı")
                .quantity(BigDecimal.ONE).unit("KG").unitCost(BigDecimal.TEN).totalCost(BigDecimal.TEN)
                .recordedAt(LocalDateTime.of(2026, 11, 2, 9, 0)).build());
        persist(ConstructionSiteStonePlan.builder().project(project).location(location).stoneType("Render Plan Taşı")
                .plannedAreaM2(BigDecimal.TEN).requiredAreaM2(BigDecimal.TEN).build());
        persist(SiteInstallation.builder().project(project).location(location).installedAreaM2(BigDecimal.ONE)
                .installedOn(DAY).build());

        Invoice invoice = persist(Invoice.builder().invoiceNo("INV-RND").invoiceDate(DAY).customer(customer)
                .project(project).build());
        invoiceId = invoice.getId();
        persist(InvoiceItem.builder().invoice(invoice).productName("Render Fatura Kalemi").block(block).build());
        persist(CollectionRecord.builder().collectionNo("THS-RND").customer(customer).invoice(invoice)
                .amount(new BigDecimal("500")).collectionDate(DAY).build());
        persist(CheckRecord.builder().checkNo("CHK-RND").dueDate(DAY).bankName("Render Bank").customer(customer)
                .amount(new BigDecimal("750")).build());
        persist(CariTransaction.builder().customer(customer).description("Render cari").invoice(invoice).build());

        SalesOrder salesOrder = persist(SalesOrder.builder().orderNo("SO-RND").customer(customer).build());
        salesOrderId = salesOrder.getId();
        persist(SalesOrderItem.builder().salesOrder(salesOrder).description("Render satış kalemi").build());

        OperationWorkOrder workOrder = persist(OperationWorkOrder.builder().orderNo("WO-RND").customer(customer)
                .department(BusinessUnit.FACTORY).sourceBlock(block).build());
        workOrderId = workOrder.getId();
        persist(OperationWorkOrderItem.builder().workOrder(workOrder).notes("Render iş kalemi").build());

        PurchaseOrder purchaseOrder = persist(PurchaseOrder.builder().poNumber("PUR-RND").supplier(supplier)
                .businessUnit(BusinessUnit.FACTORY).build());
        purchaseOrderId = purchaseOrder.getId();
        persist(PurchaseOrderItem.builder().purchaseOrder(purchaseOrder).itemName("Render alım kalemi").build());
        persist(CostCenter.builder().code("CC-RND").name("Render Merkez").businessUnit(BusinessUnit.FACTORY)
                .monthlyBudget(new BigDecimal("1000")).build());

        Machine machine = persist(Machine.builder().code("MC-RND").name("Render Makine")
                .businessUnit(BusinessUnit.FACTORY).machineType(MachineType.values()[0]).build());
        persist(MachineFuelEntry.builder().machine(machine).entryDate(LocalDate.now()).litres(BigDecimal.TEN)
                .pricePerLitre(BigDecimal.ONE).totalAmount(BigDecimal.TEN).build());
        FactoryWorkOrder factoryWorkOrder = persist(FactoryWorkOrder.builder().orderNo("FWO-RND").block(block).build());
        persist(FactoryOperation.builder().workOrder(factoryWorkOrder).processType(FactoryProcessType.values()[0])
                .machine(machine).operatorName("Render Operatör").build());

        persist(StockMovement.builder().movementCode("MOV-RND").movementType(StockMovementType.values()[0])
                .sourceDepartment(BusinessUnit.QUARRY).block(block).customer(customer).notes("Render sevk").build());
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    private void expectPage(String url, String text) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString(text)));
    }

    @Test
    @DisplayName("invoice detail and list render from views")
    void invoicePages() throws Exception {
        expectPage("/invoices/" + invoiceId, "Render Fatura Kalemi");
        expectPage("/invoices", "INV-RND");
        expectPage("/invoices/new?purchaseOrderId=" + purchaseOrderId, "PUR-RND");
    }

    @Test
    @DisplayName("sales order detail renders from a view")
    void salesDetail() throws Exception {
        expectPage("/sales/" + salesOrderId, "Render satış kalemi");
        expectPage("/sales/create", "Render Müşteri");
    }

    @Test
    @DisplayName("production detail, slab detail and slab label render from views")
    void productionPages() throws Exception {
        expectPage("/production/orders/" + productionOrderId, "PO-RND");
        expectPage("/production/slabs/" + slabId, "SLB-RND");
        expectPage("/production/slabs/" + slabId + "/label", "SLB-RND");
        expectPage("/production/tablet", "Render Makine");
        expectPage("/production/polish", "Render Makine");
        expectPage("/production/create", "BLK-FAC");
        expectPage("/production/accept", "BLK-DSP");
    }

    @Test
    @DisplayName("workshop detail and cut order form render from views")
    void workshopPages() throws Exception {
        expectPage("/workshop/" + cutOrderId, "CI-RND");
        expectPage("/workshop/" + cutOrderId + "/edit", "CUT-RND");
        expectPage("/workshop/receipts", "Render Tedarik");
    }

    @Test
    @DisplayName("project detail renders locations, consumptions, plans and installations from views")
    void projectDetail() throws Exception {
        expectPage("/projects/" + projectId, "Lobi Duvar");
        expectPage("/projects/" + projectId, "Render Yapıştırıcı");
        expectPage("/projects/" + projectId, "Render Plan Taşı");
    }

    @Test
    @DisplayName("cari statement renders from views")
    void cariStatement() throws Exception {
        expectPage("/admin/definitions/customers/" + customerId + "/transactions", "Render Müşteri");
    }

    @Test
    @DisplayName("quarry pages render from views")
    void quarryPages() throws Exception {
        expectPage("/operations/quarry/dispatches", "BLK-RND");
        expectPage("/operations/quarry", "BLK-RND");
        expectPage("/operations/quarry/new", "Render Ocağı");
    }

    @Test
    @DisplayName("stock-yard pages render from views")
    void stockYardPages() throws Exception {
        expectPage("/operations/factory/blocks", "BLK-FAC");
        expectPage("/operations/factory/slabs", "SLB-RND");
        expectPage("/operations/factory/sized", "STK-RND");
    }

    @Test
    @DisplayName("work order screens render from views")
    void workOrderPages() throws Exception {
        expectPage("/work-orders/" + workOrderId, "WO-RND");
        expectPage("/work-orders", "WO-RND");
        expectPage("/work-orders/new", "BLK-RND");
        expectPage("/operations/factory/orders", "WO-RND");
    }

    @Test
    @DisplayName("block detail and sell screens render from views")
    void blockPages() throws Exception {
        expectPage("/blocks/" + blockId, "Render taşıma");
        expectPage("/invoices/create?blockId=" + blockId, "BLK-RND");
        expectPage("/blocks/create", "Render Ocağı");
    }

    @Test
    @DisplayName("procurement detail and edit form render from views")
    void procurementPages() throws Exception {
        expectPage("/procurement/" + purchaseOrderId, "Render alım kalemi");
        expectPage("/procurement/" + purchaseOrderId + "/edit", "PUR-RND");
        expectPage("/procurement/create", "Render Tedarik");
    }

    @Test
    @DisplayName("digital passport renders block, slab and pallet views")
    void passportPages() throws Exception {
        expectPage("/passport/BLK-RND", "Render Ocağı");
        expectPage("/passport/SLB-RND", "BLK-RND");
        expectPage("/passport/PAL-RND", "A1");
    }

    @Test
    @DisplayName("collection and check lists render from views")
    void collectionPages() throws Exception {
        expectPage("/collections", "THS-RND");
        expectPage("/collections/checks", "CHK-RND");
    }

    @Test
    @DisplayName("fuel, cost and genealogy pages render from views")
    void otherPages() throws Exception {
        expectPage("/machines/fuel", "Render Makine");
        expectPage("/costs", "Render Merkez");
        expectPage("/genealogy", "BLK-RND");
    }

    @Test
    @DisplayName("list, form and edit screens that take view DTOs answer 200")
    void remainingScreensRender() throws Exception {
        String[] urls = {
            "/blocks",
            "/blocks/" + blockId + "/edit",
            "/blocks/" + blockId + "/move",
            "/production",
            "/production/slabs",
            "/production/orders/" + productionOrderId + "/edit",
            "/production/slabs/" + slabId + "/edit",
            "/projects/" + projectId + "/edit",
            "/sales/" + salesOrderId + "/edit",
            "/operations/factory/fuel",
            "/operations/factory/consumables",
            "/operations/factory/slabs/new",
            "/operations/factory/sized/new",
            "/operations/workshop",
            "/operations/workshop/slabs",
            "/operations/workshop/sized",
            "/operations/workshop/sized/new",
            "/quarry/fuel",
            "/quarry/consumables",
            "/quarry/machines",
            "/machines/fuel/create",
            "/expenses",
            "/expenses/create",
            "/admin/users/create",
            "/admin/email-templates",
            "/admin/settings",
            "/workshop",
            "/workshop/create",
            "/collections/create",
            "/projects",
            "/projects/create",
            "/reports",
            "/reports/quarry",
            "/reports/factory",
            "/reports/invoices",
            "/reports/collections",
            "/reports/work-orders",
            "/operations/quarry/" + blockId + "/dispatch",
            "/sales",
            "/procurement",
            "/admin/users",
        };
        List<String> failed = new ArrayList<>();
        for (String url : urls) {
            try {
                int status = mockMvc.perform(MockMvcRequestBuilders.get(url)).andReturn().getResponse().getStatus();
                if (status != 200) {
                    failed.add(url + " -> " + status);
                }
            } catch (ServletException e) {
                failed.add(url + " -> " + e.getMessage());
            }
        }
        Assertions.assertEquals(List.of(), failed);
    }

    @Test
    @DisplayName("list grids render the İşlemler dropdown and do not freeze leading columns")
    void listGridsRenderIslemlerDropdownWithoutStickyColumns() throws Exception {
        String[] urls = {
            "/blocks",
            "/projects",
            "/sales",
            "/procurement",
            "/production",
            "/production/slabs",
            "/workshop",
            "/expenses",
            "/admin/users",
            "/invoices"
        };
        for (String url : urls) {
            String html = mockMvc.perform(MockMvcRequestBuilders.get(url))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            Assertions.assertTrue(html.contains("İşlemler"), url + " missing İşlemler column");
            Assertions.assertFalse(html.contains("is-frozen"), url + " still has sticky columns");
            Assertions.assertTrue(
                    html.contains("data-frozen=\"0\"") || html.contains("data-frozen=0"),
                    url + " should advertise zero frozen columns");
        }
        String blocks = mockMvc.perform(MockMvcRequestBuilders.get("/blocks").param("stockFilter", "ALL"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Assertions.assertTrue(blocks.contains("grid-actions-btn"), "blocks grid missing dropdown trigger");
        Assertions.assertTrue(blocks.contains("Detay ve işlemler"), "blocks grid missing historical view action");
        Assertions.assertTrue(blocks.contains("Düzenle"), "blocks grid missing edit action");
    }

}
