package com.ozerler.marble.dto.view;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuarryRef {
    private Long id;
    private String code;
    private String name;
    private String location;
    private java.math.BigDecimal specificGravity;
}
