package com.ozerler.marble.mapper;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.dto.form.CustomerForm;
import com.ozerler.marble.dto.view.PartyView;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CustomerType;

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
        customer.setCreatedDate(LocalDateTime.of(2026, 1, 2, 3, 4));

        CustomerForm form = mapper.toForm(customer);
        Assertions.assertThat(form.getCompanyName()).isEqualTo("Atlas");
        Assertions.assertThat(form.getCustomerType()).isEqualTo(CustomerType.CONSTRUCTION);

        Customer target = new Customer();
        target.setCreatedDate(customer.getCreatedDate());
        mapper.updateEntity(form, target);
        Assertions.assertThat(target.getCompanyName()).isEqualTo("Atlas");
        Assertions.assertThat(target.getEmail()).isEqualTo("keep@example.com");
        Assertions.assertThat(target.getCreatedDate()).isEqualTo(customer.getCreatedDate());
    }

    @Test
    @DisplayName("party view copies the customer name and does not invent a supplier type")
    void partyViewCopiesCustomer() {
        Customer customer = Customer.builder().companyName("Atlas").customerType(CustomerType.CONSTRUCTION).build();
        customer.setId(4L);
        PartyView view = mapper.toDto(customer);
        Assertions.assertThat(view.getId()).isEqualTo(4L);
        Assertions.assertThat(view.getCompanyName()).isEqualTo("Atlas");
        Assertions.assertThat(view.getSupplierType()).isNull();
    }
}
