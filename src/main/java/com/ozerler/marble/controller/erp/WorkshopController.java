package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.CutOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.service.WorkshopCutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/workshop")
@RequiredArgsConstructor
public class WorkshopController {

    private final WorkshopCutService workshopCutService;

    @GetMapping
    public String workshopIndex() {
        return "erp/workshop/index";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<CutOrderDto> getCutOrdersData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {

        return workshopCutService.getCutOrdersPaged(page, size, search, sortField, sortDir);
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        populateCutForm(model);
        return "erp/workshop/cut-order-form";
    }

    @PostMapping("/create")
    public String createCutOrder(@RequestParam(value = "projectId", required = false) Long projectId,
                                 @RequestParam(value = "locationId", required = false) Long locationId,
                                 @RequestParam("slabId") Long slabId,
                                 @RequestParam("machineName") String machineName,
                                 @RequestParam("operatorName") String operatorName,
                                 @RequestParam("piecesCount") int piecesCount,
                                 @RequestParam("targetWidthCm") BigDecimal targetWidthCm,
                                 @RequestParam("targetLengthCm") BigDecimal targetLengthCm,
                                 @RequestParam(value = "edgeFinish", required = false) String edgeFinish,
                                 @RequestParam(value = "targetLocationDesc", required = false) String targetLocationDesc,
                                 @RequestParam(value = "notes", required = false) String notes,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        try {
            workshopCutService.createCutOrder(projectId, locationId, slabId, machineName, operatorName,
                    piecesCount, targetWidthCm, targetLengthCm, edgeFinish, targetLocationDesc, notes);

            redirectAttributes.addFlashAttribute("successMessage", "Ebatlama iş emri başarıyla tamamlandı.");
            return "redirect:/workshop";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Hata: " + e.getMessage());
            populateCutForm(model);
            return "erp/workshop/cut-order-form";
        }
    }

    private void populateCutForm(Model model) {
        model.addAttribute("availableSlabs", workshopCutService.getAvailableSlabs());
        model.addAttribute("projects", workshopCutService.getAllProjects());
        model.addAttribute("pageTitle", "Yeni Ebatlama Emri");
    }
}
