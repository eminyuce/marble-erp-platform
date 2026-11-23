package com.ozerler.marble.config;

import java.io.IOException;

import jakarta.servlet.ServletException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.ozerler.marble.common.Constants;

class MdcLoggingFilterTest {

    @Test
    @DisplayName("an incoming trace id is stored as both traceId and correlationId and then cleared")
    void traceIdAndCorrelationIdMatch() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(Constants.TRACE_ID_HEADER, "trace-from-caller");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new MdcLoggingFilter().doFilter(request, response, (req, res) -> {
            Assertions.assertThat(MDC.get(Constants.MDC_TRACE_ID)).isEqualTo("trace-from-caller");
            Assertions.assertThat(MDC.get(Constants.MDC_CORRELATION_ID)).isEqualTo("trace-from-caller");
        });

        Assertions.assertThat(response.getHeader(Constants.CORRELATION_ID_HEADER)).isEqualTo("trace-from-caller");
        Assertions.assertThat(response.getHeader(Constants.TRACE_ID_HEADER)).isEqualTo("trace-from-caller");
        Assertions.assertThat(MDC.get(Constants.MDC_TRACE_ID)).isNull();
        Assertions.assertThat(MDC.get(Constants.MDC_CORRELATION_ID)).isNull();
    }

    @Test
    @DisplayName("a missing id is generated once and not left on the thread")
    void generatesAndClears() throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new MdcLoggingFilter().doFilter(new MockHttpServletRequest(), response, (req, res) -> {
            Assertions.assertThat(MDC.get(Constants.MDC_TRACE_ID)).isNotBlank();
            Assertions.assertThat(MDC.get(Constants.MDC_TRACE_ID)).isEqualTo(MDC.get(Constants.MDC_CORRELATION_ID));
        });
        Assertions.assertThat(response.getHeader(Constants.TRACE_ID_HEADER)).isNotBlank();
        Assertions.assertThat(MDC.getCopyOfContextMap()).isNull();
    }
}
