package com.ozerler.marble.dto.view;

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
    private java.math.BigDecimal amount;
    private String bankName;
    private java.time.LocalDate collectionDate;
    private com.ozerler.marble.model.enums.CollectionMethod collectionMethod;
    private PartyView customer;
    private InvoiceRef invoice;
    private String notes;
}
