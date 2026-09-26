package com.ozerler.marble.exception;

import com.ozerler.marble.util.MessageUtils;
import com.ozerler.marble.web.FormDraft;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns business-rule failures into a readable page notice instead of the developer error screen.
 */
@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class UserFacingExceptionHandler {

    static final String ERROR_VIEW = "error/user-message";

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex,
                                        HttpServletRequest request,
                                        HttpServletResponse response,
                                        HandlerMethod handlerMethod,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        String message = readableMessage(ex);
        List<String> invalidFields = ex instanceof RejectedInputException rejected
                ? rejected.fieldNames()
                : List.of();
        log.warn("Rejected request on {}: {}", request.getRequestURI(), message);
        if (writesJson(handlerMethod, request)) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), fieldErrors(invalidFields, message)));
        }
        String target = returnPath(request);
        if (target != null) {
            redirectAttributes.addFlashAttribute("errorMessage", message);
            FormDraft.remember(redirectAttributes, request, invalidFields);
            return "redirect:" + target;
        }
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("errorMessage", message);
        model.addAttribute("pageTitle", MessageUtils.getMessage("notice.error.title"));
        return ERROR_VIEW;
    }

    private static String readableMessage(IllegalArgumentException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return MessageUtils.getMessage("notice.fallback");
        }
        String trimmed = message.trim();
        if (trimmed.indexOf('\n') >= 0 || trimmed.indexOf('\r') >= 0 || trimmed.length() > 400) {
            return MessageUtils.getMessage("notice.fallback");
        }
        return trimmed;
    }

    private static Map<String, String> fieldErrors(List<String> invalidFields, String message) {
        if (invalidFields == null || invalidFields.isEmpty()) {
            return null;
        }
        Map<String, String> errors = new LinkedHashMap<>();
        for (String field : invalidFields) {
            errors.putIfAbsent(field, message);
        }
        return errors;
    }

    private static boolean writesJson(HandlerMethod handlerMethod, HttpServletRequest request) {
        if (handlerMethod != null) {
            boolean methodWritesBody = AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), ResponseBody.class);
            boolean typeWritesBody = AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), ResponseBody.class);
            if (methodWritesBody || typeWritesBody) {
                return true;
            }
        }
        String uri = request.getRequestURI();
        if (uri != null && (uri.startsWith("/api/") || uri.contains("/api/"))) {
            return true;
        }
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept != null
                && accept.contains(MediaType.APPLICATION_JSON_VALUE)
                && !accept.contains(MediaType.TEXT_HTML_VALUE);
    }

    private static String returnPath(HttpServletRequest request) {
        String referer = sameOriginPath(request.getHeader("Referer"), request);
        String requestUri = request.getRequestURI();
        if ("GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod())) {
            if (referer != null && !pathOnly(referer).equals(requestUri)) {
                return referer;
            }
            return null;
        }
        if (referer != null) {
            return referer;
        }
        return isAppPath(requestUri) ? requestUri : null;
    }

    private static String sameOriginPath(String referer, HttpServletRequest request) {
        if (referer == null || referer.isBlank()) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(referer.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
        if (uri.getHost() != null
                && request.getServerName() != null
                && !uri.getHost().equalsIgnoreCase(request.getServerName())) {
            return null;
        }
        String path = uri.getRawPath();
        if (!isAppPath(path) || "/error".equals(path)) {
            return null;
        }
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return path;
        }
        if (query.indexOf('\r') >= 0 || query.indexOf('\n') >= 0) {
            return path;
        }
        return path + "?" + query;
    }

    private static String pathOnly(String pathAndQuery) {
        int query = pathAndQuery.indexOf('?');
        return query < 0 ? pathAndQuery : pathAndQuery.substring(0, query);
    }

    private static boolean isAppPath(String path) {
        return path != null
                && path.startsWith("/")
                && !path.startsWith("//")
                && path.indexOf('\\') < 0
                && path.indexOf('\r') < 0
                && path.indexOf('\n') < 0;
    }
}
