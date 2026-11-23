package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.view.CheckRef;
import com.ozerler.marble.dto.view.CollectionRef;
import com.ozerler.marble.mapper.CollectionMapper;
import com.ozerler.marble.model.CheckRecord;
import com.ozerler.marble.model.CollectionRecord;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CheckStatus;
import com.ozerler.marble.model.enums.CollectionMethod;
import com.ozerler.marble.model.enums.InvoiceStatus;
import com.ozerler.marble.repository.CheckRecordRepository;
import com.ozerler.marble.repository.CollectionRecordRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.InvoiceRepository;
import com.ozerler.marble.util.UniqueCodes;
import com.ozerler.marble.web.GridColumnFilterContext;
import com.ozerler.marble.web.GridFilterCatalog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

    private static final int CHECK_WARNING_DAYS = 15;

    private final CollectionRecordRepository collectionRepository;
    private final CollectionMapper collectionMapper;
    private final CheckRecordRepository checkRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final CariTransactionService cariTransactionService;

    public record CheckFormData(String checkNo, LocalDate checkDate, LocalDate dueDate, String bankName) {
    }

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

        Invoice invoice;
        if (invoiceId != null) {
            invoice = invoiceRepository.findById(invoiceId).orElse(null);
        } else {
            invoice = null;
        }

        String code = UniqueCodes.yearly("THS", collectionRepository::existsByCollectionNo);

        CollectionRecord collection = CollectionRecord.builder()
                .collectionNo(code)
                .collectionDate(LocalDate.now())
                .customer(customer)
                .businessUnit(customer.getBusinessUnit())
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

            LocalDate checkDate;
            if (checkData.checkDate() != null) {
                checkDate = checkData.checkDate();
            } else {
                checkDate = LocalDate.now();
            }
            CheckRecord check = CheckRecord.builder()
                    .collection(savedCollection)
                    .checkNo(checkData.checkNo().trim())
                    .checkDate(checkDate)
                    .dueDate(checkData.dueDate())
                    .bankName(checkData.bankName().trim())
                    .customer(customer)
                    .businessUnit(customer.getBusinessUnit())
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

        if (savedCollection.getCustomer() != null) {
            cariTransactionService.syncCollection(savedCollection);
        }

        return savedCollection;
    }

    @Transactional
    public CheckRecord updateCheckStatus(Long checkId, CheckStatus newStatus, String notes) {
        CheckRecord check = checkRepository.findById(checkId)
                .orElseThrow(() -> new IllegalArgumentException("Çek bulunamadı: " + checkId));

        check.setStatus(newStatus);
        if (notes != null && !notes.isBlank()) {
            String value;
            if (check.getNotes() != null) {
                value = check.getNotes() + " | ";
            } else {
                value = "";
            }
            check.setNotes(value + notes);
        }
        return checkRepository.save(check);
    }

    @Transactional(readOnly = true)
    public Page<CollectionRecord> searchCollections(CollectionMethod method, Long customerId,
                                                    LocalDate startDate, LocalDate endDate,
                                                    String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        try (GridColumnFilterContext ignored =
                     GridColumnFilterContext.open(GridFilterCatalog.aliases("collections-table"))) {
            return collectionRepository.searchCollections(
                    method, customerId, startDate, endDate, search, pageable);
        }
    }

    @Transactional(readOnly = true)
    public Page<CheckRecord> searchChecks(BusinessUnit unit, CheckStatus status, Long customerId,
                                          LocalDate startDate, LocalDate endDate,
                                          String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        try (GridColumnFilterContext ignored =
                     GridColumnFilterContext.open(GridFilterCatalog.aliases("checks-table"))) {
            return checkRepository.searchChecks(
                    unit, status, customerId, startDate, endDate, search, pageable);
        }
    }

    @Transactional(readOnly = true)
    public Page<CheckRecord> searchChecks(CheckStatus status, Long customerId,
                                          LocalDate startDate, LocalDate endDate,
                                          String search, int page, int size) {
        return searchChecks(null, status, customerId, startDate, endDate, search, page, size);
    }

    @Transactional(readOnly = true)
    public List<CheckRecord> getApproachingChecks(BusinessUnit unit) {
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(CHECK_WARNING_DAYS);
        if (unit == null) {
            return checkRepository.findApproachingChecks(today, until);
        }
        return checkRepository.findApproachingChecksByUnit(today, until, unit);
    }

    @Transactional(readOnly = true)
    public List<CheckRecord> getApproachingChecks() {
        return getApproachingChecks(null);
    }

    @Transactional(readOnly = true)
    public List<CheckRecord> getOverdueChecks(BusinessUnit unit) {
        LocalDate today = LocalDate.now();
        if (unit == null) {
            return checkRepository.findOverdueChecks(today);
        }
        return checkRepository.findOverdueChecksByUnit(today, unit);
    }

    @Transactional(readOnly = true)
    public List<CheckRecord> getOverdueChecks() {
        return getOverdueChecks(null);
    }

    @Transactional(readOnly = true)
    public long getApproachingChecksCount() {
        LocalDate today = LocalDate.now();
        return checkRepository.countApproachingChecks(today, today.plusDays(CHECK_WARNING_DAYS));
    }

    @Transactional(readOnly = true)
    public BigDecimal getMonthlyTotal() {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1);
        return collectionRepository.sumAmountByMethodAndDateRange(null, start, end);
    }

    /**
     * Same as {@code recordCollection}, copied into a view inside the transaction.
     *
     * @param customerId see {@code recordCollection}
     * @param invoiceId see {@code recordCollection}
     * @param method see {@code recordCollection}
     * @param amount see {@code recordCollection}
     * @param bankName see {@code recordCollection}
     * @param notes see {@code recordCollection}
     * @param checkData see {@code recordCollection}
     * @return the view
     */
    @Transactional
    public CollectionRef recordCollectionRef(Long customerId, Long invoiceId, CollectionMethod method,
                                            BigDecimal amount, String bankName, String notes,
                                            CheckFormData checkData) {
        return collectionMapper.toRef(
                recordCollection(
                        customerId, invoiceId, method, amount, bankName, notes, checkData));
    }

    /**
     * Same as {@code updateCheckStatus}, copied into a view inside the transaction.
     *
     * @param checkId see {@code updateCheckStatus}
     * @param newStatus see {@code updateCheckStatus}
     * @param notes see {@code updateCheckStatus}
     * @return the view
     */
    @Transactional
    public CheckRef updateCheckStatusRef(Long checkId, CheckStatus newStatus, String notes) {
        return collectionMapper.toRef(updateCheckStatus(checkId, newStatus, notes));
    }

    /**
     * Builds grid rows inside the read transaction (moved from the controller unchanged).
     *
     * @return grid rows
     */
    @Transactional(readOnly = true)
    public TabulatorResponse<Map<String, Object>> collectionRows(
            String search, CollectionMethod method, Long customerId, int page, int size) {
        int pageIndex = Math.max(0, page - 1);
        Page<CollectionRecord> collectionPage =
                searchCollections(method, customerId, null, null, search, pageIndex, size);

        List<Map<String, Object>> data = collectionPage.getContent().stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("collectionNo", c.getCollectionNo());
            Object value;
            if (c.getCollectionDate() != null) {
                value = c.getCollectionDate().toString();
            } else {
                value = "";
            }
            map.put("collectionDate", value);
            Object companyName;
            if (c.getCustomer() != null) {
                companyName = c.getCustomer().getCompanyName();
            } else {
                companyName = "—";
            }
            map.put("customerName", companyName);
            Object valueValue;
            if (c.getCollectionMethod() != null) {
                valueValue = c.getCollectionMethod().name();
            } else {
                valueValue = "";
            }
            map.put("method", valueValue);
            Object displayName;
            if (c.getCollectionMethod() != null) {
                displayName = c.getCollectionMethod().getDisplayName();
            } else {
                displayName = "";
            }
            map.put("methodLabel", displayName);
            map.put("amount", Objects.requireNonNullElse(c.getAmount(), BigDecimal.ZERO));
            map.put("bankName", Objects.requireNonNullElse(c.getBankName(), "—"));
            Object invoiceNumber;
            if (c.getInvoice() != null) {
                invoiceNumber = c.getInvoice().getInvoiceNumber();
            } else {
                invoiceNumber = "—";
            }
            map.put("invoiceNo", invoiceNumber);
            Long id;
            if (c.getInvoice() != null) {
                id = c.getInvoice().getId();
            } else {
                id = null;
            }
            map.put("invoiceId", id);
            map.put("notes", Objects.requireNonNullElse(c.getNotes(), ""));
            return map;
        }).toList();

        return TabulatorResponse.of(data, collectionPage.getTotalPages(), collectionPage.getTotalElements());
    }

    /**
     * Builds grid rows inside the read transaction (moved from the controller unchanged).
     *
     * @return grid rows
     */
    @Transactional(readOnly = true)
    public TabulatorResponse<Map<String, Object>> checkRows(
            String search,
            BusinessUnit unit,
            CheckStatus status,
            Long customerId,
            int page,
            int size) {
        int pageIndex = Math.max(0, page - 1);
        Page<CheckRecord> checkPage = searchChecks(unit, status, customerId, null, null, search, pageIndex, size);

        List<Map<String, Object>> data = checkPage.getContent().stream().map(ch -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ch.getId());
            map.put("checkNo", ch.getCheckNo());
            map.put("bankName", Objects.requireNonNullElse(ch.getBankName(), "—"));
            Object companyName;
            if (ch.getCustomer() != null) {
                companyName = ch.getCustomer().getCompanyName();
            } else {
                companyName = "—";
            }
            map.put("customerName", companyName);
            BusinessUnit bu = ch.getEffectiveBusinessUnit();
            Object value;
            if (bu != null) {
                value = bu.name();
            } else {
                value = "";
            }
            map.put("businessUnit", value);
            Object displayName;
            if (bu != null) {
                displayName = bu.getDisplayName();
            } else {
                displayName = "Genel";
            }
            map.put("businessUnitLabel", displayName);
            Object valueValue;
            if (ch.getDueDate() != null) {
                valueValue = ch.getDueDate().toString();
            } else {
                valueValue = "";
            }
            map.put("dueDate", valueValue);
            map.put("amount", Objects.requireNonNullElse(ch.getAmount(), BigDecimal.ZERO));
            Object valueValue2;
            if (ch.getStatus() != null) {
                valueValue2 = ch.getStatus().name();
            } else {
                valueValue2 = "";
            }
            map.put("status", valueValue2);
            Object label;
            if (ch.getStatus() != null) {
                label = ch.getStatus().getLabel();
            } else {
                label = "";
            }
            map.put("statusLabel", label);
            map.put("notes", Objects.requireNonNullElse(ch.getNotes(), ""));
            return map;
        }).toList();

        return TabulatorResponse.of(data, checkPage.getTotalPages(), checkPage.getTotalElements());
    }

    /**
     * Same as {@code searchCollections}, copied into a view inside the transaction.
     *
     * @param method see {@code searchCollections}
     * @param customerId see {@code searchCollections}
     * @param startDate see {@code searchCollections}
     * @param endDate see {@code searchCollections}
     * @param search see {@code searchCollections}
     * @param page see {@code searchCollections}
     * @param size see {@code searchCollections}
     * @return the view
     */
    @Transactional(readOnly = true)
    public Page<CollectionRef> searchCollectionViews(CollectionMethod method, Long customerId,
                                                    LocalDate startDate, LocalDate endDate,
                                                    String search, int page, int size) {
        return searchCollections(method, customerId, startDate, endDate, search, page, size)
                .map(collectionMapper::toRef);
    }

    /**
     * Same as {@code searchChecks}, copied into a view inside the transaction.
     *
     * @param unit see {@code searchChecks}
     * @param status see {@code searchChecks}
     * @param customerId see {@code searchChecks}
     * @param startDate see {@code searchChecks}
     * @param endDate see {@code searchChecks}
     * @param search see {@code searchChecks}
     * @param page see {@code searchChecks}
     * @param size see {@code searchChecks}
     * @return the view
     */
    @Transactional(readOnly = true)
    public Page<CheckRef> searchCheckViews(BusinessUnit unit, CheckStatus status, Long customerId,
                                          LocalDate startDate, LocalDate endDate,
                                          String search, int page, int size) {
        return searchChecks(unit, status, customerId, startDate, endDate, search, page, size)
                .map(collectionMapper::toRef);
    }

    /**
     * Same as {@code getApproachingChecks}, copied into a view inside the transaction.
     *
     * @param unit see {@code getApproachingChecks}
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<CheckRef> getApproachingCheckViews(BusinessUnit unit) {
        return getApproachingChecks(unit).stream().map(collectionMapper::toRef).toList();
    }

    /**
     * Same as {@code getOverdueChecks}, copied into a view inside the transaction.
     *
     * @param unit see {@code getOverdueChecks}
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<CheckRef> getOverdueCheckViews(BusinessUnit unit) {
        return getOverdueChecks(unit).stream().map(collectionMapper::toRef).toList();
    }
}
