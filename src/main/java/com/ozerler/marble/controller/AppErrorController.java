package com.ozerler.marble.controller;

import com.ozerler.marble.exception.ErrorResponse;
import com.ozerler.marble.util.ErrorPageDetails;
import com.ozerler.marble.util.HtmlErrors;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Map;

@Controller
@RequestMapping("${spring.web.error.path:${error.path:/error}}")
@RequiredArgsConstructor
public class AppErrorController implements ErrorController {

    private static final ErrorAttributeOptions HTML_OPTIONS = ErrorAttributeOptions.of(
            Include.MESSAGE,
            Include.EXCEPTION,
            Include.STACK_TRACE,
            Include.BINDING_ERRORS,
            Include.PATH
    );

    private static final ErrorAttributeOptions JSON_OPTIONS = ErrorAttributeOptions.of(
            Include.MESSAGE,
            Include.PATH
    );

    private final ErrorAttributes errorAttributes;
    private final org.springframework.context.MessageSource messageSource;

    @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
    public String errorHtml(HttpServletRequest request, HttpServletResponse response, Model model) {
        HttpStatus status = statusOf(request);
        response.setStatus(status.value());
        Map<String, Object> attributes = errorAttributes.getErrorAttributes(new ServletWebRequest(request), HTML_OPTIONS);
        Throwable error = errorAttributes.getError(new ServletWebRequest(request));
        ErrorPageDetails page = HtmlErrors.page(status.value(), status.getReasonPhrase(), attributes, request, error);
        model.addAttribute("errorPage", page);
        return "error";
    }

    @RequestMapping
    public ResponseEntity<ErrorResponse> errorJson(HttpServletRequest request) {
        HttpStatus status = statusOf(request);
        Map<String, Object> attributes = errorAttributes.getErrorAttributes(new ServletWebRequest(request), JSON_OPTIONS);
        String path = HtmlErrors.pathOf(request, stringValue(attributes.get("path")));
        String message = HtmlErrors.messageFor(status.value(), stringValue(attributes.get("message")));
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message, path));
    }

    private static HttpStatus statusOf(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode instanceof Integer code) {
            try {
                return HttpStatus.valueOf(code);
            } catch (IllegalArgumentException ignored) {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
