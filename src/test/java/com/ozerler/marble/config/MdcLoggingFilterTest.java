package com.ozerler.marble.config;

import com.ozerler.marble.common.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MdcLoggingFilterTest {

    private MdcLoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new MdcLoggingFilter();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("doFilterInternal sets correlationId header and populates MDC during request execution")
    void doFilterInternal_GeneratesCorrelationId_AndCleansUp() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> {
            // Verify MDC entries are present during request execution
            assertThat(MDC.get(Constants.MDC_CORRELATION_ID)).isNotBlank();
            assertThat(MDC.get(Constants.MDC_HTTP_METHOD)).isEqualTo("GET");
            assertThat(MDC.get(Constants.MDC_REQUEST_URI)).isEqualTo("/api/test");
        };

        filter.doFilterInternal(request, response, filterChain);

        // Verify response header contains X-Correlation-ID
        assertThat(response.getHeader(Constants.CORRELATION_ID_HEADER)).isNotBlank();

        // Verify MDC was cleaned up after request
        assertThat(MDC.get(Constants.MDC_CORRELATION_ID)).isNull();
    }

    @Test
    @DisplayName("doFilterInternal preserves existing X-Correlation-ID header from incoming request")
    void doFilterInternal_PreservesExistingCorrelationId() throws ServletException, IOException {
        String existingCorrelationId = "custom-trace-id-12345";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(Constants.CORRELATION_ID_HEADER, existingCorrelationId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> {
            assertThat(MDC.get(Constants.MDC_CORRELATION_ID)).isEqualTo(existingCorrelationId);
        };

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader(Constants.CORRELATION_ID_HEADER)).isEqualTo(existingCorrelationId);
        assertThat(MDC.get(Constants.MDC_CORRELATION_ID)).isNull();
    }
}
