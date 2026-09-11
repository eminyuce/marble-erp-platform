package com.ozerler.marble.controller.admin;

import com.ozerler.marble.dto.GlobalSearchResponse;
import com.ozerler.marble.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping("/api/search")
    public GlobalSearchResponse search(@RequestParam(value = "q", defaultValue = "") String query) {
        return globalSearchService.search(query);
    }
}
