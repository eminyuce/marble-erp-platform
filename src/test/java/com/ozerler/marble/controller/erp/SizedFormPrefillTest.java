package com.ozerler.marble.controller.erp;

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
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.ProcessType;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.QuarryRepository;
import com.ozerler.marble.repository.SlabRepository;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@ActiveProfiles("test")
@Transactional
class SizedFormPrefillTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private QuarryRepository quarryRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private ProductionOrderRepository productionOrderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SlabRepository slabRepository;

    private MockMvc mockMvc;
    private Slab slab;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.webAppContextSetup(context)
                        .apply(SecurityMockMvcConfigurers.springSecurity())
                        .build();
        Quarry quarry = quarryRepository.save(Quarry.builder()
                .code("Q-SIZED")
                .name("E2E Ocak")
                .location("Burdur")
                .build());
        Block block = blockRepository.save(Block.builder()
                .quarry(quarry)
                .blockCode("BLK-SIZED")
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
                .orderNo("PO-SIZED")
                .block(block)
                .machineName("Katrak")
                .processType(ProcessType.GANGSAW)
                .startTime(LocalDateTime.of(2026, 11, 2, 8, 0))
                .build());
        Customer customer = customerRepository.save(Customer.builder()
                .customerCode("C-SIZED")
                .companyName("E2E Mermer")
                .build());
        slab = slabRepository.save(Slab.builder()
                .slabCode("SLB-SIZED")
                .productionOrder(order)
                .block(block)
                .customer(customer)
                .thicknessCm(new BigDecimal("2.00"))
                .widthCm(new BigDecimal("60.00"))
                .lengthCm(new BigDecimal("120.00"))
                .surfaceAreaM2(new BigDecimal("0.7200"))
                .status(SlabStatus.AVAILABLE)
                .build());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("workshop sized form renders the prefilled slab without a lazy session")
    void workshopSizedFormRendersPrefill() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/operations/workshop/sized/new")
                                .param("slabId", slab.getId().toString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(Matchers.containsString("SLB-SIZED")))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(Matchers.containsString("Burdur Bej")))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(Matchers.containsString("E2E Mermer")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("factory sized form renders slab block and customer outside the session")
    void factorySizedFormRendersSlabList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/operations/factory/sized/new"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("SLB-SIZED")))
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("Burdur Bej")))
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("E2E Mermer")));
    }
}
