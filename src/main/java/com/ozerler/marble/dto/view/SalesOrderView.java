package com.ozerler.marble.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.ozerler.marble.model.enums.SalesOrderStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SalesOrderView extends AuditView {
    private Long id;
    private String orderNo;
    private LocalDate orderDate;
    private LocalDate deliveryDate;
    private String notes;
    private SalesOrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private PartyView customer;
    private List<LineView> items;

    @Getter
    @Setter
    public static class LineView {
        private Long id;
        private String description;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private BlockRef block;
        private SlabRef slab;
        private String orderNo;
        private Long customerId;
    }
}
