package com.ozerler.marble.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class Shipment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "waybill_no", nullable = false, unique = true, length = 50)
    private String waybillNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "vehicle_plate", nullable = false, length = 30)
    private String vehiclePlate;

    @Column(name = "driver_name", nullable = false, length = 100)
    private String driverName;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "distance_km", nullable = false)
    @Builder.Default
    private Integer distanceKm = 0;

    @Column(name = "freight_cost", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal freightCost = BigDecimal.ZERO;

    @Column(name = "delivery_status", nullable = false, length = 30)
    @Builder.Default
    private String deliveryStatus = "IN_TRANSIT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Transient
    public String getDeliveryStatusLabel() {
        return com.ozerler.marble.model.enums.ShipmentDeliveryStatus.labelOf(deliveryStatus);
    }
}
