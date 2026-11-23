package com.ozerler.marble.dto.view;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvoiceRef {
    private Long id;
    private String invoiceNo;
    private BigDecimal totalAmount;
}
