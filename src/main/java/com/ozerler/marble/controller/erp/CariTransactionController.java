package com.ozerler.marble.controller.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.CariSummaryDto;
import com.ozerler.marble.dto.CariTransactionDto;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CariTransactionType;
import com.ozerler.marble.model.enums.CustomerType;
import com.ozerler.marble.service.CariTransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class CariTransactionController extends AbstractController {

    private final com.ozerler.marble.service.FormLookupService formLookup;

    private final CariTransactionService cariTransactionService;

    @GetMapping("/admin/definitions/customers/{customerId}/transactions")
    public String viewTransactions(@PathVariable("customerId") Long customerId, Model model) {
        var customer = formLookup.getCustomer(customerId);

        List<CariTransactionDto> transactions = cariTransactionService.getTransactionsForCustomer(customerId);
        CariSummaryDto summary = cariTransactionService.getCustomerSummary(customerId);
        var projects = formLookup.projectsForCustomer(customerId);

        model.addAttribute("customer", customer);
        model.addAttribute("customerTypeLabel", labelOf(customer.getCustomerType()));
        model.addAttribute("businessUnitLabel", labelOf(customer.getBusinessUnit()));
        model.addAttribute("transactions", transactions);
        model.addAttribute("summary", summary);
        model.addAttribute("projects", projects);
        model.addAttribute("transactionTypes", CariTransactionType.values());
        model.addAttribute("activeNav", "accounts");

        return "admin/definitions/customer-transactions";
    }

    @GetMapping("/accounts/{customerId}/transactions")
    public String viewAccountTransactionsRedirect(@PathVariable("customerId") Long customerId) {
        return "redirect:/admin/definitions/customers/" + customerId + "/transactions";
    }

    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    @PostMapping("/admin/definitions/customers/{customerId}/transactions")
    public String addTransaction(@PathVariable("customerId") Long customerId,
                                 @RequestParam(value = "transactionDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate transactionDate,
                                 @RequestParam("transactionType") CariTransactionType transactionType,
                                 @RequestParam(value = "documentNo", required = false) String documentNo,
                                 @RequestParam(value = "direction", defaultValue = "DEBIT") String direction,
                                 @RequestParam("amount") BigDecimal amount,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "notes", required = false) String notes,
                                 RedirectAttributes redirectAttributes) {
        try {
            CariAmounts amounts = CariAmounts.of(direction, amount);

            cariTransactionService.createTransaction(
                    customerId, transactionDate != null ? transactionDate : LocalDate.now(),
                    transactionType, documentNo, description, amounts.debit(), amounts.credit(), notes
            );

            redirectAttributes.addFlashAttribute("successMessage", "Cari hareket başarıyla kaydedildi (" + transactionType.getLabel() + ": " + amount + " TL)");
        } catch (Exception e) {
            log.error("Cari hareket eklenemedi", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
        }
        return "redirect:/admin/definitions/customers/" + customerId + "/transactions";
    }

    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    @PostMapping("/admin/definitions/customers/{customerId}/transactions/{transactionId}/edit")
    public String editTransaction(@PathVariable("customerId") Long customerId,
                                  @PathVariable("transactionId") Long transactionId,
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false, name = "transactionDate") LocalDate transactionDate,
                                  @RequestParam(name = "transactionType") CariTransactionType transactionType,
                                  @RequestParam(required = false, name = "documentNo") String documentNo,
                                  @RequestParam(name = "direction", defaultValue = "DEBIT") String direction,
                                  @RequestParam(name = "amount") BigDecimal amount,
                                  @RequestParam(required = false, name = "description") String description,
                                  @RequestParam(required = false, name = "notes") String notes,
                                  RedirectAttributes redirectAttributes) {
        try {
            CariAmounts amounts = CariAmounts.of(direction, amount);

            cariTransactionService.updateTransaction(
                    transactionId, transactionDate, transactionType, documentNo, description,
                    amounts.debit(), amounts.credit(), notes
            );

            redirectAttributes.addFlashAttribute("successMessage", "Cari hareket başarıyla güncellendi.");
        } catch (Exception e) {
            log.error("Cari hareket güncellenemedi", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
        }
        return "redirect:/admin/definitions/customers/" + customerId + "/transactions";
    }

    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    @PostMapping("/admin/definitions/customers/{customerId}/transactions/{transactionId}/delete")
    public String deleteTransaction(@PathVariable("customerId") Long customerId,
                                    @PathVariable("transactionId") Long transactionId,
                                    RedirectAttributes redirectAttributes) {
        try {
            cariTransactionService.deleteTransaction(transactionId);
            redirectAttributes.addFlashAttribute("successMessage", "Cari hareket silindi.");
        } catch (Exception e) {
            log.error("Cari hareket silinemedi", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
        }
        return "redirect:/admin/definitions/customers/" + customerId + "/transactions";
    }

    private record CariAmounts(BigDecimal debit, BigDecimal credit) {
        private static CariAmounts of(String direction, BigDecimal amount) {
            BigDecimal debitAmount = "DEBIT".equalsIgnoreCase(direction) ? amount : BigDecimal.ZERO;
            BigDecimal creditAmount = "CREDIT".equalsIgnoreCase(direction) ? amount : BigDecimal.ZERO;
            return new CariAmounts(debitAmount, creditAmount);
        }
    }

    private static String labelOf(CustomerType customerType) {
        return customerType == null ? "Genel" : customerType.getLabel();
    }

    private static String labelOf(BusinessUnit businessUnit) {
        return businessUnit == null ? "Genel" : businessUnit.getLabel();
    }
}
