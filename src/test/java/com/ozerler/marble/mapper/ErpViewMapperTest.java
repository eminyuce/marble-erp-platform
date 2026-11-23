package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.form.CustomerForm;
import com.ozerler.marble.dto.view.InvoiceRef;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CustomerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ErpViewMapperTest {

    private ErpViewMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ErpViewMapperImpl();
    }

    @Test
    @DisplayName("customer form keeps the fields the definition screen edits")
    void customerRoundTrip() {
        Customer customer = Customer.builder()
                .customerCode("MUS-1")
                .companyName("Atlas")
                .customerType(CustomerType.CONSTRUCTION)
                .businessUnit(BusinessUnit.SITE)
                .phone("555")
                .email("keep@example.com")
                .build();
        customer.setId(4L);

        CustomerForm form = mapper.toForm(customer);
        assertThat(form.getId()).isEqualTo(4L);
        assertThat(form.getCompanyName()).isEqualTo("Atlas");
        assertThat(form.getCustomerType()).isEqualTo(CustomerType.CONSTRUCTION);
        assertThat(form.getBusinessUnit()).isEqualTo(BusinessUnit.SITE);

        Customer target = new Customer();
        mapper.update(form, target);
        assertThat(target.getCompanyName()).isEqualTo("Atlas");
        assertThat(target.getEmail()).isEqualTo("keep@example.com");
        assertThat(target.getId()).isEqualTo(4L);
    }

    @Test
    @DisplayName("invoice ref exposes the number and total without the invoice graph")
    void invoiceRef() {
        Invoice invoice = Invoice.builder()
                .invoiceNo("FTR-1")
                .totalAmount(new BigDecimal("18.00"))
                .build();
        invoice.setId(9L);

        InvoiceRef ref = mapper.toRef(invoice);
        assertThat(ref.getId()).isEqualTo(9L);
        assertThat(ref.getInvoiceNo()).isEqualTo("FTR-1");
        assertThat(ref.getTotalAmount()).isEqualByComparingTo("18.00");
    }
}
