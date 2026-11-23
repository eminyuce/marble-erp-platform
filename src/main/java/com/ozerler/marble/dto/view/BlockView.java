package com.ozerler.marble.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.StockLocationType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockView extends AuditView {
    private Long id;
    private String blockCode;
    private String stoneType;
    private String colorTone;
    private String quarrySection;
    private String dimensions;
    private Integer length;
    private Integer width;
    private Integer height;
    private Integer lengthCm;
    private Integer widthCm;
    private Integer heightCm;
    private BigDecimal volumeM3;
    private BigDecimal actualWeightKg;
    private BigDecimal weightDeviationPct;
    private BigDecimal actualTonnage;
    private BigDecimal estimatedTonnage;
    private BigDecimal approximateTonnage;
    private BigDecimal weightTons;
    private BigDecimal totalCost;
    private BigDecimal transportCost;
    private LocalDate extractionDate;
    private LocalDate arrivalDate;
    private BigDecimal salePrice;
    private String quarryName;
    private BlockStatus status;
    private BlockStatus canonicalStatus;
    private QualityGrade qualityGrade;
    private Integer crackLevel;
    private String notes;
    private String photoUrls;
    private BigDecimal theoreticalWeightKg;
    private BigDecimal unitMarketValuePerTon;
    private PartyView assignedCustomer;
    private QuarryRef quarry;
    private PartyView soldCustomer;
    private LocationView currentLocation;

    @Getter
    @Setter
    public static class LocationView {
        private String name;
        private StockLocationType locationType;
    }
}
