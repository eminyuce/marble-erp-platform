package com.ozerler.marble.dto.form;

import java.math.BigDecimal;

import com.ozerler.marble.dto.view.AuditView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuarryForm extends AuditView {
    private Long id;
    private String code;
    private String name;
    private String location;
    private BigDecimal specificGravity = new BigDecimal("2.70");
    private String licenseNo;
}
