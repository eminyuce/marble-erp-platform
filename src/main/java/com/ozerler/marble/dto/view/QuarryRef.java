package com.ozerler.marble.dto.view;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuarryRef {
    private Long id;
    private String code;
    private String name;
    private String location;
    private BigDecimal specificGravity;
}
