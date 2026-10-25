package com.ozerler.marble.service;

import com.ozerler.marble.dto.CariSummaryDto;
import com.ozerler.marble.dto.CariTransactionDto;
import com.ozerler.marble.model.CariTransaction;
import com.ozerler.marble.model.CollectionRecord;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.enums.CariTransactionType;
import com.ozerler.marble.model.enums.InvoiceType;
import com.ozerler.marble.repository.CariTransactionRepository;
import com.ozerler.marble.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CariTransactionService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final CariTransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<CariTransactionDto> getTransactionsForCustomer(Long customerId) {
        List<CariTransaction> rawList = transactionRepository.findByCustomerIdOrderByTransactionDateAscIdAsc(customerId);

        BigDecimal runningBalance = BigDecimal.ZERO;
        List<CariTransactionDto> dtoList = new ArrayList<>();

        for (CariTransaction t : rawList) {
            BigDecimal debit = t.getDebitAmount() != null ? t.getDebitAmount() : BigDecimal.ZERO;
            BigDecimal credit = t.getCreditAmount() != null ? t.getCreditAmount() : BigDecimal.ZERO;
            runningBalance = runningBalance.add(debit).subtract(credit);

            CariTransactionDto dto = CariTransactionDto.builder()
                    .id(t.getId())
                    .customerId(t.getCustomer().getId())
                    .customerCode(t.getCustomer().getCustomerCode())
                    .customerName(t.getCustomer().getCompanyName())
                    .transactionDate(t.getTransactionDate())
                    .formattedDate(t.getTransactionDate() != null ? t.getTransactionDate().format(DATE_FORMATTER) : "—")
                    .transactionType(t.getTransactionType())
                    .transactionTypeLabel(t.getTransactionType() != null ? t.getTransactionType().getLabel() : "İşlem")
                    .debit(t.getTransactionType() != null && t.getTransactionType().isDefaultDebit())
                    .documentNo(t.getDocumentNo())
                    .description(normalizeDescription(t.getDescription()))
                    .debitAmount(debit)
                    .creditAmount(credit)
                    .runningBalance(runningBalance)
                    .invoiceId(t.getInvoice() != null ? t.getInvoice().getId() : null)
                    .collectionId(t.getCollection() != null ? t.getCollection().getId() : null)
                    .notes(t.getNotes())
                    .build();

            dtoList.add(dto);
        }

        // Return sorted DESC (most recent first) for standard viewing, but keep runningBalance accurate
        Collections.reverse(dtoList);
        return dtoList;
    }

    @Transactional(readOnly = true)
    public CariSummaryDto getCustomerSummary(Long customerId) {
        List<Object[]> totals = transactionRepository.getDebitCreditTotalsByCustomerId(customerId);
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        if (totals != null && !totals.isEmpty() && totals.get(0) != null) {
            Object[] row = totals.get(0);
            if (row.length >= 2) {
                totalDebit = row[0] != null ? (BigDecimal) row[0] : BigDecimal.ZERO;
                totalCredit = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            }
        }

        BigDecimal netBalance = totalDebit.subtract(totalCredit);
        String status = "SIFIR";
        if (netBalance.compareTo(new BigDecimal("0.009")) > 0) {
            status = "BORÇLU (B)";
        } else if (netBalance.compareTo(new BigDecimal("-0.009")) < 0) {
            status = "ALACAKLI (A)";
        }

        List<CariTransaction> list = transactionRepository.findByCustomerIdOrderByTransactionDateAscIdAsc(customerId);

        return CariSummaryDto.builder()
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .netBalance(netBalance)
                .balanceStatus(status)
                .transactionCount(list.size())
                .build();
    }

    @Transactional
    public CariTransaction createTransaction(Long customerId, LocalDate date, CariTransactionType type,
                                             String documentNo, String description, BigDecimal debitAmount,
                                             BigDecimal creditAmount, String notes) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Cari bulunamadı: " + customerId));

        BigDecimal finalDebit = debitAmount != null && debitAmount.compareTo(BigDecimal.ZERO) > 0 ? debitAmount : BigDecimal.ZERO;
        BigDecimal finalCredit = creditAmount != null && creditAmount.compareTo(BigDecimal.ZERO) > 0 ? creditAmount : BigDecimal.ZERO;

        CariTransaction tx = CariTransaction.builder()
                .customer(customer)
                .transactionDate(date != null ? date : LocalDate.now())
                .transactionType(type != null ? type : CariTransactionType.BORC_DEKONTU)
                .documentNo(documentNo != null ? documentNo.trim() : null)
                .description(description != null ? description.trim() : (type != null ? type.getLabel() : "Cari Hareket"))
                .debitAmount(finalDebit)
                .creditAmount(finalCredit)
                .notes(notes != null ? notes.trim() : null)
                .build();

        return transactionRepository.save(tx);
    }

    @Transactional
    public CariTransaction updateTransaction(Long transactionId, LocalDate date, CariTransactionType type,
                                             String documentNo, String description, BigDecimal debitAmount,
                                             BigDecimal creditAmount, String notes) {
        CariTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Cari hareket bulunamadı: " + transactionId));

        if (date != null) tx.setTransactionDate(date);
        if (type != null) tx.setTransactionType(type);
        if (documentNo != null) tx.setDocumentNo(documentNo.trim());
        if (description != null) tx.setDescription(description.trim());

        if (debitAmount != null) tx.setDebitAmount(debitAmount.compareTo(BigDecimal.ZERO) > 0 ? debitAmount : BigDecimal.ZERO);
        if (creditAmount != null) tx.setCreditAmount(creditAmount.compareTo(BigDecimal.ZERO) > 0 ? creditAmount : BigDecimal.ZERO);
        if (notes != null) tx.setNotes(notes.trim());

        return transactionRepository.save(tx);
    }

    @Transactional
    public void deleteTransaction(Long transactionId) {
        CariTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Cari hareket bulunamadı: " + transactionId));
        transactionRepository.delete(tx);
    }

    @Transactional(readOnly = true)
    public CariTransaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cari hareket bulunamadı: " + id));
    }

    @Transactional
    public void syncInvoice(Invoice invoice) {
        if (invoice == null || invoice.getCustomer() == null) return;

        Optional<CariTransaction> opt = transactionRepository.findByInvoiceId(invoice.getId());
        CariTransactionType type = invoice.getInvoiceType() == InvoiceType.SALES ? CariTransactionType.SATIS_FATURASI : CariTransactionType.ALIS_FATURASI;
        BigDecimal total = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;

        CariTransaction tx = opt.orElseGet(() -> CariTransaction.builder()
                .customer(invoice.getCustomer())
                .invoice(invoice)
                .build());

        tx.setCustomer(invoice.getCustomer());
        tx.setTransactionDate(invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now());
        tx.setTransactionType(type);
        tx.setDocumentNo(invoice.getInvoiceNo());
        tx.setDescription((invoice.getInvoiceType() == InvoiceType.SALES ? "Satış Faturası: " : "Alış Faturası: ") + invoice.getInvoiceNo());
        tx.setDebitAmount(type == CariTransactionType.SATIS_FATURASI ? total : BigDecimal.ZERO);
        tx.setCreditAmount(type == CariTransactionType.ALIS_FATURASI ? total : BigDecimal.ZERO);
        tx.setNotes(invoice.getNotes());

        transactionRepository.save(tx);
    }

    @Transactional
    public void removeInvoiceSync(Long invoiceId) {
        if (invoiceId == null) return;
        transactionRepository.findByInvoiceId(invoiceId).ifPresent(transactionRepository::delete);
    }

    @Transactional
    public void syncCollection(CollectionRecord collection) {
        if (collection == null || collection.getCustomer() == null) return;

        Optional<CariTransaction> opt = transactionRepository.findByCollectionId(collection.getId());
        BigDecimal amount = collection.getAmount() != null ? collection.getAmount() : BigDecimal.ZERO;

        CariTransaction tx = opt.orElseGet(() -> CariTransaction.builder()
                .customer(collection.getCustomer())
                .collection(collection)
                .build());

        tx.setCustomer(collection.getCustomer());
        tx.setTransactionDate(collection.getCollectionDate() != null ? collection.getCollectionDate() : LocalDate.now());
        tx.setTransactionType(CariTransactionType.TAHSILAT);
        tx.setDocumentNo(collection.getCollectionNo());
        tx.setDescription("Tahsilat (" + (collection.getCollectionMethod() != null ? collection.getCollectionMethod().getDisplayName() : "Nakit") + "): " + collection.getCollectionNo());
        tx.setDebitAmount(BigDecimal.ZERO);
        tx.setCreditAmount(amount);
        tx.setNotes(collection.getNotes());

        transactionRepository.save(tx);
    }

    private String normalizeDescription(String desc) {
        if (desc == null || desc.isBlank()) {
            return desc;
        }
        return desc
                .replace("(CASH)", "(Nakit)")
                .replace("(BANK_TRANSFER)", "(EFT / Havale)")
                .replace("(CREDIT_CARD)", "(Kredi Kartı)")
                .replace("(CHECK)", "(Çek)")
                .replace("(enum.collection_method.cash)", "(Nakit)")
                .replace("(enum.collection_method.bank_transfer)", "(EFT / Havale)")
                .replace("(enum.collection_method.credit_card)", "(Kredi Kartı)")
                .replace("(enum.collection_method.check)", "(Çek)");
    }
}
