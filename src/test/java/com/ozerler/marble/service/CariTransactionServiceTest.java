package com.ozerler.marble.service;

import com.ozerler.marble.dto.CariSummaryDto;
import com.ozerler.marble.dto.CariTransactionDto;
import com.ozerler.marble.model.CariTransaction;
import com.ozerler.marble.model.CollectionRecord;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.enums.CariTransactionType;
import com.ozerler.marble.model.enums.CollectionMethod;
import com.ozerler.marble.model.enums.InvoiceType;
import com.ozerler.marble.repository.CariTransactionRepository;
import com.ozerler.marble.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CariTransactionServiceTest {

    @Mock
    private CariTransactionRepository transactionRepository;

    @Mock
    private CustomerRepository customerRepository;

    private CariTransactionService cariTransactionService;

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        cariTransactionService = new CariTransactionService(transactionRepository, customerRepository);
        sampleCustomer = Customer.builder()
                .id(1L)
                .customerCode("CR-001")
                .companyName("Örnek Müşteri Ltd.")
                .build();
    }

    @Test
    @DisplayName("getTransactionsForCustomer computes running balance chronologically and returns reverse order")
    void testGetTransactionsForCustomer() {
        CariTransaction tx1 = CariTransaction.builder()
                .id(101L)
                .customer(sampleCustomer)
                .transactionDate(LocalDate.of(2026, 1, 10))
                .transactionType(CariTransactionType.SATIS_FATURASI)
                .documentNo("FAT-01")
                .debitAmount(new BigDecimal("1000.00"))
                .creditAmount(BigDecimal.ZERO)
                .build();

        CariTransaction tx2 = CariTransaction.builder()
                .id(102L)
                .customer(sampleCustomer)
                .transactionDate(LocalDate.of(2026, 1, 15))
                .transactionType(CariTransactionType.TAHSILAT)
                .documentNo("THS-01")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("400.00"))
                .build();

        when(transactionRepository.findByCustomerIdOrderByTransactionDateAscIdAsc(1L))
                .thenReturn(new ArrayList<>(List.of(tx1, tx2)));

        List<CariTransactionDto> result = cariTransactionService.getTransactionsForCustomer(1L);

        assertThat(result).hasSize(2);
        // Most recent first
        assertThat(result.get(0).getId()).isEqualTo(102L);
        assertThat(result.get(0).getRunningBalance()).isEqualByComparingTo("600.00");

        assertThat(result.get(1).getId()).isEqualTo(101L);
        assertThat(result.get(1).getRunningBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("getCustomerSummary determines correct balance status")
    void testGetCustomerSummary() {
        List<Object[]> totals = new ArrayList<>();
        totals.add(new Object[]{new BigDecimal("5000.00"), new BigDecimal("3000.00")});

        when(transactionRepository.getDebitCreditTotalsByCustomerId(1L)).thenReturn(totals);
        when(transactionRepository.findByCustomerIdOrderByTransactionDateAscIdAsc(1L)).thenReturn(List.of(new CariTransaction()));

        CariSummaryDto summary = cariTransactionService.getCustomerSummary(1L);

        assertThat(summary.getTotalDebit()).isEqualByComparingTo("5000.00");
        assertThat(summary.getTotalCredit()).isEqualByComparingTo("3000.00");
        assertThat(summary.getNetBalance()).isEqualByComparingTo("2000.00");
        assertThat(summary.getBalanceStatus()).isEqualTo("BORÇLU (B)");
    }

    @Test
    @DisplayName("createTransaction persists a new cari transaction")
    void testCreateTransaction() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(transactionRepository.save(any(CariTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        CariTransaction saved = cariTransactionService.createTransaction(
                1L, LocalDate.now(), CariTransactionType.BORC_DEKONTU,
                "DKN-01", "Vade Farkı", new BigDecimal("150.00"), BigDecimal.ZERO, "Açıklama"
        );

        assertThat(saved.getCustomer()).isEqualTo(sampleCustomer);
        assertThat(saved.getDebitAmount()).isEqualByComparingTo("150.00");
        assertThat(saved.getCreditAmount()).isEqualByComparingTo("0.00");
        verify(transactionRepository, times(1)).save(any(CariTransaction.class));
    }

    @Test
    @DisplayName("syncInvoice creates or updates cari transaction on sales invoice")
    void testSyncInvoice() {
        Invoice invoice = Invoice.builder()
                .id(50L)
                .invoiceNo("FAT-2026-001")
                .invoiceDate(LocalDate.now())
                .invoiceType(InvoiceType.SALES)
                .customer(sampleCustomer)
                .totalAmount(new BigDecimal("12500.00"))
                .notes("İhracat faturası")
                .build();

        when(transactionRepository.findByInvoiceId(50L)).thenReturn(Optional.empty());

        cariTransactionService.syncInvoice(invoice);

        ArgumentCaptor<CariTransaction> captor = ArgumentCaptor.forClass(CariTransaction.class);
        verify(transactionRepository).save(captor.capture());

        CariTransaction captured = captor.getValue();
        assertThat(captured.getCustomer()).isEqualTo(sampleCustomer);
        assertThat(captured.getTransactionType()).isEqualTo(CariTransactionType.SATIS_FATURASI);
        assertThat(captured.getDebitAmount()).isEqualByComparingTo("12500.00");
        assertThat(captured.getCreditAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("syncCollection creates or updates cari transaction on payment collection")
    void testSyncCollection() {
        CollectionRecord collection = CollectionRecord.builder()
                .id(77L)
                .collectionNo("THS-2026-001")
                .collectionDate(LocalDate.now())
                .customer(sampleCustomer)
                .collectionMethod(CollectionMethod.BANK_TRANSFER)
                .amount(new BigDecimal("5000.00"))
                .notes("Havale")
                .build();

        when(transactionRepository.findByCollectionId(77L)).thenReturn(Optional.empty());

        cariTransactionService.syncCollection(collection);

        ArgumentCaptor<CariTransaction> captor = ArgumentCaptor.forClass(CariTransaction.class);
        verify(transactionRepository).save(captor.capture());

        CariTransaction captured = captor.getValue();
        assertThat(captured.getCustomer()).isEqualTo(sampleCustomer);
        assertThat(captured.getTransactionType()).isEqualTo(CariTransactionType.TAHSILAT);
        assertThat(captured.getDebitAmount()).isEqualByComparingTo("0.00");
        assertThat(captured.getCreditAmount()).isEqualByComparingTo("5000.00");
    }
}
