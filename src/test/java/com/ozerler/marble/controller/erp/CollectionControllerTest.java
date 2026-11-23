package com.ozerler.marble.controller.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.view.CheckRef;
import com.ozerler.marble.dto.view.InvoiceView;
import com.ozerler.marble.dto.view.PartyView;
import com.ozerler.marble.model.CheckRecord;
import com.ozerler.marble.model.CollectionRecord;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.enums.CheckStatus;
import com.ozerler.marble.model.enums.CollectionMethod;
import com.ozerler.marble.service.CollectionService;
import com.ozerler.marble.service.FormLookupService;
import com.ozerler.marble.web.ListQuery;

@ExtendWith(MockitoExtension.class)
class CollectionControllerTest {

    @Mock
    private FormLookupService formLookup;

    @Mock
    private CollectionService collectionService;

    @InjectMocks
    private CollectionController controller;

    @Test
    @DisplayName("index initializes selectedMethod as empty string when no method parameter is provided")
    void indexDefaultsToEmptyMethodWhenNull() {
        Mockito.when(collectionService.getMonthlyTotal()).thenReturn(BigDecimal.valueOf(10000));
        Mockito.when(collectionService.getApproachingChecksCount()).thenReturn(3L);
        Mockito.when(collectionService.searchCollectionViews(null, null, null, null, null, 0, 20))
                .thenReturn(Page.empty());
        Mockito.when(formLookup.customers()).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.index(null, null, new ListQuery(), model);

        Assertions.assertThat(view).isEqualTo("collections/list");
        Assertions.assertThat(model.getAttribute("selectedMethod")).isEqualTo("");
        Assertions.assertThat(model.getAttribute("monthlyTotal")).isEqualTo(BigDecimal.valueOf(10000));
        Assertions.assertThat(model.getAttribute("approachingChecksCount")).isEqualTo(3L);
    }

    @Test
    @DisplayName("index initializes selectedMethod with enum name when method parameter is provided")
    void indexSetsSelectedMethodWhenProvided() {
        Mockito.when(collectionService.getMonthlyTotal()).thenReturn(BigDecimal.valueOf(10000));
        Mockito.when(collectionService.getApproachingChecksCount()).thenReturn(3L);
        Mockito.when(
                        collectionService.searchCollectionViews(
                                CollectionMethod.CASH, null, null, null, null, 0, 20))
                .thenReturn(Page.empty());
        Mockito.when(formLookup.customers()).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.index(CollectionMethod.CASH, null, new ListQuery(), model);

        Assertions.assertThat(view).isEqualTo("collections/list");
        Assertions.assertThat(model.getAttribute("selectedMethod")).isEqualTo("CASH");
    }

