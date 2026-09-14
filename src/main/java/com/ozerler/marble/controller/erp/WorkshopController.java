package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.CutOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.CutOrder;
import com.ozerler.marble.service.WorkshopCutService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Locale;

@Controller
@RequestMapping("/workshop")
@RequiredArgsConstructor
public class WorkshopController {

    private final WorkshopCutService workshopCutService;
    private final MessageSource messageSource;

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
    public String showCreateForm(Locale locale, Model model) {
        populateCutForm(model, locale);
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
                                 Locale locale,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        try {
            workshopCutService.createCutOrder(projectId, locationId, slabId, machineName, operatorName,
                    piecesCount, targetWidthCm, targetLengthCm, edgeFinish, targetLocationDesc, notes);

            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.workshop.cut.success", null, locale));
            return "redirect:/workshop";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            populateCutForm(model, locale);
            return "erp/workshop/cut-order-form";
        }
    }

    @GetMapping("/{id}")
    public String cutOrderDetail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("order", workshopCutService.getCutOrderWithDetails(id));
        return "erp/workshop/detail";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Locale locale, Model model) {
        CutOrder order = workshopCutService.getCutOrderById(id);
        populateCutForm(model, locale);
        model.addAttribute("record", order);
        model.addAttribute("isEdit", true);
        model.addAttribute("pageTitle", "Kesim Emri Düzenle: " + order.getCutOrderNo());
        return "erp/workshop/cut-order-form";
    }

    private void populateCutForm(Model model, Locale locale) {
        model.addAttribute("availableSlabs", workshopCutService.getAvailableSlabs());
        model.addAttribute("projects", workshopCutService.getAllProjects());
        model.addAttribute("pageTitle", messageSource.getMessage("erp.workshop.title.create", null, locale));
    }
}
