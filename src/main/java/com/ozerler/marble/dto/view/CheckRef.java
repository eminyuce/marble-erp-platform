package com.ozerler.marble.dto.view;

import lombok.Getter;
import lombok.Setter;

/**
 * Id and document number of a saved record, for confirmation messages.
 */
@Getter
@Setter
public class CheckRef {
    private Long id;
    private String checkNo;
    private java.math.BigDecimal amount;
    private String bankName;
    private java.time.LocalDate dueDate;
    private PartyView customer;
    private com.ozerler.marble.model.enums.BusinessUnit effectiveBusinessUnit;
    private com.ozerler.marble.model.enums.CheckStatus status;
}