    @Test
    @DisplayName("getCollectionsData successfully queries all collections when method is null")
    void getCollectionsDataReturnsDataWhenMethodNull() {
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
        Mockito.when(
                        collectionService.searchCollections(
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.eq(0),
                                ArgumentMatchers.eq(25)))
                .thenReturn(pageResult);

        Mockito.when(
                        collectionService.collectionRows(
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.anyInt()))
                .thenCallRealMethod();

        TabulatorResponse<Map<String, Object>> response = controller.getCollectionsData(1, 25, null, null, null);

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getData()).hasSize(1);
        Assertions.assertThat(response.getData().get(0).get("collectionNo")).isEqualTo("THS-2026-0001");
        Assertions.assertThat(response.getData().get(0).get("method")).isEqualTo("CASH");
        Assertions.assertThat(response.getData().get(0).get("customerName")).isEqualTo("Öz Marble Ltd");
        Assertions.assertThat(response.getData().get(0).get("amount")).isEqualTo(BigDecimal.valueOf(5000));
    }

    @Test
    @DisplayName("getCollectionsData forwards method filter enum to collection service")
    void getCollectionsDataForwardsMethodFilter() {
        Page<CollectionRecord> emptyPage = new PageImpl<>(List.of());
        Mockito.when(
                        collectionService.searchCollections(
                                ArgumentMatchers.eq(CollectionMethod.BANK_TRANSFER),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.eq("test"),
                                ArgumentMatchers.eq(0),
                                ArgumentMatchers.eq(50)))
                .thenReturn(emptyPage);

        Mockito.when(
                        collectionService.collectionRows(
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.anyInt()))
                .thenCallRealMethod();

        TabulatorResponse<Map<String, Object>> response =
                controller.getCollectionsData(1, 50, "test", CollectionMethod.BANK_TRANSFER, null);

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getData()).isEmpty();
        Mockito.verify(collectionService)
                .searchCollections(CollectionMethod.BANK_TRANSFER, null, null, null, "test", 0, 50);
    }

    @Test
    @DisplayName("getCollectionsData forwards customerId filter to collection service")
    void getCollectionsDataForwardsCustomerIdFilter() {
        Page<CollectionRecord> emptyPage = new PageImpl<>(List.of());
        Mockito.when(
                        collectionService.searchCollections(
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.eq(42L),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.eq(0),
                                ArgumentMatchers.eq(25)))
                .thenReturn(emptyPage);

        Mockito.when(
                        collectionService.collectionRows(
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.anyInt()))
                .thenCallRealMethod();

        TabulatorResponse<Map<String, Object>> response = controller.getCollectionsData(1, 25, null, null, 42L);

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getData()).isEmpty();
        Mockito.verify(collectionService).searchCollections(null, 42L, null, null, null, 0, 25);
    }

    @Test
    @DisplayName("getChecksData returns paginated checks with customer and status details")
    void getChecksDataReturnsDataWhenStatusNull() {
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
        Mockito.when(
                        collectionService.searchChecks(
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.eq(0),
                                ArgumentMatchers.eq(25)))
                .thenReturn(pageResult);

        Mockito.when(
                        collectionService.checkRows(
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.anyInt()))
                .thenCallRealMethod();

        TabulatorResponse<Map<String, Object>> response = controller.getChecksData(1, 25, null, null, null, null);

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getData()).hasSize(1);
        Assertions.assertThat(response.getData().get(0).get("checkNo")).isEqualTo("CHK-999");
        Assertions.assertThat(response.getData().get(0).get("bankName")).isEqualTo("İş Bankası");
        Assertions.assertThat(response.getData().get(0).get("customerName")).isEqualTo("Alp Madencilik");
        Assertions.assertThat(response.getData().get(0).get("status")).isEqualTo("PORTFOLIO");
    }

    @Test
    @DisplayName("getChecksData forwards status filter enum to collection service")
    void getChecksDataForwardsStatusFilter() {
        Page<CheckRecord> emptyPage = new PageImpl<>(List.of());
        Mockito.when(
                        collectionService.searchChecks(
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.eq(CheckStatus.COLLECTED),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.eq("vade"),
                                ArgumentMatchers.eq(0),
                                ArgumentMatchers.eq(25)))
                .thenReturn(emptyPage);

        Mockito.when(
                        collectionService.checkRows(
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.anyInt()))
                .thenCallRealMethod();

        TabulatorResponse<Map<String, Object>> response =
                controller.getChecksData(1, 25, "vade", null, CheckStatus.COLLECTED, null);

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getData()).isEmpty();
        Mockito.verify(collectionService).searchChecks(null, CheckStatus.COLLECTED, null, null, null, "vade", 0, 25);
    }

    @Test
    @DisplayName("updateCheckStatus invokes collection service and redirects to checks list")
    void updateCheckStatusUpdatesAndRedirects() {
        CheckRef updated = new CheckRef();
        updated.setId(50L);
        updated.setCheckNo("CHK-50");
        Mockito.when(
                        collectionService.updateCheckStatusRef(
                                ArgumentMatchers.eq(50L),
                                ArgumentMatchers.eq(CheckStatus.BOUNCED),
                                ArgumentMatchers.eq("Karşılıksız")))
                .thenReturn(updated);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.updateCheckStatus(50L, CheckStatus.BOUNCED, "Karşılıksız", null, redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/collections/checks");
        Assertions.assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
        Mockito.verify(collectionService).updateCheckStatusRef(50L, CheckStatus.BOUNCED, "Karşılıksız");
    }

    @Test
    @DisplayName("new collection form loads invoices with their customers")
    void newCollectionFormLoadsInvoicesWithCustomer() {
        Mockito.when(formLookup.customers()).thenReturn(List.of());
        Mockito.when(formLookup.invoicesWithCustomer()).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.newCollectionForm(null, null, model);

        Assertions.assertThat(view).isEqualTo("collections/form");
        Assertions.assertThat(model.getAttribute("invoices")).isEqualTo(List.of());
        Mockito.verify(formLookup).invoicesWithCustomer();
    }

    @Test
    @DisplayName("new collection form preselects the customer from the fetched invoice")
    void newCollectionFormPreselectsCustomerFromInvoice() {
        PartyView party = new PartyView();
        party.setId(4L);
        party.setCompanyName("Özerler");
        InvoiceView invoiceView = new InvoiceView();
        invoiceView.setId(9L);
        invoiceView.setCustomer(party);
        Mockito.when(formLookup.findInvoiceWithCustomer(9L)).thenReturn(invoiceView);
        Mockito.when(formLookup.invoicesWithCustomer()).thenReturn(List.of(invoiceView));
        Mockito.when(formLookup.customers()).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.newCollectionForm(9L, null, model);

        Assertions.assertThat(view).isEqualTo("collections/form");
        Assertions.assertThat(model.getAttribute("selectedCustomerId")).isEqualTo(4L);
    }
}
