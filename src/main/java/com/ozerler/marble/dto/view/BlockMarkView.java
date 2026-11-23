package com.ozerler.marble.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.ozerler.marble.model.enums.BlockMarkStatus;

import lombok.Getter;
import lombok.Setter;

/**
 * A customer reservation mark on a block, as shown on the block detail and sell screens.
 */
@Getter
@Setter
public class BlockMarkView {
    private Long id;
    private PartyView customer;
    private LocalDate markedAt;
    private LocalDate validUntil;
    private BigDecimal offerPrice;
    private String currency;
    private BlockMarkStatus status;

    public String getCustomerName() {
        return ViewLabels.companyName(customer, "Müşteri");
    }
}
