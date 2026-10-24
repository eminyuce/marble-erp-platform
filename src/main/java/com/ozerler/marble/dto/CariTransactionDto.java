package com.ozerler.marble.dto;

import com.ozerler.marble.model.enums.CariTransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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
