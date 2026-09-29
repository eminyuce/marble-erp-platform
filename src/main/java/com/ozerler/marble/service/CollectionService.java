package com.ozerler.marble.service;

import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.CheckStatus;
import com.ozerler.marble.model.enums.CollectionMethod;
import com.ozerler.marble.model.enums.InvoiceStatus;
import com.ozerler.marble.repository.*;
import com.ozerler.marble.util.UniqueCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

    private final CollectionRecordRepository collectionRepository;
    private final CheckRecordRepository checkRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;

    public record CheckFormData(String checkNo, LocalDate checkDate, LocalDate dueDate, String bankName) {}

    @Transactional
    public CollectionRecord recordCollection(Long customerId, Long invoiceId, CollectionMethod method,
                                            BigDecimal amount, String bankName, String notes,
                                            CheckFormData checkData) {

        Objects.requireNonNull(customerId, "Müşteri seçilmelidir.");
        Objects.requireNonNull(method, "Tahsilat yöntemi seçilmelidir.");
        Objects.requireNonNull(amount, "Tutar girilmelidir.");

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Tahsilat tutarı 0'dan büyük olmalıdır.");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Müşteri bulunamadı: " + customerId));

        Invoice invoice = invoiceId != null ? invoiceRepository.findById(invoiceId).orElse(null) : null;

        String code = UniqueCodes.yearly("THS", collectionRepository::existsByCollectionNo);

        CollectionRecord collection = CollectionRecord.builder()
                .collectionNo(code)
                .collectionDate(LocalDate.now())
                .customer(customer)
                .invoice(invoice)
                .collectionMethod(method)
                .amount(amount)
                .bankName(bankName)
                .notes(notes)
                .build();

        CollectionRecord savedCollection = collectionRepository.save(collection);

        if (method == CollectionMethod.CHECK) {
            Objects.requireNonNull(checkData, "Çek bilgileri girilmelidir.");
            Objects.requireNonNull(checkData.dueDate(), "Çek vade tarihi zorunludur.");
            Objects.requireNonNull(checkData.checkNo(), "Çek numarası zorunludur.");
            Objects.requireNonNull(checkData.bankName(), "Banka adı zorunludur.");

            CheckRecord check = CheckRecord.builder()
                    .collection(savedCollection)
                    .checkNo(checkData.checkNo().trim())
                    .checkDate(checkData.checkDate() != null ? checkData.checkDate() : LocalDate.now())
                    .dueDate(checkData.dueDate())
                    .bankName(checkData.bankName().trim())
                    .customer(customer)
                    .amount(amount)
                    .status(CheckStatus.PORTFOLIO)
                    .notes(notes)
                    .build();

            checkRepository.save(check);
            savedCollection.setCheckRecord(check);
        }

        if (invoice != null) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
        }

        return savedCollection;
    }

    @Transactional
    public CheckRecord updateCheckStatus(Long checkId, CheckStatus newStatus, String notes) {
        CheckRecord check = checkRepository.findById(checkId)
                .orElseThrow(() -> new IllegalArgumentException("Çek bulunamadı: " + checkId));

        check.setStatus(newStatus);
        if (notes != null && !notes.isBlank()) {
            check.setNotes((check.getNotes() != null ? check.getNotes() + " | " : "") + notes);
        }
        return checkRepository.save(check);
    }

    @Transactional(readOnly = true)
    public Page<CollectionRecord> searchCollections(CollectionMethod method, Long customerId,
                                                    LocalDate startDate, LocalDate endDate,
                                                    String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return collectionRepository.searchCollections(method, customerId, startDate, endDate, search, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CheckRecord> searchChecks(CheckStatus status, Long customerId,
                                          LocalDate startDate, LocalDate endDate,
                                          String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return checkRepository.searchChecks(status, customerId, startDate, endDate, search, pageable);
    }

    @Transactional(readOnly = true)
    public List<CheckRecord> getApproachingChecks() {
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(15);
        return checkRepository.findApproachingChecks(today, until);
    }

    @Transactional(readOnly = true)
    public List<CheckRecord> getOverdueChecks() {
        LocalDate today = LocalDate.now();
        return checkRepository.findOverdueChecks(today);
    }

    @Transactional(readOnly = true)
    public long getApproachingChecksCount() {
        LocalDate today = LocalDate.now();
        return checkRepository.countApproachingChecks(today, today.plusDays(15));
    }

    @Transactional(readOnly = true)
    public BigDecimal getMonthlyTotal() {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1);
        return collectionRepository.sumAmountByMethodAndDateRange(null, start, end);
    }
}
