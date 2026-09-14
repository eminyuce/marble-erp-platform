package com.ozerler.marble.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

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
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        assertThat(json.path("cut_order_no").asText()).isEqualTo("CUT-2026-0001");
        assertThat(json.path("project_name").asText()).isEqualTo("Villa");
        assertThat(json.path("item_count").asInt()).isEqualTo(4);
        assertThat(json.path("total_area_m2").decimalValue()).isEqualByComparingTo("3.8400");
        assertThat(json.has("cutOrderNo")).isFalse();
        assertThat(json.has("projectName")).isFalse();
        assertThat(json.has("itemCount")).isFalse();
        assertThat(json.has("totalAreaM2")).isFalse();
    }
}
