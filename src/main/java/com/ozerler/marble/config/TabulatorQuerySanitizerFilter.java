package com.ozerler.marble.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tabulator can emit {@code page=undefined} before the first real page index
 * is known. Spring then 400s primitive {@code int} bindings and the grid stays
 * on a loading/empty state. Treat those tokens as missing so {@code defaultValue}
 * applies.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class TabulatorQuerySanitizerFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(new SanitizedRequest(request), response);
    }

    static final class SanitizedRequest extends HttpServletRequestWrapper {

        SanitizedRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            return sanitize(name, super.getParameter(name));
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] sanitized = new String[values.length];
            boolean droppedAll = true;
            int count = 0;
            for (String value : values) {
                String next = sanitize(name, value);
                if (next != null) {
                    sanitized[count++] = next;
                    droppedAll = false;
                }
            }
            if (droppedAll) {
                return null;
            }
            if (count == sanitized.length) {
                return sanitized;
            }
            String[] compact = new String[count];
            System.arraycopy(sanitized, 0, compact, 0, count);
            return compact;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> original = super.getParameterMap();
            Map<String, String[]> sanitized = new LinkedHashMap<>();
            for (Map.Entry<String, String[]> entry : original.entrySet()) {
                String[] values = getParameterValues(entry.getKey());
                if (values != null) {
                    sanitized.put(entry.getKey(), values);
                }
            }
            return Map.copyOf(sanitized);
        }

        static String sanitize(String name, String value) {
            if (!isPagingParam(name) || value == null) {
                return value;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty() || "undefined".equalsIgnoreCase(trimmed) || "null".equalsIgnoreCase(trimmed)) {
                return null;
            }
            return value;
        }

        private static boolean isPagingParam(String name) {
            return "page".equals(name) || "size".equals(name);
        }
    }
}
