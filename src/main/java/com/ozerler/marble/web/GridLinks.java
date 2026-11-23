package com.ozerler.marble.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Rewrites page, size, sort, and an existing filter while keeping the other query parameters.
 */
public final class GridLinks {

    private final String path;
    private final Map<String, String[]> parameters;

    public GridLinks(HttpServletRequest request) {
        this.path = request.getRequestURI();
        this.parameters = request.getParameterMap();
    }

    public String page(int page) {
        return url(query().put("page", Integer.toString(Math.max(1, page))));
    }

    public String size(int size) {
        return url(query().put("size", Integer.toString(size)).put("page", "1"));
    }

    public String sort(String field) {
        String next;
        if (field.equals(first("sort")) && "asc".equalsIgnoreCase(first("dir"))) {
            next = "desc";
        } else {
            next = "asc";
        }
        return url(query().put("sort", field).put("dir", next).put("page", "1"));
    }

    public String indicator(String field) {
        if (!field.equals(first("sort"))) {
            return "↕";
        }
        if ("asc".equalsIgnoreCase(first("dir"))) {
            return "↑";
        }
        return "↓";
    }

    public String with(String name, String value) {
        Query query = query().put("page", "1");
        if (value == null || value.isBlank()) {
            return url(query.remove(name));
        }
        return url(query.put(name, value));
    }

    private String first(String name) {
        String[] values = parameters.get(name);
        if (values == null || values.length == 0 || values[0] == null) {
            return "";
        }
        return values[0];
    }

    private Query query() {
        List<String> names = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            String[] current = entry.getValue();
            if (current == null) {
                continue;
            }
            for (String item : current) {
                names.add(entry.getKey());
                values.add(Objects.requireNonNullElse(item, ""));
            }
        }
        return new Query(names, values);
    }

    private String url(Query query) {
        if (query.names.isEmpty()) {
            return path;
        }
        StringBuilder builder = new StringBuilder(path);
        builder.append('?');
        for (int index = 0; index < query.names.size(); index++) {
            if (index > 0) {
                builder.append('&');
            }
            builder.append(encode(query.names.get(index)));
            builder.append('=');
            builder.append(encode(query.values.get(index)));
        }
        return builder.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(Objects.requireNonNullElse(value, ""), StandardCharsets.UTF_8);
    }

    private static final class Query {
        private final List<String> names;
        private final List<String> values;

        private Query(List<String> names, List<String> values) {
            this.names = names;
            this.values = values;
        }

        private Query put(String name, String value) {
            List<String> nextNames = new ArrayList<>();
            List<String> nextValues = new ArrayList<>();
            for (int index = 0; index < names.size(); index++) {
                if (!names.get(index).equals(name)) {
                    nextNames.add(names.get(index));
                    nextValues.add(values.get(index));
                }
            }
            nextNames.add(name);
            nextValues.add(value);
            return new Query(nextNames, nextValues);
        }

        private Query remove(String name) {
            List<String> nextNames = new ArrayList<>();
            List<String> nextValues = new ArrayList<>();
            for (int index = 0; index < names.size(); index++) {
                if (!names.get(index).equals(name)) {
                    nextNames.add(names.get(index));
                    nextValues.add(values.get(index));
                }
            }
            return new Query(nextNames, nextValues);
        }
    }
}
