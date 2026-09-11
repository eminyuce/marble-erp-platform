package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.ProductionOrderDto;
import com.ozerler.marble.dto.SlabDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.ScrapReasonCode;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.service.BarcodeService;
import com.ozerler.marble.service.ProductionService;
import com.ozerler.marble.service.QuarryBlockService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;

@Controller
@RequestMapping("/production")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;
    private final QuarryBlockService quarryBlockService;
    private final BarcodeService barcodeService;
    private final SlabRepository slabRepository;

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
    public TabulatorResponse<SlabDto> getSlabsData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {

        return productionService.getSlabsPaged(page, size, search, sortField, sortDir);
    }

    @GetMapping("/slabs/{id}/label")
    public String slabLabel(@PathVariable("id") Long id, Model model) {
        Slab slab = slabRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plaka bulunamadı: " + id));

        String passportUrl = "http://localhost:8080/passport/" + slab.getSlabCode();
        String qrCodeBase64 = barcodeService.generateQrCodeBase64(passportUrl);

        model.addAttribute("slab", slab);
        model.addAttribute("qrCodeBase64", qrCodeBase64);
        model.addAttribute("blockCode", slab.getBlock() != null ? slab.getBlock().getBlockCode() : "—");
        model.addAttribute("stoneType", slab.getBlock() != null ? slab.getBlock().getStoneType() : "—");
        model.addAttribute("quarryName", slab.getBlock() != null && slab.getBlock().getQuarry() != null
                ? slab.getBlock().getQuarry().getName() : "—");

        return "erp/production/slab-label";
    }
}
