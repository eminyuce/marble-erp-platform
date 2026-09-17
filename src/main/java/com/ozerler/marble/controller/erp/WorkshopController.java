package com.ozerler.marble.controller.erp;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.CutOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.CutOrder;
import com.ozerler.marble.model.enums.WorkshopProcessType;
import com.ozerler.marble.model.enums.WorkshopReceiptSource;
import com.ozerler.marble.model.enums.WorkshopWorkPurpose;
import com.ozerler.marble.service.WorkshopCutService;
import com.ozerler.marble.service.WorkshopOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Controller
@RequestMapping("/workshop")
@RequiredArgsConstructor
public class WorkshopController {

    private final WorkshopCutService workshopCutService;
    private final WorkshopOperationService workshopOperationService;
    private final MessageSource messageSource;

    @GetMapping
    public String workshopIndex(Model model) {
        model.addAttribute("summary", workshopCutService.getWorkshopSummary());
        return "erp/workshop/index";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<CutOrderDto> getCutOrdersData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {

        return workshopCutService.getCutOrdersPaged(page, size, search, status, sortField, sortDir);
    }

    @GetMapping("/receipts")
    public String receiptsPage(Model model) {
        populateReceiptsPage(model);
        return "erp/workshop/receipts";
    }

    @GetMapping("/create")
    public String showCreateForm(Locale locale, Model model) {
        populateCutForm(model, locale);
        return "erp/workshop/cut-order-form";
    }

    @PostMapping("/create")
    @PreAuthorize(Constants.PRE_AUTH_WORKSHOP_WRITE)
    public String createCutOrder(@RequestParam(value = "projectId", required = false) Long projectId,
                                 @RequestParam(value = "locationId", required = false) Long locationId,
                                 @RequestParam("slabId") Long slabId,
                                 @RequestParam("machineName") String machineName,
                                 @RequestParam(value = "machineId", required = false) Long machineId,
                                 @RequestParam("operatorName") String operatorName,
                                 @RequestParam("piecesCount") int piecesCount,
                                 @RequestParam("targetWidthCm") BigDecimal targetWidthCm,
                                 @RequestParam("targetLengthCm") BigDecimal targetLengthCm,
                                 @RequestParam(value = "edgeFinish", required = false) String edgeFinish,
                                 @RequestParam(value = "targetLocationDesc", required = false) String targetLocationDesc,
                                 @RequestParam(value = "notes", required = false) String notes,
                                 @RequestParam(value = "purpose", required = false) WorkshopWorkPurpose purpose,
                                 Locale locale,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        try {
            var order = workshopCutService.createCutOrder(projectId, locationId, slabId, machineName, operatorName,
                    piecesCount, targetWidthCm, targetLengthCm, edgeFinish, targetLocationDesc, notes);
            workshopOperationService.assignPurposeAndMachine(order.getId(), purpose, machineId);

            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.workshop.cut.success", null, locale));
            return "redirect:/workshop";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            populateCutForm(model, locale);
            return "erp/workshop/cut-order-form";
        }
    }

    @GetMapping("/{id}")
    public String cutOrderDetail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("order", workshopCutService.getCutOrderWithDetails(id));
        model.addAttribute("operations", workshopOperationService.operationsFor(id));
        model.addAttribute("orderCost", workshopOperationService.orderCost(id));
        model.addAttribute("processTypes", WorkshopProcessType.values());
        model.addAttribute("workshopMachines", workshopOperationService.workshopMachines());
        return "erp/workshop/detail";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Locale locale, Model model) {
        populateCutEditForm(model, locale, workshopCutService.getCutOrderWithDetails(id));
        return "erp/workshop/cut-order-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize(Constants.PRE_AUTH_WORKSHOP_WRITE)
    public String updateCutOrder(@PathVariable("id") Long id,
                                 @RequestParam(value = "projectId", required = false) Long projectId,
                                 @RequestParam("machineName") String machineName,
                                 @RequestParam(value = "machineId", required = false) Long machineId,
                                 @RequestParam("operatorName") String operatorName,
                                 @RequestParam(value = "edgeFinish", required = false) String edgeFinish,
                                 @RequestParam(value = "targetLocationDesc", required = false) String targetLocationDesc,
                                 @RequestParam(value = "notes", required = false) String notes,
                                 @RequestParam(value = "purpose", required = false) WorkshopWorkPurpose purpose,
                                 Locale locale,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            workshopCutService.updateCutOrder(id, projectId, machineName, operatorName,
                    edgeFinish, targetLocationDesc, notes);
            workshopOperationService.assignPurposeAndMachine(id, purpose, machineId);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.workshop.update.success", null, locale));
            return "redirect:/workshop";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            populateCutEditForm(model, locale, workshopCutService.getCutOrderWithDetails(id));
            return "erp/workshop/cut-order-form";
        }
    }

