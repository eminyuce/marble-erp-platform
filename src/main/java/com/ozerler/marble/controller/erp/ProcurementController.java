package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.CostCenterDto;
import com.ozerler.marble.dto.PurchaseOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.PurchaseOrder;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.PurchaseOrderStatus;
import com.ozerler.marble.service.ProcurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/procurement")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;
    private final org.springframework.context.MessageSource messageSource;

    @GetMapping
    public String procurementIndex(Model model) {
        model.addAttribute("statuses", PurchaseOrderStatus.values());
        return "erp/procurement/index";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<PurchaseOrderDto> getProcurementData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {

        return procurementService.getPurchaseOrdersPaged(page, size, search, sortField, sortDir);
    }

    @GetMapping("/create")
    public String showCreateForm(Model model, java.util.Locale locale) {
        populateProcurementForm(model, locale);
        return "erp/procurement/form";
    }

    @GetMapping("/api/cost-centers")
    @ResponseBody
    public List<CostCenterDto> costCentersForUnit(@RequestParam("businessUnit") BusinessUnit businessUnit) {
        return procurementService.getCostCentersForUnit(businessUnit).stream()
                .map(CostCenterDto::fromEntity)
                .toList();
    }

    @PostMapping("/create")
    public String createOrder(@RequestParam("poNumber") String poNumber,
                              @RequestParam("supplierId") Long supplierId,
                              @RequestParam("businessUnit") BusinessUnit businessUnit,
                              @RequestParam(value = "costCenterId", required = false) Long costCenterId,
                              @RequestParam(value = "projectId", required = false) Long projectId,
                              @RequestParam(value = "expectedDelivery", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedDelivery,
                              @RequestParam(value = "notes", required = false) String notes,
                              Model model,
                              RedirectAttributes redirectAttributes,
                              java.util.Locale locale) {
        try {
            PurchaseOrder order = procurementService.createPurchaseOrder(
                    poNumber, supplierId, projectId, businessUnit, costCenterId, expectedDelivery, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.procurement.create.success", new Object[]{order.getPoNumber()}, locale));
            return "redirect:/procurement";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            populateProcurementForm(model, locale);
            return "erp/procurement/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model, java.util.Locale locale) {
        PurchaseOrder order = procurementService.getOrderById(id);
        populateProcurementForm(model, locale);
        model.addAttribute("record", order);
        model.addAttribute("isEdit", true);
        model.addAttribute("pageTitle", "Satınalma Siparişi Düzenle: " + order.getPoNumber());
        return "erp/procurement/form";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable("id") Long id, Model model) {
        PurchaseOrder order = procurementService.getOrderById(id);
        model.addAttribute("order", order);
        return "erp/procurement/detail";
    }

    @PostMapping("/{id}/items")
    public String addItem(@PathVariable("id") Long id,
                          @RequestParam("itemName") String itemName,
                          @RequestParam(value = "itemType", defaultValue = "CONSUMABLE") String itemType,
                          @RequestParam("quantity") BigDecimal quantity,
                          @RequestParam(value = "unit", defaultValue = "ADET") String unit,
                          @RequestParam("unitPrice") BigDecimal unitPrice) {
        procurementService.addItemToOrder(id, itemName, itemType, quantity, unit, unitPrice);
        return "redirect:/procurement/" + id;
    }

    @PostMapping("/{id}/confirm")
    public String confirmOrder(@PathVariable("id") Long id) {
        procurementService.updateStatus(id, PurchaseOrderStatus.CONFIRMED);
        return "redirect:/procurement/" + id;
    }

    private void populateProcurementForm(Model model, java.util.Locale locale) {
        model.addAttribute("suppliers", procurementService.getAllSuppliers());
        model.addAttribute("projects", procurementService.getAllProjects());
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("generatedPoNumber", procurementService.generatePoNumber());
        model.addAttribute("pageTitle", messageSource.getMessage("erp.procurement.title.create", null, locale));
        PurchaseOrder record = (PurchaseOrder) model.getAttribute("record");
        if (record != null && record.getBusinessUnit() != null) {
            model.addAttribute("costCenters", procurementService.getCostCentersForUnit(record.getBusinessUnit()));
        }
    }
}
