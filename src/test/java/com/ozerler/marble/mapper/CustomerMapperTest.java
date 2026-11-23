package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.form.CustomerForm;
import com.ozerler.marble.dto.view.PartyView;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CustomerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerMapperTest {

    private CustomerMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CustomerMapperImpl();
    }

    @Test
    @DisplayName("customer form copies edited fields and leaves audit data on the entity")
    void formCopiesFieldsWithoutBusinessRules() {
        Customer customer = Customer.builder()
                .customerCode("MUS-1")
                .companyName("Atlas")
                .customerType(CustomerType.CONSTRUCTION)
                .businessUnit(BusinessUnit.SITE)
                .phone("555")
                .email("keep@example.com")
                .build();
        customer.setId(4L);
        customer.setCreatedDate(java.time.LocalDateTime.of(2026, 1, 2, 3, 4));

        CustomerForm form = mapper.toForm(customer);
        assertThat(form.getCompanyName()).isEqualTo("Atlas");
        assertThat(form.getCustomerType()).isEqualTo(CustomerType.CONSTRUCTION);

        Customer target = new Customer();
        target.setCreatedDate(customer.getCreatedDate());
        mapper.updateEntity(form, target);
        assertThat(target.getCompanyName()).isEqualTo("Atlas");
        assertThat(target.getEmail()).isEqualTo("keep@example.com");
        assertThat(target.getCreatedDate()).isEqualTo(customer.getCreatedDate());
    }

    @Test
    @DisplayName("party view copies the customer name and does not invent a supplier type")
    void partyViewCopiesCustomer() {
        Customer customer = Customer.builder().companyName("Atlas").customerType(CustomerType.CONSTRUCTION).build();
        customer.setId(4L);
        PartyView view = mapper.toDto(customer);
        assertThat(view.getId()).isEqualTo(4L);
        assertThat(view.getCompanyName()).isEqualTo("Atlas");
        assertThat(view.getSupplierType()).isNull();
    }
}
