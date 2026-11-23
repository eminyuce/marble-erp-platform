package com.ozerler.marble.dto.form;

import java.math.BigDecimal;

import com.ozerler.marble.model.enums.BusinessUnit;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CostCenterForm extends com.ozerler.marble.dto.view.AuditView {
    private Long id;
    private String code;
    private String name;
    private BusinessUnit businessUnit = BusinessUnit.FACTORY;
    private BigDecimal monthlyBudget = BigDecimal.ZERO;
    private String description;
}
