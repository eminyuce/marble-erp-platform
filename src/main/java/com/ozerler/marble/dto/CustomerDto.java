package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.enums.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("customerCode")
    @JsonAlias("customer_code")
    private String customerCode;

    @JsonProperty("companyName")
    @JsonAlias("company_name")
    private String companyName;

    @JsonProperty("contactPerson")
    @JsonAlias("contact_person")
    private String contactPerson;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("email")
    private String email;

    @JsonProperty("address")
    private String address;

    @JsonProperty("taxOffice")
    @JsonAlias("tax_office")
    private String taxOffice;

    @JsonProperty("taxNumber")
    @JsonAlias("tax_number")
    private String taxNumber;

    @JsonProperty("customerType")
    @JsonAlias("customer_type")
    private String customerType;

    @JsonProperty("customerTypeLabel")
    @JsonAlias("customer_type_label")
    private String customerTypeLabel;

    @JsonProperty("notes")
    private String notes;

    public static CustomerDto fromEntity(Customer customer) {
        CustomerType type = customer.getCustomerType();
        return CustomerDto.builder()
                .id(customer.getId())
                .customerCode(customer.getCustomerCode())
                .companyName(customer.getCompanyName())
                .contactPerson(customer.getContactPerson())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .taxOffice(customer.getTaxOffice())
                .taxNumber(customer.getTaxNumber())
                .customerType(type != null ? type.name() : null)
                .customerTypeLabel(type != null ? type.getLabel() : "")
                .notes(customer.getNotes())
                .build();
    }
}
