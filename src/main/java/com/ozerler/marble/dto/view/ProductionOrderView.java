package com.ozerler.marble.dto.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.ozerler.marble.model.enums.ProcessType;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.ScrapReasonCode;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.SurfaceFinish;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductionOrderView extends AuditView {
    private Long id;
    private String orderNo;
    private String status;
    private String statusLabel;
    private ProcessType processType;
    private String machineName;
    private String operatorName;
    private String notes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal durationHours;
    private BigDecimal electricityKwh;
    private BigDecimal bladeWearMm;
    private BlockView block;
    private List<SlabLine> slabs;
    private List<ScrapLine> scrapLogs;

    @Getter
    @Setter
    public static class SlabLine {
        private Long id;
        private String slabCode;
        private BigDecimal thicknessCm;
        private BigDecimal widthCm;
        private BigDecimal lengthCm;
        private BigDecimal surfaceAreaM2;
        private BigDecimal costPerM2;
        private QualityGrade qualityGrade;
        private SurfaceFinish surfaceFinish;
        private SlabStatus status;
    }

    @Getter
    @Setter
    public static class ScrapLine {
        private String scrapCode;
        private ScrapReasonCode reasonCode;
        private String description;
        private BigDecimal scrapWeightKg;
        private BigDecimal costImpact;
        private String loggedBy;
    }
}
