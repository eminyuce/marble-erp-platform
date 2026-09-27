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

    @GetMapping({"/learning-center", "/learning", "/ogrenme"})
    public String learningCenterAlias() {
        return "redirect:/ogrenme-merkezi";
    }

    @GetMapping("/ocak/uretim/yeni")
    public String ocakUretimYeni() {
        return "redirect:/blocks/create";
    }

    @GetMapping("/ocak/stok")
    public String ocakStok() {
        return "redirect:/blocks";
    }

    @GetMapping("/giderler/yeni")
    public String giderlerYeni() {
        return "redirect:/expenses/create";
    }

    @GetMapping("/fabrika/kesim")
    public String fabrikaKesim() {
        return "redirect:/production/create";
    }

    @GetMapping("/fabrika/cila")
    public String fabrikaCila() {
        return "redirect:/production/polish";
    }

    @GetMapping("/fabrika/ebatlama")
    public String fabrikaEbatlama() {
        return "redirect:/production/pallets";
    }

    @GetMapping("/atolye/is-emri")
    public String atolyeIsEmri() {
        return "redirect:/workshop/create";
    }

    @GetMapping("/santiye/planlama")
    public String santiyePlanlama() {
        return "redirect:/projects/create";
    }

    @GetMapping("/santiye/montaj")
    public String santiyeMontaj() {
        return "redirect:/projects";
    }

    @GetMapping("/maliyet-analizi")
    public String maliyetAnalizi() {
        return "redirect:/costs";
    }
}
