package com.ozerler.marble.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.ozerler.marble.model.enums.CollectionMethod;

import lombok.Getter;
import lombok.Setter;

/**
 * Id and document number of a saved record, for confirmation messages.
 */
@Getter
@Setter
public class CollectionRef {
    private Long id;
    private String collectionNo;
    private BigDecimal amount;
    private String bankName;
    private LocalDate collectionDate;
    private CollectionMethod collectionMethod;
    private PartyView customer;
    private InvoiceRef invoice;
    private String notes;
}
