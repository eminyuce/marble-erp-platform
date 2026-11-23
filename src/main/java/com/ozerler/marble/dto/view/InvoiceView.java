package com.ozerler.marble.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CollectionMethod;
import com.ozerler.marble.model.enums.InvoiceStatus;
import com.ozerler.marble.model.enums.InvoiceType;
import com.ozerler.marble.model.enums.QuarryCategory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceView {
    private Long id;
    private String invoiceNumber;
    private String invoiceNo;
    private LocalDate invoiceDate;
    private InvoiceType invoiceType;
    private InvoiceStatus status;
    private BusinessUnit department;
    private BusinessUnit targetDepartment;
    private QuarryCategory quarryCategory;
    private boolean internalTransfer;
    private Boolean directExpense;
    private Boolean stockProcessed;
    private String partyName;
    private String notes;
    private BigDecimal subtotalAmount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private PartyView customer;
    private PartyView supplier;
    private ProjectView project;
    private List<LineView> items;
    private List<CollectionView> collections;

    @Getter
    @Setter
    public static class LineView {
        private String productName;
        private String description;
        private BigDecimal quantity;
        private BigDecimal wasteQuantity;
        private String unitLabel;
        private BigDecimal widthCm;
        private BigDecimal calculatedM2;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private BlockRef block;
        private SlabRef slab;
        private StockRef stockItem;
    }

    @Getter
    @Setter
    public static class CollectionView {
        private CollectionMethod method;
        private BigDecimal amount;
        private LocalDate collectionDate;
    }

    public String getCustomerName() {
        return ViewLabels.companyName(customer, "");
    }
}
