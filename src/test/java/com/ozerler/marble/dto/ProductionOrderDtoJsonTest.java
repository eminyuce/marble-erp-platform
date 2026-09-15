package com.ozerler.marble.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.enums.ProcessType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionOrderDtoJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("ProductionOrderDto keeps status as the API code and adds status_label")
    void serializesStatusCodeAndTurkishLabel() throws Exception {
        ProductionOrderDto dto = ProductionOrderDto.builder()
                .id(1L)
                .orderNo("PRD-2026-0001")
                .status("COMPLETED")
                .statusLabel("Tamamlandı")
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        assertThat(json.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(json.path("status_label").asText()).isEqualTo("Tamamlandı");
        assertThat(json.has("statusLabel")).isFalse();
    }

    @Test
    @DisplayName("fromEntity maps COMPLETED to the Turkish status label")
    void fromEntity_mapsTurkishStatusLabel() {
        ProductionOrder order = ProductionOrder.builder()
                .id(1L)
                .orderNo("PRD-2026-0001")
                .block(Block.builder().id(2L).blockCode("BLK-001").stoneType("Burdur Bej").build())
                .machineName("Katrak-01")
                .processType(ProcessType.GANGSAW)
                .startTime(LocalDateTime.of(2026, 3, 1, 8, 0))
                .status("COMPLETED")
                .build();

        ProductionOrderDto dto = ProductionOrderDto.fromEntity(order);

        assertThat(dto.getStatus()).isEqualTo("COMPLETED");
        assertThat(dto.getStatusLabel()).isEqualTo("Tamamlandı");
        assertThat(dto.getProcessLabel()).isEqualTo("Katrak");
    }
}
