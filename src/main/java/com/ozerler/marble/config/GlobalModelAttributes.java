package com.ozerler.marble.config;

import com.ozerler.marble.service.HelpService;
import com.ozerler.marble.service.SettingService;
import com.ozerler.marble.util.Ints;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final SettingService settingService;
    private final HelpService helpService;

    @Value("${app.asset-version:20260916-multifile-grid}")
    private String configuredAssetVersion;

    private final String fallbackAssetVersion = "20260916-" + System.currentTimeMillis();

    @ModelAttribute("gridDefaultPageSize")
    public int gridDefaultPageSize() {
        String val = settingService.getSetting("grid.default_page_size", "25");
        int size = Ints.parseOrDefault(val, 25);
        return (size > 0 && size <= 100) ? size : 25;
    }

    @ModelAttribute("isEdit")
    public boolean defaultIsEdit() {
        return false;
    }

    @ModelAttribute("helpPageKey")
    public String helpPageKey(HttpServletRequest request) {
        return helpService.resolvePageKey(request.getRequestURI()).orElse("");
    }

    @ModelAttribute("assetVersion")
    public String assetVersion() {
        if (configuredAssetVersion != null && !configuredAssetVersion.isBlank()) {
            return configuredAssetVersion;
        }
        return fallbackAssetVersion;
    }
}
