package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.CheckRecord;
import com.ozerler.marble.model.CollectionRecord;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.enums.CheckStatus;
import com.ozerler.marble.model.enums.CollectionMethod;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.InvoiceRepository;
import com.ozerler.marble.service.CollectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionControllerTest {

    @Mock
    private CollectionService collectionService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private CollectionController controller;

    @Test
    @DisplayName("index initializes selectedMethod as empty string when no method parameter is provided")
    void index_DefaultsToEmptyMethodWhenNull() {
        when(collectionService.getMonthlyTotal()).thenReturn(BigDecimal.valueOf(10000));
        when(collectionService.getApproachingChecksCount()).thenReturn(3L);
        when(customerRepository.findAll()).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.index(null, null, model);

        assertThat(view).isEqualTo("collections/list");
        assertThat(model.getAttribute("selectedMethod")).isEqualTo("");
        assertThat(model.getAttribute("monthlyTotal")).isEqualTo(BigDecimal.valueOf(10000));
        assertThat(model.getAttribute("approachingChecksCount")).isEqualTo(3L);
    }

    @Test
    @DisplayName("index initializes selectedMethod with enum name when method parameter is provided")
    void index_SetsSelectedMethodWhenProvided() {
        when(collectionService.getMonthlyTotal()).thenReturn(BigDecimal.valueOf(10000));
        when(collectionService.getApproachingChecksCount()).thenReturn(3L);
        when(customerRepository.findAll()).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.index(CollectionMethod.CASH, null, model);

        assertThat(view).isEqualTo("collections/list");
        assertThat(model.getAttribute("selectedMethod")).isEqualTo("CASH");
    }

    @Test
    @DisplayName("getCollectionsData successfully queries all collections when method is null")
    void getCollectionsData_ReturnsDataWhenMethodNull() {
        Customer customer = Customer.builder().companyName("Öz Marble Ltd").build();
        customer.setId(10L);

        CollectionRecord record = CollectionRecord.builder()
                .id(1L)
                .collectionNo("THS-2026-0001")
                .collectionDate(LocalDate.of(2026, 9, 30))
                .collectionMethod(CollectionMethod.CASH)
                .customer(customer)
                .amount(BigDecimal.valueOf(5000))
                .notes("Kasa tahsilatı")
                .build();

        Page<CollectionRecord> pageResult = new PageImpl<>(List.of(record));
        when(collectionService.searchCollections(isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(25)))
                .thenReturn(pageResult);

        TabulatorResponse<Map<String, Object>> response = controller.getCollectionsData(1, 25, null, null, null);

        assertThat(response).isNotNull();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).get("collectionNo")).isEqualTo("THS-2026-0001");
        assertThat(response.getData().get(0).get("method")).isEqualTo("CASH");
        assertThat(response.getData().get(0).get("customerName")).isEqualTo("Öz Marble Ltd");
        assertThat(response.getData().get(0).get("amount")).isEqualTo(BigDecimal.valueOf(5000));
    }

    @Test
    @DisplayName("getCollectionsData forwards method filter enum to collection service")
    void getCollectionsData_ForwardsMethodFilter() {
        Page<CollectionRecord> emptyPage = new PageImpl<>(List.of());
        when(collectionService.searchCollections(eq(CollectionMethod.BANK_TRANSFER), isNull(), isNull(), isNull(), eq("test"), eq(0), eq(50)))
                .thenReturn(emptyPage);

        TabulatorResponse<Map<String, Object>> response = controller.getCollectionsData(1, 50, "test", CollectionMethod.BANK_TRANSFER, null);

        assertThat(response).isNotNull();
        assertThat(response.getData()).isEmpty();
        verify(collectionService).searchCollections(CollectionMethod.BANK_TRANSFER, null, null, null, "test", 0, 50);
    }

    @Test
    @DisplayName("getCollectionsData forwards customerId filter to collection service")
    void getCollectionsData_ForwardsCustomerIdFilter() {
        Page<CollectionRecord> emptyPage = new PageImpl<>(List.of());
        when(collectionService.searchCollections(isNull(), eq(42L), isNull(), isNull(), isNull(), eq(0), eq(25)))
                .thenReturn(emptyPage);

        TabulatorResponse<Map<String, Object>> response = controller.getCollectionsData(1, 25, null, null, 42L);

        assertThat(response).isNotNull();
        assertThat(response.getData()).isEmpty();
        verify(collectionService).searchCollections(null, 42L, null, null, null, 0, 25);
    }

    @Test
    @DisplayName("getChecksData returns paginated checks with customer and status details")
    void getChecksData_ReturnsDataWhenStatusNull() {
        Customer customer = Customer.builder().companyName("Alp Madencilik").build();
        customer.setId(5L);

        CheckRecord check = CheckRecord.builder()
                .id(101L)
                .checkNo("CHK-999")
                .bankName("İş Bankası")
                .customer(customer)
                .dueDate(LocalDate.of(2026, 10, 15))
                .amount(BigDecimal.valueOf(150000))
                .status(CheckStatus.PORTFOLIO)
                .notes("Müşteri çeki")
                .build();

        Page<CheckRecord> pageResult = new PageImpl<>(List.of(check));
        when(collectionService.searchChecks(isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(25)))
                .thenReturn(pageResult);

        TabulatorResponse<Map<String, Object>> response = controller.getChecksData(1, 25, null, null, null);

        assertThat(response).isNotNull();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).get("checkNo")).isEqualTo("CHK-999");
        assertThat(response.getData().get(0).get("bankName")).isEqualTo("İş Bankası");
        assertThat(response.getData().get(0).get("customerName")).isEqualTo("Alp Madencilik");
        assertThat(response.getData().get(0).get("status")).isEqualTo("PORTFOLIO");
    }

    @Test
    @DisplayName("getChecksData forwards status filter enum to collection service")
    void getChecksData_ForwardsStatusFilter() {
        Page<CheckRecord> emptyPage = new PageImpl<>(List.of());
        when(collectionService.searchChecks(eq(CheckStatus.COLLECTED), isNull(), isNull(), isNull(), eq("vade"), eq(0), eq(25)))
                .thenReturn(emptyPage);

        TabulatorResponse<Map<String, Object>> response = controller.getChecksData(1, 25, "vade", CheckStatus.COLLECTED, null);

        assertThat(response).isNotNull();
        assertThat(response.getData()).isEmpty();
        verify(collectionService).searchChecks(CheckStatus.COLLECTED, null, null, null, "vade", 0, 25);
    }

    @Test
    @DisplayName("updateCheckStatus invokes collection service and redirects to checks list")
    void updateCheckStatus_UpdatesAndRedirects() {
        CheckRecord updated = CheckRecord.builder().id(50L).checkNo("CHK-50").build();
        when(collectionService.updateCheckStatus(eq(50L), eq(CheckStatus.BOUNCED), eq("Karşılıksız")))
                .thenReturn(updated);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.updateCheckStatus(50L, CheckStatus.BOUNCED, "Karşılıksız", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/collections/checks");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
        verify(collectionService).updateCheckStatus(50L, CheckStatus.BOUNCED, "Karşılıksız");
    }
}
