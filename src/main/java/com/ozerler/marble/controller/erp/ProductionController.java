package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.ProductionOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.ScrapReasonCode;
import com.ozerler.marble.service.ProductionService;
import com.ozerler.marble.service.QuarryBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/production")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;
    private final QuarryBlockService quarryBlockService;

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
    public String showCreateModal(Model model) {
        model.addAttribute("availableBlocks", quarryBlockService.getAvailableBlocksForProduction());
        model.addAttribute("scrapReasons", ScrapReasonCode.values());
        return "erp/production/order-form :: orderModalContent";
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
                                    Model model) {

        try {
            productionService.executeGangsawCut(blockId, orderNo, machineName, durationHours, electricityKwh,
                    bladeWearMm, directCuttingExpense, operatorName, notes,
                    slabCountGradeA, slabCountGradeB, slabCountGradeC,
                    slabWidthCm, slabLengthCm, thicknessCm, scrapReason, scrapWeightKg, scrapNotes);

            model.addAttribute("success", true);
            model.addAttribute("message", "Katrak kesim emri tamamlandı ve plakalar dinamik kalite katsayılı maliyetle üretildi.");
            return "erp/production/order-form :: orderModalSuccess";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Hata: " + e.getMessage());
            model.addAttribute("availableBlocks", quarryBlockService.getAvailableBlocksForProduction());
            model.addAttribute("scrapReasons", ScrapReasonCode.values());
            return "erp/production/order-form :: orderModalContent";
        }
    }

    @GetMapping("/slabs")
    public String slabsView() {
        return "erp/production/slabs";
    }

    @GetMapping("/api/slabs")
    @ResponseBody
    public TabulatorResponse<Slab> getSlabsData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {

        return productionService.getSlabsPaged(page, size, search, sortField, sortDir);
    }
}
