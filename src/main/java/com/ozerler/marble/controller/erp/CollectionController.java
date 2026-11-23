package com.ozerler.marble.controller.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.view.CheckRef;
import com.ozerler.marble.dto.view.CollectionRef;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CheckStatus;
import com.ozerler.marble.model.enums.CollectionMethod;
import com.ozerler.marble.service.CollectionService;
import com.ozerler.marble.service.FormLookupService;
import com.ozerler.marble.web.CheckGrid;
import com.ozerler.marble.web.CollectionGrid;
import com.ozerler.marble.web.ListQuery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/collections")
@RequiredArgsConstructor
@Slf4j
public class CollectionController extends AbstractController {

    private final FormLookupService formLookup;

    private final CollectionService collectionService;

    @GetMapping
    public String index(@RequestParam(value = "method", required = false) CollectionMethod method,
                        @RequestParam(value = "customerId", required = false) Long customerId,
                        @ModelAttribute ListQuery query, Model model) {

        BigDecimal monthlyTotal = collectionService.getMonthlyTotal();
        long approachingChecksCount = collectionService.getApproachingChecksCount();

        model.addAttribute(
                "grid",
                CollectionGrid.view(
                        collectionService.searchCollectionViews(
                                method,
                                customerId,
                                null,
                                null,
                                query.getSearch(),
                                query.pageNumber() - 1,
                                query.pageSize()),
                        query.pageNumber(),
                        query.pageSize(),
                        query.sortKey(),
                        query.sortDirection()));
        Object valueValue;
        if (method != null) {
            valueValue = method.name();
        } else {
            valueValue = "";
        }
        model.addAttribute("selectedMethod", valueValue);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("customers", formLookup.customers());
        model.addAttribute("methods", CollectionMethod.values());
        model.addAttribute("monthlyTotal", monthlyTotal);
        model.addAttribute("approachingChecksCount", approachingChecksCount);
        model.addAttribute("activeNav", "collections");

        return "collections/list";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<Map<String, Object>> getCollectionsData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "method", required = false) CollectionMethod method,
            @RequestParam(value = "customerId", required = false) Long customerId) {

        return collectionService.collectionRows(search, method, customerId, page, size);
    }

    @GetMapping({"/new", "/create"})
    public String newCollectionForm(@RequestParam(value = "invoiceId", required = false) Long invoiceId,
                                    @RequestParam(value = "customerId", required = false) Long customerId,
                                    Model model) {

        var invoice = formLookup.findInvoiceWithCustomer(invoiceId);
        if (invoice != null && invoice.getCustomer() != null && customerId == null) {
            customerId = invoice.getCustomer().getId();
        }

        model.addAttribute("selectedInvoice", invoice);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("customers", formLookup.customers());
        model.addAttribute("invoices", formLookup.invoicesWithCustomer());
        model.addAttribute("methods", CollectionMethod.values());
        model.addAttribute("activeNav", "collections");

        return "collections/form";
    }

    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    @PostMapping({"/new", "/create"})
    public String createCollection(@RequestParam("customerId") Long customerId,
                                   @RequestParam(value = "invoiceId", required = false) Long invoiceId,
                                   @RequestParam("method") CollectionMethod method,
                                   @RequestParam("amount") BigDecimal amount,
                                   @RequestParam(value = "bankName", required = false) String bankName,
                                   @RequestParam(value = "notes", required = false) String notes,
                                   @RequestParam(value = "checkNo", required = false) String checkNo,
                                   @RequestParam(value = "checkDate", required = false) LocalDate checkDate,
                                   @RequestParam(value = "dueDate", required = false) LocalDate dueDate,
                                   RedirectAttributes redirectAttributes) {

        try {
            CollectionService.CheckFormData checkData = null;
            if (method == CollectionMethod.CHECK) {
                checkData = new CollectionService.CheckFormData(checkNo, checkDate, dueDate, bankName);
            }

            CollectionRef collection = collectionService.recordCollectionRef(
                    customerId, invoiceId, method, amount, bankName, notes, checkData
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Tahsilat ("
                            + collection.getCollectionNo()
                            + ") başarıyla kaydedildi: "
                            + amount
                            + " TL ("
                            + method.getLabel()
                            + ")");
            return "redirect:/collections";
        } catch (Exception e) {
            log.error("Tahsilat kaydedilemedi", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Tahsilat hatası: " + e.getMessage());
            return "redirect:/collections/new";
        }
    }

    @GetMapping("/checks")
    public String checksTracker(@RequestParam(value = "unit", required = false) BusinessUnit unit,
                                @RequestParam(value = "status", required = false) CheckStatus status,
                                @RequestParam(value = "customerId", required = false) Long customerId,
                                @ModelAttribute ListQuery query, Model model) {

        Page<CheckRef> checkPage = collectionService.searchCheckViews(
                unit, status, customerId, null, null, query.getSearch(), query.pageNumber() - 1, query.pageSize());
        List<CheckRef> approaching = collectionService.getApproachingCheckViews(unit);
        List<CheckRef> overdue = collectionService.getOverdueCheckViews(unit);

        model.addAttribute("checks", checkPage.getContent());
        model.addAttribute("page", checkPage);
        model.addAttribute("approachingChecks", approaching);
        model.addAttribute("overdueChecks", overdue);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCustomerId", customerId);
        Object valueValue;
        if (unit != null) {
            valueValue = unit.name();
        } else {
            valueValue = "";
        }
        model.addAttribute("selectedUnit", valueValue);
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("customers", formLookup.customers());
        model.addAttribute("checkStatuses", CheckStatus.values());
        model.addAttribute("grid", CheckGrid.view(
                checkPage, query.pageNumber(), query.pageSize(), query.sortKey(), query.sortDirection()));
        model.addAttribute("activeNav", "collections");

        return "collections/checks";
    }

    @GetMapping("/checks/api/data")
    @ResponseBody
    public TabulatorResponse<Map<String, Object>> getChecksData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "unit", required = false) BusinessUnit unit,
            @RequestParam(value = "status", required = false) CheckStatus status,
            @RequestParam(value = "customerId", required = false) Long customerId) {

        return collectionService.checkRows(search, unit, status, customerId, page, size);
    }

    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    @PostMapping("/checks/{id}/status")
    public String updateCheckStatus(@PathVariable("id") Long id,
                                    @RequestParam("status") CheckStatus status,
                                    @RequestParam(value = "notes", required = false) String notes,
                                    @RequestParam(value = "unit", required = false) BusinessUnit unit,
                                    RedirectAttributes redirectAttributes) {
        try {
            CheckRef check = collectionService.updateCheckStatusRef(id, status, notes);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Çek (" + check.getCheckNo() + ") durumu güncellendi: " + status.getLabel());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
        }
        String valueValue;
        if (unit != null) {
            valueValue = "?unit=" + unit.name();
        } else {
            valueValue = "";
        }
        return "redirect:/collections/checks" + valueValue;
    }
}
