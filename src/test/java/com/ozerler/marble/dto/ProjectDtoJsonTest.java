package com.ozerler.marble.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectDtoJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("ProjectDto serializes project code as project_code for the grid API")
    void serializesProjectCodeAsSnakeCase() throws Exception {
        ProjectDto dto = ProjectDto.builder()
                .id(1L)
                .projectCode("PRJ-2026-0001")
                .name("Konut Projesi")
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"project_code\":\"PRJ-2026-0001\"");
        assertThat(json).doesNotContain("\"projectCode\"");
    }

    @Test
    @DisplayName("Projects grid camelizes API rows so Tabulator can read projectCode")
    void projectsGridCamelizesApiRows() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/projects/index.html")) {
            assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        assertThat(html).contains("erpGridAjaxResponse(");
        assertThat(html).contains("field: \"projectCode\"");
    }
}
