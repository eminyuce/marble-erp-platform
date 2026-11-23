package com.ozerler.marble.config;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.util.ClientIps;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that populates the SLF4J MDC (Mapped Diagnostic Context)
 * with correlationId, userId, clientIp, and HTTP request details for structured JSON logging.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // One id is stored under both names so log readers can search correlationId or traceId.
            String correlationId = firstHeader(request, Constants.CORRELATION_ID_HEADER, Constants.TRACE_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            MDC.put(Constants.MDC_CORRELATION_ID, correlationId);
            MDC.put(Constants.MDC_TRACE_ID, correlationId);
            response.setHeader(Constants.CORRELATION_ID_HEADER, correlationId);
            response.setHeader(Constants.TRACE_ID_HEADER, correlationId);

            // 2. Resolve Client IP
            String clientIp = ClientIps.from(request);
            MDC.put(Constants.MDC_CLIENT_IP, clientIp);

            // 3. Request metadata
            MDC.put(Constants.MDC_HTTP_METHOD, request.getMethod());
            MDC.put(Constants.MDC_REQUEST_URI, request.getRequestURI());

            // 4. Resolve authenticated user if available
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                MDC.put(Constants.MDC_USER_ID, authentication.getName());
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clean up MDC to prevent thread-pool context leakage
            MDC.clear();
        }
    }

    private static String firstHeader(HttpServletRequest request, String primary, String secondary) {
        String value = request.getHeader(primary);
        if (value == null || value.isBlank()) {
            return request.getHeader(secondary);
        }
        return value;
    }
}
