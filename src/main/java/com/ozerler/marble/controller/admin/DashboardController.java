package com.ozerler.marble.controller.admin;

import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.ProjectStatus;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DashboardController {

    private final BlockRepository blockRepository;
    private final SlabRepository slabRepository;
    private final ProjectRepository projectRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final ScrapLogRepository scrapLogRepository;
    private final UserRepository userRepository;
    private final CostTransactionRepository costTransactionRepository;

    /**
     * Genel Müdür Tek Ekran Canlı Kokpiti (BRD Section 9.1)
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // KPI 1: Physical Stock
        BigDecimal totalSlabArea = slabRepository.getTotalInventoryAreaM2();
        BigDecimal availableSlabArea = slabRepository.getTotalAreaByStatus(SlabStatus.AVAILABLE);
        BigDecimal reservedSlabArea = slabRepository.getTotalAreaByStatus(SlabStatus.RESERVED);
        long factoryBlockCount = blockRepository.countByStatus(BlockStatus.FACTORY_STOCK);
        Double factoryBlockWeight = blockRepository.getTotalFactoryStockWeightKg();

        // KPI 2: Active Projects
        long activeProjectsCount = projectRepository.countByStatus(ProjectStatus.ACTIVE);

        // KPI 3: Scrap & Waste Impact
        BigDecimal totalScrapImpact = scrapLogRepository.getTotalScrapCostImpact();
        List<Object[]> scrapSummary = scrapLogRepository.getScrapSummaryByReason();

        // KPI 4: Users & System metrics
        long totalUsers = userRepository.countByDeletedFalse();

        // Cost distribution
        List<Object[]> costDistribution = costTransactionRepository.getCostDistributionByCenter();

        model.addAttribute("totalSlabArea", totalSlabArea);
        model.addAttribute("availableSlabArea", availableSlabArea);
        model.addAttribute("reservedSlabArea", reservedSlabArea);
        model.addAttribute("factoryBlockCount", factoryBlockCount);
        model.addAttribute("factoryBlockWeightTon", factoryBlockWeight != null ? factoryBlockWeight / 1000.0 : 0.0);
        model.addAttribute("activeProjectsCount", activeProjectsCount);
        model.addAttribute("totalScrapImpact", totalScrapImpact);
        model.addAttribute("scrapSummary", scrapSummary);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("costDistribution", costDistribution);

        return "admin/dashboard";
    }
}
