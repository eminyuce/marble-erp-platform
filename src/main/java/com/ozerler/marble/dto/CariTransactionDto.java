package com.ozerler.marble.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.ozerler.marble.model.enums.CariTransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariTransactionDto {

    private Long id;
    private Long customerId;
    private String customerCode;
    private String customerName;

    private LocalDate transactionDate;
    private String formattedDate;

    private CariTransactionType transactionType;
    private String transactionTypeLabel;
    private boolean debit;

    private String documentNo;
    private String description;

    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal runningBalance;

    private Long invoiceId;
    private Long collectionId;
    private String notes;
}
