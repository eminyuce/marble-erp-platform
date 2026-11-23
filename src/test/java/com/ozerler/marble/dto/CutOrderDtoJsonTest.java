package com.ozerler.marble.dto;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozerler.marble.model.CutOrder;

class CutOrderDtoJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("CutOrderDto serializes workshop grid fields as snake_case")
    void serializesWorkshopFieldsAsSnakeCase() throws Exception {
        CutOrderDto dto = CutOrderDto.builder()
                .id(1L)
                .cutOrderNo("CUT-2026-0001")
                .projectName("Villa")
                .locationName("Lobi")
                .machineName("Köprü-01")
                .operatorName("Ahmet")
                .itemCount(4)
                .totalAreaM2(new BigDecimal("3.8400"))
                .status("COMPLETED")
                .statusLabel("Tamamlandı")
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        Assertions.assertThat(json.path("cut_order_no").asText()).isEqualTo("CUT-2026-0001");
        Assertions.assertThat(json.path("project_name").asText()).isEqualTo("Villa");
        Assertions.assertThat(json.path("item_count").asInt()).isEqualTo(4);
        Assertions.assertThat(json.path("total_area_m2").decimalValue()).isEqualByComparingTo("3.8400");
        Assertions.assertThat(json.path("status").asText()).isEqualTo("COMPLETED");
        Assertions.assertThat(json.path("status_label").asText()).isEqualTo("Tamamlandı");
        Assertions.assertThat(json.has("cutOrderNo")).isFalse();
        Assertions.assertThat(json.has("statusLabel")).isFalse();
        Assertions.assertThat(json.has("projectName")).isFalse();
        Assertions.assertThat(json.has("itemCount")).isFalse();
        Assertions.assertThat(json.has("totalAreaM2")).isFalse();
    }

    @Test
    @DisplayName("fromEntity maps COMPLETED to the Turkish status label")
    void fromEntityMapsTurkishStatusLabel() {
        CutOrderDto dto = CutOrderDto.fromEntity(
                CutOrder.builder()
                        .id(1L)
                        .cutOrderNo("CUT-2026-0001")
                        .status("COMPLETED")
                        .build());

        Assertions.assertThat(dto.getStatus()).isEqualTo("COMPLETED");
        Assertions.assertThat(dto.getStatusLabel()).isEqualTo("Tamamlandı");
    }
}
