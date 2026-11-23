package com.ozerler.marble.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GridLinksAdvice {

    @ModelAttribute("gridLinks")
    public GridLinks gridLinks(HttpServletRequest request) {
        return new GridLinks(request);
    }

    @ModelAttribute("filters")
    public GridFilters filters(HttpServletRequest request) {
        return GridFilters.from(request);
    }
}
