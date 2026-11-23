package com.ozerler.marble.dto.form;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CustomerType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerForm {
    private Long id;
    private String customerCode;
    private CustomerType customerType = CustomerType.CONSTRUCTION;
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
