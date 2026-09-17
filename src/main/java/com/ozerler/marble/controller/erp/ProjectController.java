package com.ozerler.marble.controller.erp;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.ProjectDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.enums.ConsumptionType;
import com.ozerler.marble.model.enums.ProjectStatus;
import com.ozerler.marble.model.enums.SupplyRoute;
import com.ozerler.marble.model.enums.SurfaceFinish;
import com.ozerler.marble.service.ConstructionSiteService;
import com.ozerler.marble.service.CostAnalysisService;
import com.ozerler.marble.service.ProjectSiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectSiteService projectSiteService;
    private final ConstructionSiteService constructionSiteService;
    private final CostAnalysisService costAnalysisService;
    private final MessageSource messageSource;

    @GetMapping
    public String projectsIndex(Model model) {
        model.addAttribute("summary", projectSiteService.getProjectSummary());
        return "erp/projects/index";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<ProjectDto> getProjectsData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {

        return projectSiteService.getProjectsPaged(page, size, search, status, sortField, sortDir);
    }

    @GetMapping("/create")
    public String showCreateForm(Locale locale, Model model) {
        model.addAttribute("pageTitle", messageSource.getMessage("erp.project.title.create", null, locale));
        return "erp/projects/form";
    }

    @PostMapping("/create")
    @PreAuthorize(Constants.PRE_AUTH_SITE_WRITE)
    public String createProject(@RequestParam("projectCode") String projectCode,
                                @RequestParam("name") String name,
                                @RequestParam("customerName") String customerName,
                                @RequestParam("contractValue") BigDecimal contractValue,
                                @RequestParam("estimatedCost") BigDecimal estimatedCost,
                                @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                @RequestParam(value = "deliveryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                                @RequestParam(value = "notes", required = false) String notes,
                                Locale locale,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        try {
            projectSiteService.createProject(projectCode, name, customerName, contractValue, estimatedCost, startDate, deliveryDate, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.project.create.success", null, locale));
            return "redirect:/projects";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            model.addAttribute("pageTitle", messageSource.getMessage("erp.project.title.create", null, locale));
            return "erp/projects/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Locale locale, Model model) {
        Project project = projectSiteService.getProjectById(id);
        model.addAttribute("project", project);
        model.addAttribute("record", project);
        model.addAttribute("isEdit", true);
        model.addAttribute("pageTitle",
                messageSource.getMessage("erp.project.title.edit", null, locale) + ": " + project.getName());
        return "erp/projects/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize(Constants.PRE_AUTH_SITE_WRITE)
    public String updateProject(@PathVariable("id") Long id,
                                @RequestParam("projectCode") String projectCode,
                                @RequestParam("name") String name,
                                @RequestParam("customerName") String customerName,
                                @RequestParam(value = "customerId", required = false) Long customerId,
                                @RequestParam("contractValue") BigDecimal contractValue,
                                @RequestParam("estimatedCost") BigDecimal estimatedCost,
                                @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                @RequestParam(value = "deliveryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                                @RequestParam(value = "notes", required = false) String notes,
                                @RequestParam(value = "status", required = false) ProjectStatus status,
                                Locale locale,
                                RedirectAttributes redirectAttributes) {
        constructionSiteService.updateProject(id, projectCode, name, customerName, customerId,
                contractValue, estimatedCost, startDate, deliveryDate, notes, status);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.project.update.success", null, locale));
        return "redirect:/projects/" + id;
    }

    @GetMapping("/{id}")
    public String projectDetail(@PathVariable("id") Long id, Model model) {
        Project project = projectSiteService.getProjectById(id);
        model.addAttribute("project", project);
        model.addAttribute("locations", projectSiteService.getLocationsByProject(id));
        model.addAttribute("consumptions", projectSiteService.getConsumptionsByProject(id));
        model.addAttribute("consumptionTypes", ConsumptionType.values());
        model.addAttribute("stonePlans", constructionSiteService.plansFor(id));
        model.addAttribute("installations", constructionSiteService.installationsFor(id));
        model.addAttribute("profit", costAnalysisService.profitFor(project));
        model.addAttribute("supplyRoutes", SupplyRoute.values());
        model.addAttribute("surfaceFinishes", SurfaceFinish.values());
        return "erp/projects/detail";
    }

    @PostMapping("/{id}/locations")
    @PreAuthorize(Constants.PRE_AUTH_SITE_WRITE)
    public String addLocation(@PathVariable("id") Long projectId,
                              @RequestParam(value = "parentId", required = false) Long parentId,
                              @RequestParam("locationName") String locationName,
                              @RequestParam(value = "floorLevel", required = false) String floorLevel,
                              @RequestParam(value = "stoneSpec", required = false) String stoneSpec,
                              @RequestParam("plannedAreaM2") BigDecimal plannedAreaM2) {

        projectSiteService.addLocation(projectId, parentId, locationName, floorLevel, stoneSpec, plannedAreaM2);
        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/{id}/consumptions")
    @PreAuthorize(Constants.PRE_AUTH_SITE_WRITE)
    public String recordConsumption(@PathVariable("id") Long projectId,
                                    @RequestParam("locationId") Long locationId,
                                    @RequestParam("consumptionType") ConsumptionType type,
                                    @RequestParam("itemName") String itemName,
                                    @RequestParam("quantity") BigDecimal quantity,
                                    @RequestParam("unit") String unit,
                                    @RequestParam("unitCost") BigDecimal unitCost,
                                    @RequestParam(value = "notes", required = false) String notes) {

        projectSiteService.recordConsumption(projectId, locationId, type, itemName, quantity, unit, unitCost, notes);
        constructionSiteService.recalculateActualCost(projectId);
        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/{id}/plans")
    @PreAuthorize(Constants.PRE_AUTH_SITE_WRITE)
    public String addStonePlan(@PathVariable("id") Long projectId,
                               @RequestParam("locationId") Long locationId,
                               @RequestParam("stoneType") String stoneType,
                               @RequestParam(value = "surfaceFinish", required = false) SurfaceFinish surfaceFinish,
                               @RequestParam(value = "widthCm", required = false) BigDecimal widthCm,
                               @RequestParam(value = "lengthCm", required = false) BigDecimal lengthCm,
                               @RequestParam("plannedAreaM2") BigDecimal plannedAreaM2,
                               @RequestParam(value = "scrapPercent", required = false) BigDecimal scrapPercent,
                               @RequestParam(value = "supplyRoute", required = false) SupplyRoute supplyRoute,
                               @RequestParam(value = "notes", required = false) String notes,
                               Locale locale,
                               RedirectAttributes redirectAttributes) {
        constructionSiteService.addStonePlan(projectId, locationId, stoneType, surfaceFinish, widthCm, lengthCm,
                plannedAreaM2, scrapPercent, supplyRoute, notes);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.project.plan.success", null, locale));
        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/{id}/installations")
    @PreAuthorize(Constants.PRE_AUTH_SITE_WRITE)
    public String recordInstallation(@PathVariable("id") Long projectId,
                                     @RequestParam("locationId") Long locationId,
                                     @RequestParam(value = "materialLotId", required = false) Long materialLotId,
                                     @RequestParam(value = "palletId", required = false) Long palletId,
                                     @RequestParam("installedAreaM2") BigDecimal installedAreaM2,
                                     @RequestParam(value = "wasteAreaM2", required = false) BigDecimal wasteAreaM2,
                                     @RequestParam(value = "installedOn", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate installedOn,
                                     @RequestParam(value = "crewName", required = false) String crewName,
                                     @RequestParam(value = "notes", required = false) String notes,
                                     Locale locale,
                                     RedirectAttributes redirectAttributes) {
        constructionSiteService.recordInstallation(projectId, locationId, materialLotId, palletId,
                installedAreaM2, wasteAreaM2, installedOn, crewName, notes);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.project.install.success", null, locale));
        return "redirect:/projects/" + projectId;
    }
}
