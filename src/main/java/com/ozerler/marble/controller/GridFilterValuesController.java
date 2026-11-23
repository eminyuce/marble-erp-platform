package com.ozerler.marble.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ozerler.marble.service.GridColumnFilterValuesService;
import com.ozerler.marble.web.GridFilterValue;

/**
 * Distinct values for the shared grid header filter. Requires an authenticated session.
 */
@RestController
@PreAuthorize("isAuthenticated()")
public class GridFilterValuesController {

    private final GridColumnFilterValuesService valuesService;

    public GridFilterValuesController(GridColumnFilterValuesService valuesService) {
        this.valuesService = valuesService;
    }

    @GetMapping(path = "/grid-filter-values", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<GridFilterValue> values(@RequestParam("grid") String grid,
                                        @RequestParam("column") String column) {
        return valuesService.values(grid, column);
    }
}
