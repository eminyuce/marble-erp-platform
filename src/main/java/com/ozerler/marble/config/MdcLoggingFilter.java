package com.ozerler.marble.config;

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

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_CLIENT_IP = "clientIp";
    public static final String MDC_HTTP_METHOD = "httpMethod";
    public static final String MDC_REQUEST_URI = "requestUri";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Resolve or generate Correlation ID
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            MDC.put(MDC_CORRELATION_ID, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            // 2. Resolve Client IP
            String clientIp = resolveClientIp(request);
            MDC.put(MDC_CLIENT_IP, clientIp);

            // 3. Request metadata
            MDC.put(MDC_HTTP_METHOD, request.getMethod());
            MDC.put(MDC_REQUEST_URI, request.getRequestURI());

            // 4. Resolve authenticated user if available
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                MDC.put(MDC_USER_ID, authentication.getName());
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clean up MDC to prevent thread-pool context leakage
            MDC.clear();
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
