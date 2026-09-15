package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.Supplier;
import com.ozerler.marble.model.enums.SupplierType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("supplierCode")
    @JsonAlias("supplier_code")
    private String supplierCode;

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

    @JsonProperty("supplierType")
    @JsonAlias("supplier_type")
    private String supplierType;

    @JsonProperty("supplierTypeLabel")
    @JsonAlias("supplier_type_label")
    private String supplierTypeLabel;

    @JsonProperty("notes")
    private String notes;

    public static SupplierDto fromEntity(Supplier supplier) {
        SupplierType type = supplier.getSupplierType();
        return SupplierDto.builder()
                .id(supplier.getId())
                .supplierCode(supplier.getSupplierCode())
                .companyName(supplier.getCompanyName())
                .contactPerson(supplier.getContactPerson())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .taxOffice(supplier.getTaxOffice())
                .taxNumber(supplier.getTaxNumber())
                .supplierType(type != null ? type.name() : null)
                .supplierTypeLabel(type != null ? type.getLabel() : "")
                .notes(supplier.getNotes())
                .build();
    }
}
