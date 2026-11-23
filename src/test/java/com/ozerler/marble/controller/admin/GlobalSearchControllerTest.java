package com.ozerler.marble.controller.admin;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.GlobalSearchResponse;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.service.GlobalSearchService;

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
        Mockito.when(globalSearchService.search(query)).thenReturn(dummy);

        BackEndResponse response = globalSearchController.search(query);

        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        Assertions.assertThat(response.getResponse().getBody()).isEqualTo(dummy);
    }

    @Test
    @DisplayName("search should return fatal BackEndResponse on search failure")
    void shouldReturnFatalBackEndResponseOnError() {
        String query = "AFYON";
        Mockito.when(globalSearchService.search(query)).thenThrow(new RuntimeException("Search timeout"));

        BackEndResponse response = globalSearchController.search(query);

        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.ERR_FATAL);
    }
}
