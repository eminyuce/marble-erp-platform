package com.ozerler.marble.controller.erp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErpAliasController {

    @GetMapping("/quarry")
    public String quarry() {
        return "redirect:/blocks";
    }

    @GetMapping("/factory")
    public String factory() {
        return "redirect:/production";
    }

    @GetMapping("/sites")
    public String sites() {
        return "redirect:/projects";
    }

    @GetMapping("/cost-analysis")
    public String costAnalysis() {
        return "redirect:/costs";
    }
}
