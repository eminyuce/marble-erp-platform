package com.ozerler.marble.config;

import com.ozerler.marble.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final SettingService settingService;

    @ModelAttribute("gridDefaultPageSize")
    public int gridDefaultPageSize() {
        String val = settingService.getSetting("grid.default_page_size", "25");
        try {
            int size = Integer.parseInt(val);
            return (size > 0 && size <= 100) ? size : 25;
        } catch (NumberFormatException e) {
            return 25;
        }
    }
}
