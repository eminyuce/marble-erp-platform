package com.ozerler.marble.controller.admin;

import com.ozerler.marble.dto.DashboardKpiDto;
import com.ozerler.marble.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Genel Müdür Tek Ekran Canlı Kokpiti (BRD Section 9.1)
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardKpiDto kpis = dashboardService.getDashboardKpis();

        model.addAttribute("totalSlabArea", kpis.getTotalSlabArea());
        model.addAttribute("availableSlabArea", kpis.getAvailableSlabArea());
        model.addAttribute("reservedSlabArea", kpis.getReservedSlabArea());
        model.addAttribute("factoryBlockCount", kpis.getFactoryBlockCount());
        model.addAttribute("factoryBlockWeightTon", kpis.getFactoryBlockWeightTon());
        model.addAttribute("activeProjectsCount", kpis.getActiveProjectsCount());
        model.addAttribute("totalScrapImpact", kpis.getTotalScrapImpact());
        model.addAttribute("scrapSummary", kpis.getScrapSummary());
        model.addAttribute("totalUsers", kpis.getTotalUsers());
        model.addAttribute("costDistribution", kpis.getCostDistribution());

        return "admin/dashboard";
    }

    /**
     * Kullanıcı Yardım ve Sistem Özellikleri Rehberi (Site Features Guide)
     */
    @GetMapping({"/dashboard/oursitefeatures", "/dashboard/oursitefeatures/"})
    public String ourSiteFeatures(Model model) {
        model.addAttribute("currentSection", "oursitefeatures");
        return "admin/site-features";
    }
}
