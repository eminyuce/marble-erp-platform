package com.ozerler.marble.controller.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.view.BlockView;
import com.ozerler.marble.dto.view.StockMovementView;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.TargetDestination;
import com.ozerler.marble.service.QuarryOperationService;
import com.ozerler.marble.web.ListQuery;
import com.ozerler.marble.web.OperationQuarryBlockGrid;
import com.ozerler.marble.web.QuarryDispatchGrid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/operations/quarry")
@RequiredArgsConstructor
@Slf4j
public class QuarryOperationController extends AbstractController {

    private final com.ozerler.marble.service.FormLookupService formLookup;
    private final com.ozerler.marble.service.StockMovementService stockMovementService;

    private final QuarryOperationService quarryOperationService;

    @GetMapping
    public String index(@RequestParam(value = "status", required = false) BlockStatus status,
                        @ModelAttribute ListQuery query, Model model) {
        model.addAttribute("status", status == null ? "" : status.name());
        model.addAttribute("totalEstTon", formLookup.estimatedTonnage());
        model.addAttribute("totalActTon", formLookup.actualTonnage());
        model.addAttribute("grid", OperationQuarryBlockGrid.view(
                quarryOperationService.searchQuarryBlockViews(query.getSearch(), status, query.pageNumber() - 1, query.pageSize()),
                query.pageNumber(), query.pageSize(), query.sortKey(), query.sortDirection()));
        model.addAttribute("activeSection", "quarry");
        return "operations/quarry/index";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<Map<String, Object>> getQuarryBlocksData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) BlockStatus status) {

        return quarryOperationService.quarryBlockRows(search, status, page, size);
    }

    @GetMapping({"/new", "/create"})
    public String newBlockForm(Model model) {
        model.addAttribute("quarries", quarryOperationService.getAllQuarryViews());
        model.addAttribute("qualityGrades", QualityGrade.values());
        model.addAttribute("activeSection", "quarry");
        return "operations/quarry/form";
    }

    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
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
            BlockView block = quarryOperationService.createQuarryBlockView(
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

    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    @PostMapping("/{id}/actual-tonnage")
    public String updateActualTonnage(@PathVariable("id") Long id,
                                      @RequestParam("actualTonnage") BigDecimal actualTonnage,
                                      RedirectAttributes redirectAttributes) {
        try {
            BlockView block = quarryOperationService.updateActualTonnageView(id, actualTonnage);
            redirectAttributes.addFlashAttribute("successMessage", "Blok (" + block.getBlockCode() + ") gerçek tonajı güncellendi: " + block.getActualTonnage() + " ton (Tahmini tonaj: " + block.getEstimatedTonnage() + " ton korundu).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
        }
        return "redirect:/operations/quarry";
    }

    @GetMapping("/{id}/dispatch")
    public String dispatchForm(@PathVariable("id") Long id, Model model) {
        var block = formLookup.getBlock(id);

        model.addAttribute("block", block);
        model.addAttribute("customers", formLookup.customers());
        model.addAttribute("invoices", formLookup.invoices());
        model.addAttribute("destinations", TargetDestination.values());
        model.addAttribute("activeSection", "quarry");
        return "operations/quarry/dispatch";
    }

    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    @PostMapping("/{id}/dispatch")
    public String dispatchBlock(@PathVariable("id") Long id,
                                @RequestParam("targetDestination") TargetDestination targetDestination,
                                @RequestParam(value = "customerId", required = false) Long customerId,
                                @RequestParam(value = "invoiceId", required = false) Long invoiceId,
                                @RequestParam(value = "notes", required = false) String notes,
                                RedirectAttributes redirectAttributes) {
        try {
            BlockView block = quarryOperationService.dispatchBlockView(id, targetDestination, customerId, invoiceId, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Blok (" + block.getBlockCode() + ") başarıyla " + targetDestination.getLabel() + " hedefine sevk edildi. Stok hareketi oluşturuldu.");
            return "redirect:/operations/quarry";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sevk hatası: " + e.getMessage());
            return "redirect:/operations/quarry/" + id + "/dispatch";
        }
    }

    @GetMapping("/dispatches")
    public String dispatchesReport(@RequestParam(value = "page", required = false) Integer page,
                                   @ModelAttribute ListQuery query, Model model) {
        Page<StockMovementView> pageData = stockMovementService.quarryDispatchViews(
                PageRequest.of(query.pageNumber() - 1, query.pageSize()));
        model.addAttribute("grid", QuarryDispatchGrid.view(
                pageData, query.pageNumber(), query.pageSize(), query.sortKey(), query.sortDirection()));
        model.addAttribute("activeSection", "quarry");
        return "operations/quarry/dispatches";
    }

    @GetMapping("/dispatches/api/data")
    @ResponseBody
    public TabulatorResponse<Map<String, Object>> getQuarryDispatchesData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "25") int size) {

        return stockMovementService.quarryDispatchRows(page, size);
    }
}
