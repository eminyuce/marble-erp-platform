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

    @JsonProperty("businessUnit")
    @JsonAlias("business_unit")
    private String businessUnit;

    @JsonProperty("businessUnitLabel")
    @JsonAlias("business_unit_label")
    private String businessUnitLabel;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("balance")
    private java.math.BigDecimal balance;

    @JsonProperty("projectCount")
    @JsonAlias("project_count")
    private int projectCount;

    public static CustomerDto fromEntity(Customer customer) {
        CustomerType type = customer.getCustomerType();
        com.ozerler.marble.model.enums.BusinessUnit bu = customer.getBusinessUnit();
        int pCount;
        if (customer.getProjects() != null) {
            pCount = customer.getProjects().size();
        } else {
            pCount = 0;
        }
        String customerTypeValue;
        if (type != null) {
            customerTypeValue = type.name();
        } else {
            customerTypeValue = null;
        }
        String customerTypeLabelValue;
        if (type != null) {
            customerTypeLabelValue = type.getLabel();
        } else {
            customerTypeLabelValue = "";
        }
        String businessUnitValue;
        if (bu != null) {
            businessUnitValue = bu.name();
        } else {
            businessUnitValue = null;
        }
        String businessUnitLabelValue;
        if (bu != null) {
            businessUnitLabelValue = bu.getDisplayName();
        } else {
            businessUnitLabelValue = "";
        }
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
                .customerType(customerTypeValue)
                .customerTypeLabel(customerTypeLabelValue)
                .businessUnit(businessUnitValue)
                .businessUnitLabel(businessUnitLabelValue)
                .notes(customer.getNotes())
                .projectCount(pCount)
                .build();
    }
}
