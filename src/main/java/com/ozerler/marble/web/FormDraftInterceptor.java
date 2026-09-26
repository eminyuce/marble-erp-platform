package com.ozerler.marble.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * When a controller reports a user-facing error, keep the submitted fields for the re-rendered form.
 */
@Component
public class FormDraftInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(@NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull Object handler,
                           @Nullable ModelAndView modelAndView) {
        if (modelAndView == null || !hasUserError(modelAndView, request)) {
            return;
        }
        if (isRedirect(modelAndView)) {
            FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
            if (flash == null || flash.containsKey(FormDraft.VALUES)) {
                return;
            }
            Map<String, List<String>> values = FormDraft.snapshot(request);
            if (values.isEmpty()) {
                return;
            }
            flash.put(FormDraft.VALUES, values);
            flash.put(FormDraft.ACTION, request.getRequestURI());
            return;
        }
        FormDraft.remember(modelAndView, request);
    }

    private static boolean isRedirect(ModelAndView modelAndView) {
        String viewName = modelAndView.getViewName();
        if (viewName != null && viewName.startsWith("redirect:")) {
            return true;
        }
        return modelAndView.getView() instanceof RedirectView;
    }

    private static boolean hasUserError(ModelAndView modelAndView, HttpServletRequest request) {
        if (present(modelAndView.getModel().get("errorMessage"))
                || present(modelAndView.getModel().get("formErrors"))) {
            return true;
        }
        FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
        return flash != null && present(flash.get("errorMessage"));
    }

    private static boolean present(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return !text.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }
        return true;
    }
}
