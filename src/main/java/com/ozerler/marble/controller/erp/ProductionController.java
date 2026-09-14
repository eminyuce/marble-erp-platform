package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.ProductionOrderDto;
import com.ozerler.marble.dto.SlabDto;
import com.ozerler.marble.dto.SlabLabelDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.ScrapReasonCode;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.SurfaceFinish;
import com.ozerler.marble.service.ProductionService;
import com.ozerler.marble.service.QuarryBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Locale;

@Controller
@RequestMapping("/production")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;
    private final QuarryBlockService quarryBlockService;
    private final MessageSource messageSource;

    @GetMapping
    public String productionIndex(Model model) {
        model.addAttribute("scrapReasons", ScrapReasonCode.values());
        return "erp/production/index";
    }

    @GetMapping("/api/orders")
    @ResponseBody
    public TabulatorResponse<ProductionOrderDto> getOrdersData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {

        return productionService.getOrdersPaged(page, size, search, sortField, sortDir);
    }

    @GetMapping("/create")
    public String showCreateForm(Locale locale, Model model) {
        populateProductionForm(model, locale);
        return "erp/production/order-form";
    }

    @PostMapping("/create")
    public String executeGangsawCut(@RequestParam("blockId") Long blockId,
                                    @RequestParam(value = "orderNo", required = false) String orderNo,
                                    @RequestParam("machineName") String machineName,
                                    @RequestParam("durationHours") BigDecimal durationHours,
                                    @RequestParam("electricityKwh") BigDecimal electricityKwh,
                                    @RequestParam("bladeWearMm") BigDecimal bladeWearMm,
                                    @RequestParam("directCuttingExpense") BigDecimal directCuttingExpense,
                                    @RequestParam("operatorName") String operatorName,
                                    @RequestParam(value = "notes", required = false) String notes,
                                    @RequestParam(value = "slabCountGradeA", defaultValue = "0") int slabCountGradeA,
                                    @RequestParam(value = "slabCountGradeB", defaultValue = "0") int slabCountGradeB,
                                    @RequestParam(value = "slabCountGradeC", defaultValue = "0") int slabCountGradeC,
                                    @RequestParam("slabWidthCm") BigDecimal slabWidthCm,
                                    @RequestParam("slabLengthCm") BigDecimal slabLengthCm,
                                    @RequestParam("thicknessCm") BigDecimal thicknessCm,
                                    @RequestParam(value = "scrapReason", required = false) ScrapReasonCode scrapReason,
                                    @RequestParam(value = "scrapWeightKg", required = false) BigDecimal scrapWeightKg,
                                    @RequestParam(value = "scrapNotes", required = false) String scrapNotes,
                                    Locale locale,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {

        try {
            productionService.executeGangsawCut(blockId, orderNo, machineName, durationHours, electricityKwh,
                    bladeWearMm, directCuttingExpense, operatorName, notes,
                    slabCountGradeA, slabCountGradeB, slabCountGradeC,
                    slabWidthCm, slabLengthCm, thicknessCm, scrapReason, scrapWeightKg, scrapNotes);

            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.production.gangsaw.cut.success", null, locale));
            return "redirect:/production";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            populateProductionForm(model, locale);
            return "erp/production/order-form";
        }
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable("id") Long id, Model model) {
        ProductionOrder order = productionService.getOrderWithDetails(id);
        model.addAttribute("order", order);
        return "erp/production/detail";
    }

    @GetMapping("/orders/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Locale locale, Model model) {
        ProductionOrder order = productionService.getOrderById(id);
        populateProductionForm(model, locale);
        model.addAttribute("record", order);
        model.addAttribute("isEdit", true);
        model.addAttribute("pageTitle", "Üretim Emri Düzenle: " + order.getOrderNo());
        return "erp/production/order-form";
    }

    @GetMapping("/slabs")
    public String slabsView() {
        return "erp/production/slabs";
    }

    @GetMapping("/api/slabs")
    @ResponseBody
    public TabulatorResponse<SlabDto> getSlabsData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {

        return productionService.getSlabsPaged(page, size, search, sortField, sortDir);
    }

    @GetMapping("/slabs/{id}")
    public String slabDetail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("slab", productionService.getSlabWithDetails(id));
        return "erp/production/slab-detail";
    }

    @GetMapping("/slabs/{id}/edit")
    public String showSlabEditForm(@PathVariable("id") Long id, Locale locale, Model model) {
        populateSlabEditForm(model, productionService.getSlabWithDetails(id), locale);
        return "erp/production/slab-form";
    }

    @PostMapping("/slabs/{id}/edit")
    public String updateSlab(@PathVariable("id") Long id,
                             @RequestParam("slabCode") String slabCode,
                             @RequestParam("thicknessCm") BigDecimal thicknessCm,
                             @RequestParam("widthCm") BigDecimal widthCm,
                             @RequestParam("lengthCm") BigDecimal lengthCm,
                             @RequestParam("surfaceFinish") SurfaceFinish surfaceFinish,
                             @RequestParam("qualityGrade") QualityGrade qualityGrade,
                             @RequestParam(value = "glossLevel", required = false) Integer glossLevel,
                             @RequestParam("costPerM2") BigDecimal costPerM2,
                             @RequestParam("status") SlabStatus status,
                             Locale locale,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            productionService.updateSlab(id, slabCode, thicknessCm, widthCm, lengthCm,
                    surfaceFinish, qualityGrade, glossLevel, costPerM2, status);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.slab.update.success", null, locale));
            return "redirect:/production/slabs";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            try {
                populateSlabEditForm(model, productionService.getSlabWithDetails(id), locale);
            } catch (Exception ignored) {
                model.addAttribute("pageTitle", messageSource.getMessage("erp.slab.title.edit", null, locale));
            }
            return "erp/production/slab-form";
        }
    }

    @GetMapping("/slabs/{id}/label")
    public String slabLabel(@PathVariable("id") Long id, Model model) {
        SlabLabelDto label = productionService.getSlabLabelData(id, "http://localhost:8080/passport/");

        model.addAttribute("slab", label.getSlab());
        model.addAttribute("qrCodeBase64", label.getQrCodeBase64());
        model.addAttribute("blockCode", label.getBlockCode());
        model.addAttribute("stoneType", label.getStoneType());
        model.addAttribute("quarryName", label.getQuarryName());

        return "erp/production/slab-label";
    }

    private void populateProductionForm(Model model, Locale locale) {
        model.addAttribute("availableBlocks", quarryBlockService.getAvailableBlocksForProduction());
        model.addAttribute("scrapReasons", ScrapReasonCode.values());
        model.addAttribute("pageTitle", messageSource.getMessage("erp.production.title.create", null, locale));
    }

    private void populateSlabEditForm(Model model, Slab slab, Locale locale) {
        model.addAttribute("record", slab);
        model.addAttribute("slab", slab);
        model.addAttribute("qualityGrades", QualityGrade.values());
        model.addAttribute("surfaceFinishes", SurfaceFinish.values());
        model.addAttribute("slabStatuses", SlabStatus.values());
        model.addAttribute("pageTitle", messageSource.getMessage("erp.slab.title.edit", null, locale)
                + ": " + slab.getSlabCode());
    }
}
