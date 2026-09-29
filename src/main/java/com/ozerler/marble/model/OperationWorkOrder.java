package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.OperationWorkOrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "operation_work_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class OperationWorkOrder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 60)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "order_date", nullable = false)
    @Builder.Default
    private LocalDate orderDate = LocalDate.now();

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private BusinessUnit department = BusinessUnit.FACTORY;

    @Column(name = "responsible_person", length = 120)
    private String responsiblePerson;

    @Column(name = "stone_type", length = 100)
    private String stoneType;

    @Column(name = "color_quality", length = 100)
    private String colorQuality;

    @Column(name = "thickness_cm", precision = 8, scale = 2)
    private BigDecimal thicknessCm;

    @Column(name = "width_cm", precision = 10, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "length_cm", precision = 10, scale = 2)
    private BigDecimal lengthCm;

    @Column(precision = 12, scale = 4)
    private BigDecimal quantity;

    @Column(name = "quantity_unit", length = 20)
    @Builder.Default
    private String quantityUnit = "m2";

    @Column(name = "surface_operation", length = 60)
    private String surfaceOperation;

    @Column(name = "edge_operation", length = 60)
    private String edgeOperation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OperationWorkOrderStatus status = OperationWorkOrderStatus.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_stock_item_id")
    private StockItem sourceStockItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_slab_id")
    private Slab sourceSlab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_block_id")
    private Block sourceBlock;

    @Column(name = "used_quantity", precision = 12, scale = 4)
    private BigDecimal usedQuantity;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("changedAt DESC")
    @Builder.Default
    private List<WorkOrderStatusHistory> statusHistory = new ArrayList<>();

    @Transient
    public String getCustomerDisplayName() {
        return customer != null ? customer.getCompanyName() : "Genel Sipariş";
    }

    @Transient
    public String getDepartmentDisplayName() {
        if (department == BusinessUnit.FACTORY) return "Fabrika";
        if (department == BusinessUnit.WORKSHOP) return "Atölye";
        if (department == BusinessUnit.SITE) return "Şantiye";
        return department.name();
    }

    @Transient
    public String getOrderNumber() {
        return orderNo;
    }

    @Transient
    public BigDecimal getPlannedQuantity() {
        return quantity;
    }

    @Transient
    public List<WorkOrderStatusHistory> getStatusHistories() {
        return statusHistory;
    }

    @Transient
    public String getWorkOrderNo() {
        return orderNo;
    }

    @Transient
    public LocalDate getTargetDate() {
        return dueDate;
    }

    @Transient
    public String getStoneColorQuality() {
        return colorQuality;
    }

    @Transient
    public BigDecimal getStoneThicknessCm() {
        return thicknessCm;
    }

    @Transient
    public String getUnit() {
        return quantityUnit;
    }
}
