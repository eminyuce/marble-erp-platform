package com.ozerler.marble.dto;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

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

        Assertions.assertThat(json).contains("\"project_code\":\"PRJ-2026-0001\"");
        Assertions.assertThat(json).doesNotContain("\"projectCode\"");
    }

    @Test
    @DisplayName("Projects grid camelizes API rows so Tabulator can read projectCode")
    void projectsGridCamelizesApiRows() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/projects/index.html")) {
            Assertions.assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        Assertions.assertThat(html).contains("fragments/data-grid :: shell");
        try (var app = getClass().getResourceAsStream("/static/js/app.js")) {
            Assertions.assertThat(app).isNotNull();
            Assertions.assertThat(new String(app.readAllBytes())).contains("camelizeTabulatorRows");
        }
    }
}
