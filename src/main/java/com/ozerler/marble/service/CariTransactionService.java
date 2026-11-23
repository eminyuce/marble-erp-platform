package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            BigDecimal debit;
            if (t.getDebitAmount() != null) {
                debit = t.getDebitAmount();
            } else {
                debit = BigDecimal.ZERO;
            }
            BigDecimal credit;
            if (t.getCreditAmount() != null) {
                credit = t.getCreditAmount();
            } else {
                credit = BigDecimal.ZERO;
            }
            runningBalance = runningBalance.add(debit).subtract(credit);

            String formattedDate;
            if (t.getTransactionDate() != null) {
                formattedDate = t.getTransactionDate().format(DATE_FORMATTER);
            } else {
                formattedDate = "—";
            }
            String transactionTypeLabel;
            if (t.getTransactionType() != null) {
                transactionTypeLabel = t.getTransactionType().getLabel();
            } else {
                transactionTypeLabel = "İşlem";
            }
            Long invoiceId;
            if (t.getInvoice() != null) {
                invoiceId = t.getInvoice().getId();
            } else {
                invoiceId = null;
            }
            Long collectionId;
            if (t.getCollection() != null) {
                collectionId = t.getCollection().getId();
            } else {
                collectionId = null;
            }
            CariTransactionDto dto = CariTransactionDto.builder()
                    .id(t.getId())
                    .customerId(t.getCustomer().getId())
                    .customerCode(t.getCustomer().getCustomerCode())
                    .customerName(t.getCustomer().getCompanyName())
                    .transactionDate(t.getTransactionDate())
                    .formattedDate(formattedDate)
                    .transactionType(t.getTransactionType())
                    .transactionTypeLabel(transactionTypeLabel)
                    .debit(t.getTransactionType() != null && t.getTransactionType().isDefaultDebit())
                    .documentNo(t.getDocumentNo())
                    .description(normalizeDescription(t.getDescription()))
                    .debitAmount(debit)
                    .creditAmount(credit)
                    .runningBalance(runningBalance)
                    .invoiceId(invoiceId)
                    .collectionId(collectionId)
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
                if (row[0] != null) {
                    totalDebit = (BigDecimal) row[0];
                } else {
                    totalDebit = BigDecimal.ZERO;
                }
                if (row[1] != null) {
                    totalCredit = (BigDecimal) row[1];
                } else {
                    totalCredit = BigDecimal.ZERO;
                }
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

        BigDecimal finalDebit;
        if (debitAmount != null && debitAmount.compareTo(BigDecimal.ZERO) > 0) {
            finalDebit = debitAmount;
        } else {
            finalDebit = BigDecimal.ZERO;
        }
        BigDecimal finalCredit;
        if (creditAmount != null && creditAmount.compareTo(BigDecimal.ZERO) > 0) {
            finalCredit = creditAmount;
        } else {
            finalCredit = BigDecimal.ZERO;
        }

        LocalDate transactionDate;
        if (date != null) {
            transactionDate = date;
        } else {
            transactionDate = LocalDate.now();
        }
        String documentNoValue;
        if (documentNo != null) {
            documentNoValue = documentNo.trim();
        } else {
            documentNoValue = null;
        }
        String descriptionValue;
        if (description != null) {
            descriptionValue = description.trim();
        } else {
            if (type != null) {
                descriptionValue = type.getLabel();
            } else {
                descriptionValue = "Cari Hareket";
            }
        }
        String notesValue;
        if (notes != null) {
            notesValue = notes.trim();
        } else {
            notesValue = null;
        }
        CariTransaction tx = CariTransaction.builder()
                .customer(customer)
                .transactionDate(transactionDate)
                .transactionType(Objects.requireNonNullElse(type, CariTransactionType.BORC_DEKONTU))
                .documentNo(documentNoValue)
                .description(descriptionValue)
                .debitAmount(finalDebit)
                .creditAmount(finalCredit)
                .notes(notesValue)
                .build();

        return transactionRepository.save(tx);
    }

    @Transactional
    public CariTransaction updateTransaction(Long transactionId, LocalDate date, CariTransactionType type,
                                             String documentNo, String description, BigDecimal debitAmount,
                                             BigDecimal creditAmount, String notes) {
        CariTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Cari hareket bulunamadı: " + transactionId));

        if (date != null) {
            tx.setTransactionDate(date);
        }
        if (type != null) {
            tx.setTransactionType(type);
        }
        if (documentNo != null) {
            tx.setDocumentNo(documentNo.trim());
        }
        if (description != null) {
            tx.setDescription(description.trim());
        }

        if (debitAmount != null) {
            BigDecimal debitAmountValue;
            if (debitAmount.compareTo(BigDecimal.ZERO) > 0) {
                debitAmountValue = debitAmount;
            } else {
                debitAmountValue = BigDecimal.ZERO;
            }
            tx.setDebitAmount(debitAmountValue);
        }
        if (creditAmount != null) {
            BigDecimal creditAmountValue;
            if (creditAmount.compareTo(BigDecimal.ZERO) > 0) {
                creditAmountValue = creditAmount;
            } else {
                creditAmountValue = BigDecimal.ZERO;
            }
            tx.setCreditAmount(creditAmountValue);
        }
        if (notes != null) {
            tx.setNotes(notes.trim());
        }

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
        if (invoice == null || invoice.getCustomer() == null) {
            return;
        }

        Optional<CariTransaction> opt = transactionRepository.findByInvoiceId(invoice.getId());
        CariTransactionType type;
        if (invoice.getInvoiceType() == InvoiceType.SALES) {
            type = CariTransactionType.SATIS_FATURASI;
        } else {
            type = CariTransactionType.ALIS_FATURASI;
        }
        BigDecimal total;
        if (invoice.getTotalAmount() != null) {
            total = invoice.getTotalAmount();
        } else {
            total = BigDecimal.ZERO;
        }

        CariTransaction tx = opt.orElseGet(() -> CariTransaction.builder()
                .customer(invoice.getCustomer())
                .invoice(invoice)
                .build());

        tx.setCustomer(invoice.getCustomer());
        LocalDate transactionDate;
        if (invoice.getInvoiceDate() != null) {
            transactionDate = invoice.getInvoiceDate();
        } else {
            transactionDate = LocalDate.now();
        }
        tx.setTransactionDate(transactionDate);
        tx.setTransactionType(type);
        tx.setDocumentNo(invoice.getInvoiceNo());
        String value;
        if (invoice.getInvoiceType() == InvoiceType.SALES) {
            value = "Satış Faturası: ";
        } else {
            value = "Alış Faturası: ";
        }
        tx.setDescription(value + invoice.getInvoiceNo());
        BigDecimal debitAmount;
        if (type == CariTransactionType.SATIS_FATURASI) {
            debitAmount = total;
        } else {
            debitAmount = BigDecimal.ZERO;
        }
        tx.setDebitAmount(debitAmount);
        BigDecimal creditAmount;
        if (type == CariTransactionType.ALIS_FATURASI) {
            creditAmount = total;
        } else {
            creditAmount = BigDecimal.ZERO;
        }
        tx.setCreditAmount(creditAmount);
        tx.setNotes(invoice.getNotes());

        transactionRepository.save(tx);
    }

    @Transactional
    public void removeInvoiceSync(Long invoiceId) {
        if (invoiceId == null) {
            return;
        }
        transactionRepository.findByInvoiceId(invoiceId).ifPresent(transactionRepository::delete);
    }

    @Transactional
    public void syncCollection(CollectionRecord collection) {
        if (collection == null || collection.getCustomer() == null) {
            return;
        }

        Optional<CariTransaction> opt = transactionRepository.findByCollectionId(collection.getId());
        BigDecimal amount;
        if (collection.getAmount() != null) {
            amount = collection.getAmount();
        } else {
            amount = BigDecimal.ZERO;
        }

        CariTransaction tx = opt.orElseGet(() -> CariTransaction.builder()
                .customer(collection.getCustomer())
                .collection(collection)
                .build());

        tx.setCustomer(collection.getCustomer());
        LocalDate transactionDate;
        if (collection.getCollectionDate() != null) {
            transactionDate = collection.getCollectionDate();
        } else {
            transactionDate = LocalDate.now();
        }
        tx.setTransactionDate(transactionDate);
        tx.setTransactionType(CariTransactionType.TAHSILAT);
        tx.setDocumentNo(collection.getCollectionNo());
        String displayName;
        if (collection.getCollectionMethod() != null) {
            displayName = collection.getCollectionMethod().getDisplayName();
        } else {
            displayName = "Nakit";
        }
        tx.setDescription("Tahsilat (" + displayName + "): " + collection.getCollectionNo());
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
