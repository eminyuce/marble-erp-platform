package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.Quarry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuarryDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("location")
    private String location;

    @JsonProperty("specificGravity")
    @JsonAlias("specific_gravity")
    private BigDecimal specificGravity;

    @JsonProperty("licenseNo")
    @JsonAlias("license_no")
    private String licenseNo;

    public static QuarryDto fromEntity(Quarry quarry) {
        return QuarryDto.builder()
                .id(quarry.getId())
                .code(quarry.getCode())
                .name(quarry.getName())
                .location(quarry.getLocation())
                .specificGravity(quarry.getSpecificGravity())
                .licenseNo(quarry.getLicenseNo())
                .build();
    }
}
