package com.ozerler.marble.dto.view;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.PurchaseItemType;
import com.ozerler.marble.model.enums.PurchaseOrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class PurchaseOrderView {
    private Long id;
    private String poNumber;
    private LocalDate orderDate;
    private PurchaseOrderStatus status;
    private BusinessUnit businessUnit;
    private BigDecimal totalAmount;
    private PartyView supplier;
    private ProjectView project;
    private CostCenterRef costCenter;
    private List<LineView> items;

    @Getter
    @Setter
    public static class LineView {
        private String itemName;
        private PurchaseItemType itemType;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}
