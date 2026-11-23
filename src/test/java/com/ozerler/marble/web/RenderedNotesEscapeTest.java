package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hamcrest.Matchers;
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
import com.ozerler.marble.model.CutOrder;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.enums.ProcessType;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CutOrderRepository;
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.QuarryRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RenderedNotesEscapeTest {

    private static final String RAW = "<script>alert(1)</script>";
    private static final String ESCAPED = "&lt;script&gt;alert(1)&lt;/script&gt;";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private QuarryRepository quarryRepository;
    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private ProductionOrderRepository productionOrderRepository;
    @Autowired
    private CutOrderRepository cutOrderRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("production detail escapes a script stored in the order notes")
    void productionNotesAreEscaped() throws Exception {
        Quarry quarry = quarryRepository.save(Quarry.builder().code("Q-NOTE").name("Not Ocağı").location("Burdur").build());
        Block block = blockRepository.save(Block.builder()
                .quarry(quarry)
                .blockCode("BLK-NOTE")
                .extractionDate(LocalDate.of(2026, 11, 1))
                .widthCm(100)
                .lengthCm(200)
                .heightCm(150)
                .volumeM3(new BigDecimal("3.000"))
                .theoreticalWeightKg(new BigDecimal("8000.00"))
                .actualWeightKg(new BigDecimal("7900.00"))
                .weightDeviationPct(new BigDecimal("-1.25"))
                .stoneType("Burdur Bej")
                .build());
        ProductionOrder order = productionOrderRepository.save(ProductionOrder.builder()
                .orderNo("PO-NOTE")
                .block(block)
                .machineName("Katrak")
                .processType(ProcessType.GANGSAW)
                .startTime(LocalDateTime.of(2026, 11, 2, 8, 0))
                .notes(RAW)
                .build());

        mockMvc.perform(MockMvcRequestBuilders.get("/production/orders/" + order.getId()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString(ESCAPED)))
                .andExpect(MockMvcResultMatchers.content().string(Matchers.not(Matchers.containsString(RAW))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("workshop detail escapes a script stored in the cut order notes")
    void workshopNotesAreEscaped() throws Exception {
        CutOrder order = cutOrderRepository.save(CutOrder.builder()
                .cutOrderNo("CUT-NOTE")
                .machineName("Köprü")
                .notes(RAW)
                .build());

        mockMvc.perform(MockMvcRequestBuilders.get("/workshop/" + order.getId()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString(ESCAPED)))
                .andExpect(MockMvcResultMatchers.content().string(Matchers.not(Matchers.containsString(RAW))));
    }
}
