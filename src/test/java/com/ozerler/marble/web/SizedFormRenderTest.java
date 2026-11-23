package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.enums.ProcessType;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.StockProductType;

/**
 * Renders both sized-form pages with slabs and stock items that have, and have not got,
 * a customer. Slab.block is required in the schema, so a missing block is covered by
 * SlabView tests. The persistence context is cleared after seeding so associations are
 * not already open, matching production with open-in-view disabled.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "ADMIN")
class SizedFormRenderTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 12);

    @Autowired
    private WebApplicationContext context;

    @PersistenceContext
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private Long assignedSlabId;
    private Long generalSlabId;
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        seed();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("factory sized-form renders assigned, general-stock and no-block slabs")
    void factorySizedFormRendersCustomerAndGeneralStockSlabs() throws Exception {
        String html = render("/operations/factory/sized/new");
        Assertions.assertTrue(html.contains("SLB-CUST"), html);
        Assertions.assertTrue(html.contains("Ebat Müşteri"), html);
        Assertions.assertTrue(html.contains("SLB-GEN"), html);
        Assertions.assertTrue(html.contains("Genel Stok"), html);
        Assertions.assertTrue(html.contains("Burdur Bej"), html);
    }

    @Test
    @DisplayName("workshop sized-form renders slabs and stock items with and without a customer")
    void workshopSizedFormRendersSlabsAndStockItems() throws Exception {
        String html = render("/operations/workshop/sized/new");
        Assertions.assertTrue(html.contains("SLB-CUST"), html);
        Assertions.assertTrue(html.contains("Ebat Müşteri"), html);
        Assertions.assertTrue(html.contains("SLB-GEN"), html);
        Assertions.assertTrue(html.contains("Genel Stok"), html);
        Assertions.assertTrue(html.contains("STK-CUST"), html);
        Assertions.assertTrue(html.contains("STK-GEN"), html);
    }

    @Test
    @DisplayName("workshop sized-form prefills a general-stock slab without a customer or exception")
    void workshopSizedFormPrefillsGeneralStockSlab() throws Exception {
        String html = render("/operations/workshop/sized/new?slabId=" + generalSlabId);
        Assertions.assertTrue(html.contains("SLB-GEN"), html);
        Assertions.assertTrue(html.contains("data-slab-desc"), html);
    }

    @Test
    @DisplayName("workshop sized-form prefills a slab that has a customer and a block")
    void workshopSizedFormPrefillsAssignedSlab() throws Exception {
        String html = render("/operations/workshop/sized/new?slabId=" + assignedSlabId);
        Assertions.assertTrue(html.contains("SLB-CUST"), html);
        Assertions.assertTrue(html.contains(String.valueOf(assignedSlabId)), html);
    }

    private String render(String url) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void seed() {
        Customer customer = persist(Customer.builder()
                .customerCode("C-SIZE")
                .companyName("Ebat Müşteri")
                .build());
        Quarry quarry = persist(Quarry.builder()
                .code("Q-SIZE")
                .name("Ebat Ocağı")
                .location("Burdur")
                .build());
        Block block = persist(Block.builder()
                .quarry(quarry)
                .blockCode("BLK-SIZE")
                .extractionDate(DAY)
                .widthCm(100)
                .lengthCm(200)
                .heightCm(150)
                .volumeM3(new BigDecimal("3.000"))
                .theoreticalWeightKg(new BigDecimal("8000.00"))
                .actualWeightKg(new BigDecimal("7900.00"))
                .weightDeviationPct(new BigDecimal("-1.25"))
                .stoneType("Burdur Bej")
                .build());

        ProductionOrder productionOrder = persist(ProductionOrder.builder()
                .orderNo("PO-SIZE")
                .block(block)
                .machineName("Katrak")
                .processType(ProcessType.GANGSAW)
                .startTime(LocalDateTime.of(2026, 11, 12, 8, 0))
                .build());

        Slab assigned = persist(slab("SLB-CUST", productionOrder, block, customer));
        assignedSlabId = assigned.getId();
        Slab general = persist(slab("SLB-GEN", productionOrder, block, null));
        generalSlabId = general.getId();

        persist(StockItem.builder()
                .itemCode("STK-CUST")
                .productType(StockProductType.SIZED)
                .description("Müşteri ebatlı")
                .quantity(new BigDecimal("2"))
                .unit("m2")
                .thicknessCm(new BigDecimal("2"))
                .widthCm(new BigDecimal("30"))
                .lengthCm(new BigDecimal("60"))
                .pieceCount(2)
                .customer(customer)
                .status("AVAILABLE")
                .build());
        persist(StockItem.builder()
                .itemCode("STK-GEN")
                .productType(StockProductType.SIZED)
                .description("Genel ebatlı")
                .quantity(new BigDecimal("3"))
                .unit("m2")
                .thicknessCm(new BigDecimal("2"))
                .widthCm(new BigDecimal("40"))
                .lengthCm(new BigDecimal("80"))
                .pieceCount(1)
                .status("AVAILABLE")
                .build());
    }

    private Slab slab(String code, ProductionOrder productionOrder, Block block, Customer customer) {
        return Slab.builder()
                .slabCode(code)
                .productionOrder(productionOrder)
                .block(block)
                .customer(customer)
                .thicknessCm(new BigDecimal("2"))
                .widthCm(new BigDecimal("100"))
                .lengthCm(new BigDecimal("200"))
                .surfaceAreaM2(new BigDecimal("2.00"))
                .status(SlabStatus.AVAILABLE)
                .build();
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
