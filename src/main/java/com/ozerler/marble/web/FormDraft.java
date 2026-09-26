package com.ozerler.marble.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Copies a rejected form submission so the next render can put the values back.
 * Passwords, CSRF tokens, and file parts are never kept.
 */
public final class FormDraft {

    public static final String VALUES = "preservedForm";
    public static final String FIELDS = "invalidFields";
    public static final String ACTION = "preservedFormAction";

    private static final int MAX_FIELDS = 80;
    private static final int MAX_VALUE_LENGTH = 4000;

    private FormDraft() {
    }

    public static void remember(RedirectAttributes redirectAttributes,
                                HttpServletRequest request,
                                List<String> invalidFields) {
        Map<String, List<String>> values = snapshot(request);
        if (values.isEmpty() && (invalidFields == null || invalidFields.isEmpty())) {
            return;
        }
        if (!values.isEmpty()) {
            redirectAttributes.addFlashAttribute(VALUES, values);
            redirectAttributes.addFlashAttribute(ACTION, request.getRequestURI());
        }
        if (invalidFields != null && !invalidFields.isEmpty()) {
            redirectAttributes.addFlashAttribute(FIELDS, List.copyOf(invalidFields));
        }
    }

    public static void remember(ModelAndView modelAndView, HttpServletRequest request) {
        if (modelAndView.getModel().containsKey(VALUES)) {
            return;
        }
        Map<String, List<String>> values = snapshot(request);
        if (values.isEmpty()) {
            return;
        }
        modelAndView.addObject(VALUES, values);
        modelAndView.addObject(ACTION, request.getRequestURI());
    }

    public static Map<String, List<String>> snapshot(HttpServletRequest request) {
        Map<String, List<String>> kept = new LinkedHashMap<>();
        if (request == null) {
            return kept;
        }
        Map<String, String[]> parameters = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            if (kept.size() >= MAX_FIELDS) {
                break;
            }
            String name = entry.getKey();
            if (skip(name)) {
                continue;
            }
            String[] raw = entry.getValue();
            if (raw == null) {
                continue;
            }
            List<String> values = new ArrayList<>();
            for (String value : raw) {
                if (value == null) {
                    continue;
                }
                values.add(value.length() > MAX_VALUE_LENGTH ? value.substring(0, MAX_VALUE_LENGTH) : value);
            }
            kept.put(name, values);
        }
        return kept;
    }

    private static boolean skip(String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        return "_csrf".equals(normalized)
                || "csrf".equals(normalized)
                || normalized.contains("password")
                || normalized.contains("passwd");
    }
}
