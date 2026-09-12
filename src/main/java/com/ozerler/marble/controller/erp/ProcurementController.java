package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.PurchaseOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.PurchaseOrder;
import com.ozerler.marble.model.enums.PurchaseOrderStatus;
import com.ozerler.marble.service.ProcurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

@Controller
@RequestMapping("/procurement")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;

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
    public String showCreateForm(Model model) {
        populateProcurementForm(model);
        return "erp/procurement/form";
    }

    @PostMapping("/create")
    public String createOrder(@RequestParam("poNumber") String poNumber,
                              @RequestParam("supplierId") Long supplierId,
                              @RequestParam(value = "projectId", required = false) Long projectId,
                              @RequestParam(value = "expectedDelivery", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedDelivery,
                              @RequestParam(value = "notes", required = false) String notes,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            PurchaseOrder order = procurementService.createPurchaseOrder(poNumber, supplierId, projectId, expectedDelivery, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Satın alma siparişi " + order.getPoNumber() + " başarıyla oluşturuldu.");
            return "redirect:/procurement";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Hata: " + e.getMessage());
            populateProcurementForm(model);
            return "erp/procurement/form";
        }
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

    private void populateProcurementForm(Model model) {
        model.addAttribute("suppliers", procurementService.getAllSuppliers());
        model.addAttribute("projects", procurementService.getAllProjects());
        model.addAttribute("generatedPoNumber", procurementService.generatePoNumber());
        model.addAttribute("pageTitle", "Yeni Satın Alma Siparişi");
    }
}
