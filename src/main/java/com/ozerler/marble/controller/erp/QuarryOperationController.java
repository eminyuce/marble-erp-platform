package com.ozerler.marble.controller.erp;

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.*;
import com.ozerler.marble.repository.*;
import com.ozerler.marble.service.InvoiceService;
import com.ozerler.marble.service.QuarryOperationService;
import com.ozerler.marble.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/operations/quarry")
@RequiredArgsConstructor
@Slf4j
public class QuarryOperationController extends AbstractController {

    private final QuarryOperationService quarryOperationService;
    private final BlockRepository blockRepository;
    private final StockLocationRepository stockLocationRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final StockMovementRepository movementRepository;

    @GetMapping
    public String index(@RequestParam(value = "search", required = false) String search,
                        @RequestParam(value = "status", required = false) BlockStatus status,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "20") int size,
                        Model model) {

        Page<Block> blockPage = quarryOperationService.searchQuarryBlocks(search, status, page, size);
        List<Quarry> quarries = quarryOperationService.getAllQuarries();

        long prodYardCount = blockRepository.countByLocationType(StockLocationType.PRODUCTION_YARD);
        long dispatchYardCount = blockRepository.countByLocationType(StockLocationType.DISPATCH_YARD);
        BigDecimal totalEstTon = blockRepository.sumTotalEstimatedTonnage();
        BigDecimal totalActTon = blockRepository.sumTotalActualTonnage();

        model.addAttribute("blocks", blockPage.getContent());
        model.addAttribute("page", blockPage);
        model.addAttribute("quarries", quarries);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("prodYardCount", prodYardCount);
        model.addAttribute("dispatchYardCount", dispatchYardCount);
        model.addAttribute("totalEstTon", totalEstTon != null ? totalEstTon : BigDecimal.ZERO);
        model.addAttribute("totalActTon", totalActTon != null ? totalActTon : BigDecimal.ZERO);
        model.addAttribute("activeSection", "quarry");

        return "operations/quarry/index";
    }

    @GetMapping({"/new", "/create"})
    public String newBlockForm(Model model) {
        model.addAttribute("quarries", quarryOperationService.getAllQuarries());
        model.addAttribute("qualityGrades", QualityGrade.values());
        model.addAttribute("activeSection", "quarry");
        return "operations/quarry/form";
    }

    @PostMapping({"/new", "/create"})
    public String createBlock(@RequestParam("quarryId") Long quarryId,
                              @RequestParam(value = "blockCode", required = false) String blockCode,
                              @RequestParam(value = "extractionDate", required = false) LocalDate extractionDate,
                              @RequestParam("widthCm") int widthCm,
                              @RequestParam("lengthCm") int lengthCm,
                              @RequestParam("heightCm") int heightCm,
                              @RequestParam("estimatedTonnage") BigDecimal estimatedTonnage,
                              @RequestParam(value = "actualTonnage", required = false) BigDecimal actualTonnage,
                              @RequestParam(value = "stoneType", required = false) String stoneType,
                              @RequestParam(value = "colorTone", required = false) String colorTone,
                              @RequestParam(value = "qualityGrade", required = false) QualityGrade qualityGrade,
                              @RequestParam(value = "crackLevel", defaultValue = "0") int crackLevel,
                              @RequestParam(value = "quarrySection", required = false) String quarrySection,
                              @RequestParam(value = "notes", required = false) String notes,
                              RedirectAttributes redirectAttributes) {
        try {
            Block block = quarryOperationService.createQuarryBlock(
                    quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                    estimatedTonnage, actualTonnage, stoneType, colorTone, qualityGrade,
                    crackLevel, quarrySection, notes
            );
            redirectAttributes.addFlashAttribute("successMessage", "Blok (" + block.getBlockCode() + ") başarıyla kaydedildi. Tahmini: " + block.getEstimatedTonnage() + " ton.");
            return "redirect:/operations/quarry";
        } catch (Exception e) {
            log.error("Blok kaydedilemedi", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
            return "redirect:/operations/quarry/new";
        }
    }

    @PostMapping("/{id}/actual-tonnage")
    public String updateActualTonnage(@PathVariable("id") Long id,
                                      @RequestParam("actualTonnage") BigDecimal actualTonnage,
                                      RedirectAttributes redirectAttributes) {
        try {
            Block block = quarryOperationService.updateActualTonnage(id, actualTonnage);
            redirectAttributes.addFlashAttribute("successMessage", "Blok (" + block.getBlockCode() + ") gerçek tonajı güncellendi: " + block.getActualTonnage() + " ton (Tahmini tonaj: " + block.getEstimatedTonnage() + " ton korundu).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
        }
        return "redirect:/operations/quarry";
    }

    @GetMapping("/{id}/dispatch")
    public String dispatchForm(@PathVariable("id") Long id, Model model) {
        Block block = blockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Blok bulunamadı: " + id));

        model.addAttribute("block", block);
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("invoices", invoiceRepository.findAll());
        model.addAttribute("destinations", TargetDestination.values());
        model.addAttribute("activeSection", "quarry");
        return "operations/quarry/dispatch";
    }

    @PostMapping("/{id}/dispatch")
    public String dispatchBlock(@PathVariable("id") Long id,
                                @RequestParam("targetDestination") TargetDestination targetDestination,
                                @RequestParam(value = "customerId", required = false) Long customerId,
                                @RequestParam(value = "invoiceId", required = false) Long invoiceId,
                                @RequestParam(value = "notes", required = false) String notes,
                                RedirectAttributes redirectAttributes) {
        try {
            Block block = quarryOperationService.dispatchBlock(id, targetDestination, customerId, invoiceId, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Blok (" + block.getBlockCode() + ") başarıyla " + targetDestination.getLabel() + " hedefine sevk edildi. Stok hareketi oluşturuldu.");
            return "redirect:/operations/quarry";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sevk hatası: " + e.getMessage());
            return "redirect:/operations/quarry/" + id + "/dispatch";
        }
    }

    @GetMapping("/dispatches")
    public String dispatchesReport(Model model) {
        List<StockMovement> dispatches = movementRepository.findRecentBySourceDepartment(BusinessUnit.QUARRY, java.time.LocalDateTime.now().minusDays(90));
        model.addAttribute("dispatches", dispatches);
        model.addAttribute("activeSection", "quarry");
        return "operations/quarry/dispatches";
    }
}
