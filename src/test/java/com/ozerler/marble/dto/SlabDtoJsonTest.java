package com.ozerler.marble.dto;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.SurfaceFinish;

class SlabDtoJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("SlabDto keeps enum codes and serializes Turkish labels for the grid")
    void serializesCodesAndTurkishLabels() throws Exception {
        SlabDto dto = SlabDto.builder()
                .id(1L)
                .slabCode("SLB-2026-0001")
                .surfaceFinish("RAW")
                .surfaceFinishLabel("Ham (Testere Çıkışı)")
                .qualityGrade("EXTRA")
                .qualityGradeLabel("Ekstra")
                .status("AVAILABLE")
                .statusLabel("Serbest Stok")
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        Assertions.assertThat(json.path("status").asText()).isEqualTo("AVAILABLE");
        Assertions.assertThat(json.path("statusLabel").asText()).isEqualTo("Serbest Stok");
        Assertions.assertThat(json.path("surfaceFinish").asText()).isEqualTo("RAW");
        Assertions.assertThat(json.path("surfaceFinishLabel").asText()).isEqualTo("Ham (Testere Çıkışı)");
        Assertions.assertThat(json.path("qualityGrade").asText()).isEqualTo("EXTRA");
        Assertions.assertThat(json.path("qualityGradeLabel").asText()).isEqualTo("Ekstra");
    }

    @Test
    @DisplayName("fromEntity maps slab enum names to Turkish labels")
    void fromEntityMapsTurkishLabels() {
        Slab slab = Slab.builder()
                .id(1L)
                .slabCode("SLB-2026-0001")
                .surfaceFinish(SurfaceFinish.RAW)
                .qualityGrade(QualityGrade.EXTRA)
                .status(SlabStatus.AVAILABLE)
                .build();

        SlabDto dto = SlabDto.fromEntity(slab);

        Assertions.assertThat(dto.getStatus()).isEqualTo("AVAILABLE");
        Assertions.assertThat(dto.getStatusLabel()).isEqualTo("Serbest Stok");
        Assertions.assertThat(dto.getSurfaceFinish()).isEqualTo("RAW");
        Assertions.assertThat(dto.getSurfaceFinishLabel()).isEqualTo("Ham (Testere Çıkışı)");
        Assertions.assertThat(dto.getQualityGrade()).isEqualTo("EXTRA");
        Assertions.assertThat(dto.getQualityGradeLabel()).isEqualTo("Ekstra");
    }
}
