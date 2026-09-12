package com.ozerler.marble.controller.admin;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.GlobalSearchResponse;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.service.GlobalSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSearchControllerTest {

    @Mock
    private GlobalSearchService globalSearchService;

    @InjectMocks
    private GlobalSearchController globalSearchController;

    @Test
    @DisplayName("search should return success BackEndResponse with search payload")
    void shouldReturnBackEndResponseOnSearch() {
        String query = "AFYON";
        GlobalSearchResponse dummy = GlobalSearchResponse.empty(query);
        when(globalSearchService.search(query)).thenReturn(dummy);

        BackEndResponse response = globalSearchController.search(query);

        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        assertThat(response.getResponse().getBody()).isEqualTo(dummy);
    }

    @Test
    @DisplayName("search should return fatal BackEndResponse on search failure")
    void shouldReturnFatalBackEndResponseOnError() {
        String query = "AFYON";
        when(globalSearchService.search(query)).thenThrow(new RuntimeException("Search timeout"));

        BackEndResponse response = globalSearchController.search(query);

        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.ERR_FATAL);
    }
}
