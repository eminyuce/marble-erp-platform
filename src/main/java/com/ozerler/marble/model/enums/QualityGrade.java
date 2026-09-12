package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;
import lombok.Getter;

import java.math.BigDecimal;

public enum QualityGrade {
    EXTRA("enum.quality_grade.extra", new BigDecimal("1.30")),
    A("enum.quality_grade.a", new BigDecimal("1.15")),
    B("enum.quality_grade.b", new BigDecimal("1.00")),
    C("enum.quality_grade.c", new BigDecimal("0.65")),
    MOLOZ("enum.quality_grade.moloz", new BigDecimal("0.30"));

    private final String messageKey;
    @Getter
    private final BigDecimal multiplier;

    QualityGrade(String messageKey, BigDecimal multiplier) {
        this.messageKey = messageKey;
        this.multiplier = multiplier;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
