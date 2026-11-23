package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.time.LocalDate;

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
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockLocationType;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "ADMIN")
class GridHeaderFilterRenderTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 2);

    @Autowired
    private WebApplicationContext context;

    @PersistenceContext
    private EntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        Quarry quarry = persist(Quarry.builder().code("Q-FLT").name("Filtre Ocağı").location("Burdur").build());
        StockLocation yard = persist(StockLocation.builder().code("YRD-FLT").name("Filtre Saha")
                .businessUnit(BusinessUnit.QUARRY).locationType(StockLocationType.PRODUCTION_YARD).build());
        persist(Block.builder().quarry(quarry).blockCode("BLK-KEEP").extractionDate(DAY)
                .widthCm(100).lengthCm(200).heightCm(150).volumeM3(new BigDecimal("3.000"))
                .theoreticalWeightKg(new BigDecimal("8000.00")).actualWeightKg(new BigDecimal("7900.00"))
                .weightDeviationPct(new BigDecimal("-1.25"))
                .currentLocation(yard).stoneType("Bej").build());
        persist(Block.builder().quarry(quarry).blockCode("BLK-DROP").extractionDate(DAY)
                .widthCm(100).lengthCm(200).heightCm(150).volumeM3(new BigDecimal("3.000"))
                .theoreticalWeightKg(new BigDecimal("8000.00")).actualWeightKg(new BigDecimal("7900.00"))
                .weightDeviationPct(BigDecimal.ZERO)
                .currentLocation(yard).stoneType("Gri").build());
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Blocks grid renders the shared header filter and keeps it in the URL")
    void blocksGridRendersHeaderFilterControls() throws Exception {
        String html = mockMvc.perform(MockMvcRequestBuilders.get("/blocks")
                        .param("stockFilter", "ALL"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Assertions.assertTrue(html.contains("erp-data-grid__colfilter-btn"), "missing header filter button");
        Assertions.assertTrue(html.contains("İçerir"), "missing contains operator");
        Assertions.assertTrue(html.contains("Eşittir"), "missing equals operator");
        Assertions.assertTrue(html.contains("İle Başlar"), "missing starts-with operator");
        Assertions.assertTrue(html.contains("İçermez"), "missing not-contains operator");
        Assertions.assertTrue(html.contains("name=\"cf.blockCode\""), "missing blockCode filter field");
        Assertions.assertTrue(html.contains("Aralık"), "missing between operator");
        Assertions.assertTrue(html.contains("Önce") && html.contains("Sonra"), "missing date comparison operators");
        Assertions.assertTrue(html.contains("name=\"cf.extractionDate\""), "missing date filter field");
        Assertions.assertTrue(html.contains("name=\"ct.extractionDate\""), "missing date range end field");
        Assertions.assertTrue(html.contains("type=\"date\""), "missing date input");
        Assertions.assertTrue(html.contains("name=\"cf.totalCost\""), "missing numeric filter field");
        Assertions.assertTrue(html.contains("/grid-filter-values?grid=blocks-table"), "missing distinct-values URL");
        Assertions.assertTrue(html.contains("/js/app.js?v=1.0.7"),
                "app.js should be cache-busted with the app version");
        Assertions.assertTrue(html.contains("admin-topbar-version") && html.contains("1.0.7"),
                "header should show the application version");
        Assertions.assertTrue(html.contains("admin-app-footer") && html.contains("Sürüm"),
                "footer should show the application version");
    }

    @Test
    @DisplayName("Contains filter is applied server-side and other rows disappear")
    void containsFilterIsAppliedServerSide() throws Exception {
        String html = mockMvc.perform(MockMvcRequestBuilders.get("/blocks")
                        .param("stockFilter", "ALL")
                        .param("cf.blockCode", "KEEP")
                        .param("co.blockCode", "contains"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Assertions.assertTrue(html.contains("BLK-KEEP"), "matching row should remain");
        Assertions.assertFalse(html.contains("BLK-DROP"), "non-matching row should be filtered out");
        Assertions.assertTrue(html.contains("Filtreleri temizle"), "reset link should appear when a filter is active");
    }

    @Test
    @DisplayName("Unknown filter columns are ignored and do not 500")
    void unknownFilterColumnIsIgnored() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/blocks")
                        .param("stockFilter", "ALL")
                        .param("cf.notAColumn", "x")
                        .param("cf.drop;table", "x"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Distinct values endpoint returns JSON for a whitelisted column")
    void distinctValuesEndpointReturnsJson() throws Exception {
        String body = mockMvc.perform(MockMvcRequestBuilders.get("/grid-filter-values")
                        .param("grid", "blocks-table")
                        .param("column", "blockCode"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Assertions.assertTrue(body.contains("BLK-KEEP"));
        Assertions.assertTrue(body.contains("BLK-DROP"));
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
