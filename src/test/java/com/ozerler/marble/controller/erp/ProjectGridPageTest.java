package com.ozerler.marble.controller.erp;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ozerler.marble.dto.ProjectDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.service.ConstructionSiteService;
import com.ozerler.marble.service.CostAnalysisService;
import com.ozerler.marble.service.FormLookupService;
import com.ozerler.marble.service.ProjectSiteService;
import com.ozerler.marble.web.DataGridView;

@ExtendWith(MockitoExtension.class)
class ProjectGridPageTest {

    @Mock
    private FormLookupService formLookup;
    @Mock
    private ProjectSiteService projectSiteService;
    @Mock
    private ConstructionSiteService constructionSiteService;
    @Mock
    private CostAnalysisService costAnalysisService;
    @Mock
    private MessageSource messageSource;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProjectController controller = new ProjectController(
                formLookup,
                projectSiteService,
                constructionSiteService,
                costAnalysisService,
                messageSource);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Project list keeps page, page size, and total from the paged service")
    void indexExposesPageSizeAndTotal() throws Exception {
        ProjectDto project = ProjectDto.builder()
                .id(18067L)
                .projectCode("PRJ-18")
                .name("Ada")
                .statusLabel("Onaylı")
                .build();
        Mockito.when(projectSiteService.getProjectsPaged(2, 50, "ada", "ACTIVE", 9L, "name", "asc"))
                .thenReturn(TabulatorResponse.of(List.of(project), 8, 336));
        Mockito.when(formLookup.customersByCompanyName()).thenReturn(List.of());
        Mockito.when(formLookup.findCustomer(9L)).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.get("/projects")
                        .param("page", "2")
                        .param("size", "50")
                        .param("search", "ada")
                        .param("status", "ACTIVE")
                        .param("customerId", "9")
                        .param("sort", "name")
                        .param("dir", "asc"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.model().attribute("grid", Matchers.allOf(
                        Matchers.instanceOf(DataGridView.class),
                        Matchers.hasProperty("number", Matchers.is(2)),
                        Matchers.hasProperty("size", Matchers.is(50)),
                        Matchers.hasProperty("total", Matchers.is(336L)),
                        Matchers.hasProperty("totalPages", Matchers.is(8))
                )));
    }

    @Test
    @DisplayName("An unsupported page size falls back to 20 and resets the service call")
    void unsupportedSizeFallsBackToTwenty() throws Exception {
        Mockito.when(projectSiteService.getProjectsPaged(1, 20, null, null, null, null, "desc"))
                .thenReturn(TabulatorResponse.of(List.of(), 1, 0));
        Mockito.when(formLookup.customersByCompanyName()).thenReturn(List.of());

        mockMvc.perform(MockMvcRequestBuilders.get("/projects").param("size", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.model().attribute("grid", Matchers.hasProperty("size", Matchers.is(20))))
                .andExpect(MockMvcResultMatchers.model().attribute("grid", Matchers.hasProperty("total", Matchers.is(0L))));
    }
}
