package com.ozerler.marble.model.enums;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum QualityGrade {
    EXTRA("Ekstra", new BigDecimal("1.30")),
    A("A Kalite", new BigDecimal("1.15")),
    B("B Kalite", new BigDecimal("1.00")),
    C("C Kalite", new BigDecimal("0.65")),
    MOLOZ("Moloz / Düşük", new BigDecimal("0.30"));

    private final String label;
    private final BigDecimal multiplier;

    QualityGrade(String label, BigDecimal multiplier) {
        this.label = label;
        this.multiplier = multiplier;
    }
}
