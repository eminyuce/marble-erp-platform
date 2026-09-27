package com.ozerler.marble.controller.erp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Özerler Mermer ERP - 10 Adımda Öğrenme Merkezi Controller.
 * Sistemdeki kritik operasyonel ve yönetsel iş akışlarını 2-3 dakikalık
 * interaktif rehberlerle öğretir.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LearningCenterController {

    @GetMapping({"/ogrenme-merkezi", "/ogrenme-merkezi/"})
    public String index(Model model) {
        model.addAttribute("pageTitle", "10 Adımda Öğrenme Merkezi");
        model.addAttribute("helpPageKey", "learning-center");
        model.addAttribute("currentSection", "learning-center");
        return "learning/index";
    }
}
