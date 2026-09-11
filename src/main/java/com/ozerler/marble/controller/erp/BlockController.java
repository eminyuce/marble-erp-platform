package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.BlockDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.service.QuarryBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/blocks")
@RequiredArgsConstructor
public class BlockController {

    private final QuarryBlockService quarryBlockService;

    @GetMapping
    public String blocksIndex(Model model) {
        model.addAttribute("quarries", quarryBlockService.getAllQuarries());
        return "erp/blocks/index";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<BlockDto> getBlocksData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {

        return quarryBlockService.getBlocksPaged(page, size, search, sortField, sortDir);
    }

    @GetMapping("/create")
    public String showCreateModal(Model model) {
        model.addAttribute("quarries", quarryBlockService.getAllQuarries());
        model.addAttribute("qualityGrades", QualityGrade.values());
        return "erp/blocks/form :: blockModalContent";
    }

    @PostMapping("/create")
    public String createBlock(@RequestParam("quarryId") Long quarryId,
                              @RequestParam("blockCode") String blockCode,
                              @RequestParam(value = "extractionDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate extractionDate,
                              @RequestParam("widthCm") int widthCm,
                              @RequestParam("lengthCm") int lengthCm,
                              @RequestParam("heightCm") int heightCm,
                              @RequestParam("actualWeightKg") BigDecimal actualWeightKg,
                              @RequestParam("stoneType") String stoneType,
                              @RequestParam(value = "colorTone", required = false) String colorTone,
                              @RequestParam("qualityGrade") QualityGrade qualityGrade,
                              @RequestParam(value = "crackLevel", defaultValue = "0") int crackLevel,
                              @RequestParam("extractionCost") BigDecimal extractionCost,
                              @RequestParam(value = "notes", required = false) String notes,
                              @RequestParam(value = "photoUrls", required = false) String photoUrls,
                              Model model) {

        try {
            quarryBlockService.registerBlock(quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                    actualWeightKg, stoneType, colorTone, qualityGrade, crackLevel, extractionCost, notes, photoUrls);
            model.addAttribute("success", true);
            model.addAttribute("message", "Ham blok başarıyla sisteme kaydedildi.");
            return "erp/blocks/form :: blockModalSuccess";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Hata: " + e.getMessage());
            model.addAttribute("quarries", quarryBlockService.getAllQuarries());
            model.addAttribute("qualityGrades", QualityGrade.values());
            return "erp/blocks/form :: blockModalContent";
        }
    }

    @PostMapping("/{id}/transfer-to-factory")
    @ResponseBody
    public ResponseEntity<Void> transferToFactory(@PathVariable("id") Long id,
                                                 @RequestParam("transportCost") BigDecimal transportCost) {
        quarryBlockService.transferToFactory(id, transportCost);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/sell")
    @ResponseBody
    public ResponseEntity<Void> sellBlock(@PathVariable("id") Long id) {
        quarryBlockService.sellBlockExternally(id);
        return ResponseEntity.ok().build();
    }
}
