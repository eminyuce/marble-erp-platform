package com.ozerler.marble.dto.view;

import java.math.BigDecimal;

import com.ozerler.marble.model.enums.ConsumptionType;
import com.ozerler.marble.model.enums.SupplyRoute;

import lombok.Getter;
import lombok.Setter;

public final class SiteViews {
    private SiteViews() {
    }

    @Getter
    @Setter
    public static class LocationView {
        private Long id;
        private String locationName;
        private String stoneSpec;
        private String status;
        private String statusLabel;
        private BigDecimal installedAreaM2;
        private BigDecimal plannedAreaM2;
        private BigDecimal progressPercentage;
    }

    @Getter
    @Setter
    public static class ConsumptionView {
        private String itemName;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitCost;
        private BigDecimal totalCost;
        private ConsumptionType consumptionType;
        private LocationView location;
    }

    @Getter
    @Setter
    public static class PlanView {
        private String stoneType;
        private BigDecimal plannedAreaM2;
        private BigDecimal requiredAreaM2;
        private SupplyRoute supplyRoute;
        private LocationView location;
    }

    @Getter
    @Setter
    public static class InstallationView {
        private LocationView location;
        private BigDecimal installedAreaM2;
        private BigDecimal wasteAreaM2;
    }
}
