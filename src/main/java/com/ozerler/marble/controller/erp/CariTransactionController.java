package com.ozerler.marble.controller.erp;

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.CariSummaryDto;
import com.ozerler.marble.dto.CariTransactionDto;
import com.ozerler.marble.model.CariTransaction;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.enums.CariTransactionType;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.service.CariTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class CariTransactionController extends AbstractController {

    private final CariTransactionService cariTransactionService;
    private final CustomerRepository customerRepository;

    @GetMapping("/admin/definitions/customers/{customerId}/transactions")
    public String viewTransactions(@PathVariable("customerId") Long customerId, Model model) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Cari kart bulunamadı: " + customerId));

        List<CariTransactionDto> transactions = cariTransactionService.getTransactionsForCustomer(customerId);
        CariSummaryDto summary = cariTransactionService.getCustomerSummary(customerId);

        model.addAttribute("customer", customer);
        model.addAttribute("transactions", transactions);
        model.addAttribute("summary", summary);
        model.addAttribute("transactionTypes", CariTransactionType.values());
        model.addAttribute("activeNav", "accounts");

        return "admin/definitions/customer-transactions";
    }

    @GetMapping("/accounts/{customerId}/transactions")
    public String viewAccountTransactionsRedirect(@PathVariable("customerId") Long customerId) {
        return "redirect:/admin/definitions/customers/" + customerId + "/transactions";
    }

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
            BigDecimal debitAmount = "DEBIT".equalsIgnoreCase(direction) ? amount : BigDecimal.ZERO;
            BigDecimal creditAmount = "CREDIT".equalsIgnoreCase(direction) ? amount : BigDecimal.ZERO;

            CariTransaction tx = cariTransactionService.createTransaction(
                    customerId, transactionDate != null ? transactionDate : LocalDate.now(),
                    transactionType, documentNo, description, debitAmount, creditAmount, notes
            );

            redirectAttributes.addFlashAttribute("successMessage", "Cari hareket başarıyla kaydedildi (" + tx.getTransactionType().getLabel() + ": " + amount + " TL)");
        } catch (Exception e) {
            log.error("Cari hareket eklenemedi", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
        }
        return "redirect:/admin/definitions/customers/" + customerId + "/transactions";
    }

    @PostMapping("/admin/definitions/customers/{customerId}/transactions/{transactionId}/edit")
    public String editTransaction(@PathVariable("customerId") Long customerId,
                                  @PathVariable("transactionId") Long transactionId,
                                  @RequestParam(value = "transactionDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate transactionDate,
                                  @RequestParam("transactionType") CariTransactionType transactionType,
                                  @RequestParam(value = "documentNo", required = false) String documentNo,
                                  @RequestParam(value = "direction", defaultValue = "DEBIT") String direction,
                                  @RequestParam("amount") BigDecimal amount,
                                  @RequestParam(value = "description", required = false) String description,
                                  @RequestParam(value = "notes", required = false) String notes,
                                  RedirectAttributes redirectAttributes) {
        try {
            BigDecimal debitAmount = "DEBIT".equalsIgnoreCase(direction) ? amount : BigDecimal.ZERO;
            BigDecimal creditAmount = "CREDIT".equalsIgnoreCase(direction) ? amount : BigDecimal.ZERO;

            cariTransactionService.updateTransaction(
                    transactionId, transactionDate, transactionType, documentNo, description,
                    debitAmount, creditAmount, notes
            );

            redirectAttributes.addFlashAttribute("successMessage", "Cari hareket başarıyla güncellendi.");
        } catch (Exception e) {
            log.error("Cari hareket güncellenemedi", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
        }
        return "redirect:/admin/definitions/customers/" + customerId + "/transactions";
    }

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
}
