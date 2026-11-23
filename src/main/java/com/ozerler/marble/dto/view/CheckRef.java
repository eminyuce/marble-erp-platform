package com.ozerler.marble.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CheckStatus;

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
    private BigDecimal amount;
    private String bankName;
    private LocalDate dueDate;
    private PartyView customer;
    private BusinessUnit effectiveBusinessUnit;
    private CheckStatus status;
}
