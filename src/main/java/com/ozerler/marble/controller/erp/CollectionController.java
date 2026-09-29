package com.ozerler.marble.controller.erp;

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.CheckStatus;
import com.ozerler.marble.model.enums.CollectionMethod;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.InvoiceRepository;
import com.ozerler.marble.service.CollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/collections")
@RequiredArgsConstructor
@Slf4j
public class CollectionController extends AbstractController {

    private final CollectionService collectionService;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;

    @GetMapping
    public String index(@RequestParam(value = "method", required = false) CollectionMethod method,
                        @RequestParam(value = "customerId", required = false) Long customerId,
                        @RequestParam(value = "search", required = false) String search,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "20") int size,
                        Model model) {

        Page<CollectionRecord> collectionPage = collectionService.searchCollections(method, customerId, null, null, search, page, size);
        BigDecimal monthlyTotal = collectionService.getMonthlyTotal();
        long approachingChecksCount = collectionService.getApproachingChecksCount();

        model.addAttribute("collections", collectionPage.getContent());
        model.addAttribute("page", collectionPage);
        model.addAttribute("selectedMethod", method);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("methods", CollectionMethod.values());
        model.addAttribute("monthlyTotal", monthlyTotal);
        model.addAttribute("approachingChecksCount", approachingChecksCount);
        model.addAttribute("activeNav", "collections");

        return "collections/list";
    }

    @GetMapping({"/new", "/create"})
    public String newCollectionForm(@RequestParam(value = "invoiceId", required = false) Long invoiceId,
                                    @RequestParam(value = "customerId", required = false) Long customerId,
                                    Model model) {

        Invoice invoice = invoiceId != null ? invoiceRepository.findById(invoiceId).orElse(null) : null;
        if (invoice != null && invoice.getCustomer() != null && customerId == null) {
            customerId = invoice.getCustomer().getId();
        }

        model.addAttribute("selectedInvoice", invoice);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("invoices", invoiceRepository.findAll());
        model.addAttribute("methods", CollectionMethod.values());
        model.addAttribute("activeNav", "collections");

        return "collections/form";
    }

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

            CollectionRecord collection = collectionService.recordCollection(
                    customerId, invoiceId, method, amount, bankName, notes, checkData
            );

            redirectAttributes.addFlashAttribute("successMessage", "Tahsilat (" + collection.getCollectionNo() + ") başarıyla kaydedildi: " + amount + " TL (" + method.getLabel() + ")");
            return "redirect:/collections";
        } catch (Exception e) {
            log.error("Tahsilat kaydedilemedi", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Tahsilat hatası: " + e.getMessage());
            return "redirect:/collections/new";
        }
    }

    @GetMapping("/checks")
    public String checksTracker(@RequestParam(value = "status", required = false) CheckStatus status,
                                @RequestParam(value = "customerId", required = false) Long customerId,
                                @RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "20") int size,
                                Model model) {

        Page<CheckRecord> checkPage = collectionService.searchChecks(status, customerId, null, null, search, page, size);
        List<CheckRecord> approaching = collectionService.getApproachingChecks();
        List<CheckRecord> overdue = collectionService.getOverdueChecks();

        model.addAttribute("checks", checkPage.getContent());
        model.addAttribute("page", checkPage);
        model.addAttribute("approachingChecks", approaching);
        model.addAttribute("overdueChecks", overdue);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("checkStatuses", CheckStatus.values());
        model.addAttribute("activeNav", "collections");

        return "collections/checks";
    }

    @PostMapping("/checks/{id}/status")
    public String updateCheckStatus(@PathVariable("id") Long id,
                                    @RequestParam("status") CheckStatus status,
                                    @RequestParam(value = "notes", required = false) String notes,
                                    RedirectAttributes redirectAttributes) {
        try {
            CheckRecord check = collectionService.updateCheckStatus(id, status, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Çek (" + check.getCheckNo() + ") durumu güncellendi: " + status.getLabel());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
        }
        return "redirect:/collections/checks";
    }
}
