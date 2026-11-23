package com.ozerler.marble.dto.form;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QuarryForm {
    private Long id;
    private String code;
    private String name;
    private String location;
    private BigDecimal specificGravity = new BigDecimal("2.70");
    private String licenseNo;
}
