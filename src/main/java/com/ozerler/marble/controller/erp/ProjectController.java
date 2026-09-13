package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.ProjectDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.enums.ConsumptionType;
import com.ozerler.marble.service.ProjectSiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
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
    private final MessageSource messageSource;

    @GetMapping
    public String projectsIndex() {
        return "erp/projects/index";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<ProjectDto> getProjectsData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {

        return projectSiteService.getProjectsPaged(page, size, search, sortField, sortDir);
    }

    @GetMapping("/create")
    public String showCreateForm(Locale locale, Model model) {
        model.addAttribute("pageTitle", messageSource.getMessage("erp.project.title.create", null, locale));
        return "erp/projects/form";
    }

    @PostMapping("/create")
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

    @GetMapping("/{id}")
    public String projectDetail(@PathVariable("id") Long id, Model model) {
        Project project = projectSiteService.getProjectById(id);
        model.addAttribute("project", project);
        model.addAttribute("locations", projectSiteService.getLocationsByProject(id));
        model.addAttribute("consumptions", projectSiteService.getConsumptionsByProject(id));
        model.addAttribute("consumptionTypes", ConsumptionType.values());
        return "erp/projects/detail";
    }

    @PostMapping("/{id}/locations")
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
    public String recordConsumption(@PathVariable("id") Long projectId,
                                    @RequestParam("locationId") Long locationId,
                                    @RequestParam("consumptionType") ConsumptionType type,
                                    @RequestParam("itemName") String itemName,
                                    @RequestParam("quantity") BigDecimal quantity,
                                    @RequestParam("unit") String unit,
                                    @RequestParam("unitCost") BigDecimal unitCost,
                                    @RequestParam(value = "notes", required = false) String notes) {

        projectSiteService.recordConsumption(projectId, locationId, type, itemName, quantity, unit, unitCost, notes);
        return "redirect:/projects/" + projectId;
    }
}
