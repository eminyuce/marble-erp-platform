package com.ozerler.marble.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.SurfaceFinish;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(json.path("status").asText()).isEqualTo("AVAILABLE");
        assertThat(json.path("statusLabel").asText()).isEqualTo("Serbest Stok");
        assertThat(json.path("surfaceFinish").asText()).isEqualTo("RAW");
        assertThat(json.path("surfaceFinishLabel").asText()).isEqualTo("Ham (Testere Çıkışı)");
        assertThat(json.path("qualityGrade").asText()).isEqualTo("EXTRA");
        assertThat(json.path("qualityGradeLabel").asText()).isEqualTo("Ekstra");
    }

    @Test
    @DisplayName("fromEntity maps slab enum names to Turkish labels")
    void fromEntity_mapsTurkishLabels() {
        Slab slab = Slab.builder()
                .id(1L)
                .slabCode("SLB-2026-0001")
                .surfaceFinish(SurfaceFinish.RAW)
                .qualityGrade(QualityGrade.EXTRA)
                .status(SlabStatus.AVAILABLE)
                .build();

        SlabDto dto = SlabDto.fromEntity(slab);

        assertThat(dto.getStatus()).isEqualTo("AVAILABLE");
        assertThat(dto.getStatusLabel()).isEqualTo("Serbest Stok");
        assertThat(dto.getSurfaceFinish()).isEqualTo("RAW");
        assertThat(dto.getSurfaceFinishLabel()).isEqualTo("Ham (Testere Çıkışı)");
        assertThat(dto.getQualityGrade()).isEqualTo("EXTRA");
        assertThat(dto.getQualityGradeLabel()).isEqualTo("Ekstra");
    }
}
