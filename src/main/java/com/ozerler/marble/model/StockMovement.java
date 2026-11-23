package com.ozerler.marble.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockMovementType;
import com.ozerler.marble.model.enums.TargetDestination;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class StockMovement extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 0;

    @Column(name = "movement_code", nullable = false, unique = true, length = 60)
    private String movementCode;

    @Column(name = "movement_date", nullable = false)
    @Builder.Default
    private LocalDateTime movementDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 40)
    private StockMovementType movementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_department", length = 40)
    private BusinessUnit sourceDepartment;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_department", length = 40)
    private BusinessUnit targetDepartment;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_destination", length = 40)
    private TargetDestination targetDestination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_location_id")
    private StockLocation fromLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_location_id")
    private StockLocation toLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slab_id")
    private Slab slab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_item_id")
    private StockItem stockItem;

    @Column(name = "item_description", length = 255)
    private String itemDescription;

    @Column(precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "quantity_unit", length = 30)
    @Builder.Default
    private String quantityUnit = "m2";

    @Column(precision = 10, scale = 3)
    private BigDecimal tonnage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id")
    private OperationWorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Transient
    public String getItemDisplayName() {
        if (block != null) {
            return "Blok: " + block.getBlockCode();
        }
        if (slab != null) {
            return "Plaka: " + slab.getSlabCode();
        }
        if (stockItem != null) {
            return "Ürün: " + stockItem.getItemCode();
        }
        if (itemDescription != null) {
            return itemDescription;
        }
        return "—";
    }

    @Transient
    public String getSourceDisplayName() {
        if (fromLocation != null) {
            return fromLocation.getName();
        }
        if (sourceDepartment != null) {
            return sourceDepartment.getLabel();
        }
        return "—";
    }

    @Transient
    public String getTargetDisplayName() {
        if (toLocation != null) {
            return toLocation.getName();
        }
        if (targetDestination != null) {
            if (targetDestination == TargetDestination.CUSTOMER && customer != null) {
                return "Müşteri: " + customer.getCompanyName();
            }
            return targetDestination.getLabel();
        }
        if (targetDepartment != null) {
            return targetDepartment.getLabel();
        }
        return "—";
    }
}
