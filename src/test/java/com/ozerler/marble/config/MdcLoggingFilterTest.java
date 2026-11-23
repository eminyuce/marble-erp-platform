package com.ozerler.marble.config;

import com.ozerler.marble.common.Constants;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class MdcLoggingFilterTest {

    @Test
    @DisplayName("an incoming trace id is stored as both traceId and correlationId and then cleared")
    void traceIdAndCorrelationIdMatch() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(Constants.TRACE_ID_HEADER, "trace-from-caller");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new MdcLoggingFilter().doFilter(request, response, (req, res) -> {
            assertThat(MDC.get(Constants.MDC_TRACE_ID)).isEqualTo("trace-from-caller");
            assertThat(MDC.get(Constants.MDC_CORRELATION_ID)).isEqualTo("trace-from-caller");
        });

        assertThat(response.getHeader(Constants.CORRELATION_ID_HEADER)).isEqualTo("trace-from-caller");
        assertThat(response.getHeader(Constants.TRACE_ID_HEADER)).isEqualTo("trace-from-caller");
        assertThat(MDC.get(Constants.MDC_TRACE_ID)).isNull();
        assertThat(MDC.get(Constants.MDC_CORRELATION_ID)).isNull();
    }

    @Test
    @DisplayName("a missing id is generated once and not left on the thread")
    void generatesAndClears() throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new MdcLoggingFilter().doFilter(new MockHttpServletRequest(), response, (req, res) -> {
            assertThat(MDC.get(Constants.MDC_TRACE_ID)).isNotBlank();
            assertThat(MDC.get(Constants.MDC_TRACE_ID)).isEqualTo(MDC.get(Constants.MDC_CORRELATION_ID));
        });
        assertThat(response.getHeader(Constants.TRACE_ID_HEADER)).isNotBlank();
        assertThat(MDC.getCopyOfContextMap()).isNull();
    }
}
