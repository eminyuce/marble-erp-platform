package com.ozerler.marble.dto.view;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CustomerType;
import com.ozerler.marble.model.enums.SupplierType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartyView {
    private Long id;
    private String companyName;
    private String phone;
    private String customerCode;
    private String email;
    private String taxNumber;
    private String taxOffice;
    private CustomerType customerType;
    private SupplierType supplierType;
    private BusinessUnit businessUnit;
}
