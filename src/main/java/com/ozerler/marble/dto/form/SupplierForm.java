package com.ozerler.marble.dto.form;

import com.ozerler.marble.dto.view.AuditView;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.SupplierType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierForm extends AuditView {
    private Long id;
    private String supplierCode;
    private SupplierType supplierType = SupplierType.CONSUMABLE;
    private BusinessUnit businessUnit;
    private String companyName;
    private String contactPerson;
    private String phone;
    private String email;
    private String taxOffice;
    private String taxNumber;
    private String address;
    private String notes;
}