    @PostMapping("/receipts")
    @PreAuthorize(Constants.PRE_AUTH_WORKSHOP_WRITE)
    public String receiveMaterial(@RequestParam("source") WorkshopReceiptSource source,
                                  @RequestParam(value = "supplierId", required = false) Long supplierId,
                                  @RequestParam(value = "purchaseOrderItemId", required = false) Long purchaseOrderItemId,
                                  @RequestParam("stoneType") String stoneType,
                                  @RequestParam(value = "quantity", required = false) BigDecimal quantity,
                                  @RequestParam(value = "areaM2", required = false) BigDecimal areaM2,
                                  @RequestParam(value = "purchaseCost", required = false) BigDecimal purchaseCost,
                                  @RequestParam(value = "receivedAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receivedAt,
                                  @RequestParam(value = "notes", required = false) String notes,
                                  Locale locale,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            workshopOperationService.receiveMaterial(source, supplierId, purchaseOrderItemId, stoneType,
                    quantity, areaM2, purchaseCost, receivedAt, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.workshop.receipt.success", null, locale));
            return "redirect:/workshop/receipts";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            populateReceiptsPage(model);
            return "erp/workshop/receipts";
        }
    }

    @PostMapping("/{id}/operations")
    @PreAuthorize(Constants.PRE_AUTH_WORKSHOP_WRITE)
    public String recordOperation(@PathVariable("id") Long id,
                                  @RequestParam("processType") WorkshopProcessType processType,
                                  @RequestParam(value = "machineId", required = false) Long machineId,
                                  @RequestParam("operatorName") String operatorName,
                                  @RequestParam(value = "laborHours", required = false) BigDecimal laborHours,
                                  @RequestParam("inputAreaM2") BigDecimal inputAreaM2,
                                  @RequestParam("outputAreaM2") BigDecimal outputAreaM2,
                                  @RequestParam("wasteAreaM2") BigDecimal wasteAreaM2,
                                  @RequestParam(value = "extraExpense", required = false) BigDecimal extraExpense,
                                  @RequestParam(value = "notes", required = false) String notes,
                                  Locale locale,
                                  RedirectAttributes redirectAttributes) {
        workshopOperationService.recordOperation(id, processType, machineId, operatorName, laborHours,
                inputAreaM2, outputAreaM2, wasteAreaM2, extraExpense, notes);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.workshop.operation.success", null, locale));
        return "redirect:/workshop/" + id;
    }

    private void populateCutForm(Model model, Locale locale) {
        model.addAttribute("availableSlabs", workshopCutService.getAvailableSlabs());
        model.addAttribute("projects", workshopCutService.getAllProjects());
        model.addAttribute("workshopMachines", workshopOperationService.workshopMachines());
        model.addAttribute("purposes", WorkshopWorkPurpose.values());
        model.addAttribute("pageTitle", messageSource.getMessage("erp.workshop.title.create", null, locale));
    }

    private void populateCutEditForm(Model model, Locale locale, CutOrder order) {
        populateCutForm(model, locale);
        model.addAttribute("record", order);
        model.addAttribute("isEdit", true);
        model.addAttribute("pageTitle",
                messageSource.getMessage("erp.workshop.title.edit", null, locale) + ": " + order.getCutOrderNo());
    }

    private void populateReceiptsPage(Model model) {
        model.addAttribute("receipts", workshopOperationService.listReceipts());
        model.addAttribute("receiptSources", WorkshopReceiptSource.values());
        model.addAttribute("suppliers", workshopOperationService.suppliers());
    }
}
