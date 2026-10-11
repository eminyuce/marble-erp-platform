package com.ozerler.marble.controller.erp;

import com.ozerler.marble.model.enums.BusinessUnit;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller providing primary /accounts routes for Cari Hesaplar (Müşteri, Tedarikçi, Şantiyeler).
 */
@Controller("cariAccountController")
@RequestMapping("/accounts")
public class CariAccountController {

    @GetMapping
    public String accountsIndex(@RequestParam(value = "unit", required = false) BusinessUnit unit) {
        if (unit != null) {
            return "redirect:/admin/definitions/customers?unit=" + unit.name();
        }
        return "redirect:/admin/definitions/customers";
    }

    @GetMapping("/create")
    public String createAccount(@RequestParam(value = "unit", required = false) BusinessUnit unit) {
        if (unit != null) {
            return "redirect:/admin/definitions/customers/create?unit=" + unit.name();
        }
        return "redirect:/admin/definitions/customers/create";
    }

    @GetMapping("/suppliers")
    public String suppliers() {
        return "redirect:/admin/definitions/suppliers";
    }

    @GetMapping("/suppliers/create")
    public String createSupplier() {
        return "redirect:/admin/definitions/suppliers/create";
    }

    @GetMapping("/projects")
    public String projects() {
        return "redirect:/projects";
    }

    @GetMapping("/projects/create")
    public String createProject() {
        return "redirect:/projects/create";
    }
}
