package com.ozerler.marble.dto.view;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceRef {
    private Long id;
    private String invoiceNo;
    private BigDecimal totalAmount;
}
