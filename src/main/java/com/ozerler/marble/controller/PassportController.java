package com.ozerler.marble.controller;

import com.ozerler.marble.dto.PassportResult;
import com.ozerler.marble.service.PassportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/passport")
@RequiredArgsConstructor
public class PassportController {

    private final PassportService passportService;

    /**
     * Public responsive Digital Stone Passport endpoint (BRD Section 7.2)
     */
    @GetMapping("/{code}")
    public String viewDigitalPassport(@PathVariable("code") String code, Model model) {
        PassportResult result = passportService.getPassportByCode(code);

        if (result.isFound()) {
            model.addAttribute("type", result.getType());
            model.addAttribute("title", result.getTitle());

            switch (result.getType()) {
                case "PALLET" -> model.addAttribute("pallet", result.getPallet());
                case "SLAB" -> model.addAttribute("slab", result.getSlab());
                case "ITEM" -> model.addAttribute("item", result.getItem());
                case "BLOCK" -> model.addAttribute("block", result.getBlock());
                default -> {}
            }
        } else {
            model.addAttribute("errorMessage", result.getErrorMessage());
        }

        return "erp/passport/view";
    }
}
