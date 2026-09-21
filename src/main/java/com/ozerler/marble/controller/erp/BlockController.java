package com.ozerler.marble.controller.erp;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.BlockCodeAvailabilityDto;
import com.ozerler.marble.dto.BlockDto;
import com.ozerler.marble.dto.FileStorageDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;
import com.ozerler.marble.service.BlockCostCalculationService;
import com.ozerler.marble.service.BlockCustomerMarkService;
import com.ozerler.marble.service.FileStorageService;
import com.ozerler.marble.service.QuarryBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
@Controller
@RequestMapping("/blocks")
@RequiredArgsConstructor
public class BlockController extends AbstractController {

    private final QuarryBlockService quarryBlockService;
    private final BlockCostCalculationService blockCostCalculationService;
    private final BlockCustomerMarkService blockCustomerMarkService;
    private final MessageSource messageSource;
    private final FileStorageService fileStorageService;

    @GetMapping
    public String blocksIndex(Model model) {
        model.addAttribute("summary", quarryBlockService.quarrySummary());
        return "erp/blocks/index";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<BlockDto> getBlocksData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir,
            @RequestParam(value = "locationType", required = false) StockLocationType locationType,
            @RequestParam(value = "status", required = false) BlockStatus status) {

        return quarryBlockService.getBlocksPaged(page, size, search, sortField, sortDir, locationType, status);
    }

    @GetMapping("/api/generate-code")
    @ResponseBody
    public java.util.Map<String, String> generateBlockCode(
            @RequestParam(value = "quarryId", required = false) Long quarryId,
            @RequestParam(value = "section", defaultValue = "A3") String section) {
        String code = quarryBlockService.generateStandardBlockCode(quarryId, section);
        return java.util.Map.of("code", code);
    }

    @GetMapping("/api/block-code-available")
    @ResponseBody
    public BlockCodeAvailabilityDto checkBlockCode(@RequestParam("code") String code,
                                                   @RequestParam(value = "excludeId", required = false) Long excludeId,
                                                   Locale locale) {
        boolean available = quarryBlockService.isBlockCodeAvailable(code, excludeId);
        String message = available
                ? messageSource.getMessage("erp.block.code.available", null, locale)
                : messageSource.getMessage("error.block.code.duplicate", new Object[]{code}, locale);
        return new BlockCodeAvailabilityDto(available, message);
    }

    @GetMapping("/create")
    public String showCreateForm(Locale locale, Model model) {
        model.addAttribute("isEdit", false);
        model.addAttribute("currentLocationType", StockLocationType.PRODUCTION_YARD);
        model.addAttribute("attachedFiles", Collections.emptyList());
        populateBlockForm(model, locale);
        return "erp/blocks/form";
    }

    @PostMapping("/create")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public String createBlock(@RequestParam("quarryId") Long quarryId,
                              @RequestParam("blockCode") String blockCode,
                              @RequestParam(value = "locationType", required = false) StockLocationType locationType,
                              @RequestParam(value = "extractionDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate extractionDate,
                              @RequestParam("widthCm") int widthCm,
                              @RequestParam("lengthCm") int lengthCm,
                              @RequestParam("heightCm") int heightCm,
                              @RequestParam(value = "actualWeightKg", required = false) BigDecimal actualWeightKg,
                              @RequestParam(value = "stoneType", required = false) String stoneType,
                              @RequestParam(value = "colorTone", required = false) String colorTone,
                              @RequestParam("qualityGrade") QualityGrade qualityGrade,
                              @RequestParam(value = "crackLevel", defaultValue = "0") int crackLevel,
                              @RequestParam(value = "notes", required = false) String notes,
                              @RequestParam(value = "photoUrls", required = false) String photoUrls,
                              @RequestParam(value = "fileIds", required = false) List<Long> fileIds,
                              @RequestParam("quarrySection") String quarrySection,
                              Locale locale,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        try {
            if (quarrySection == null || quarrySection.isBlank()) {
                throw new IllegalArgumentException(messageSource.getMessage("error.block.section.required", null, locale));
            }
            quarryBlockService.registerBlock(quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                    actualWeightKg, stoneType, colorTone, qualityGrade, crackLevel, notes, photoUrls,
                    locationType, fileIds, quarrySection);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.block.create.success", null, locale));
            return "redirect:/blocks";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            model.addAttribute("isEdit", false);
            model.addAttribute("attachedFiles", Collections.emptyList());
            populateBlockForm(model, locale);
            return "erp/blocks/form";
        }
    }

    @GetMapping("/{id}")
    public String blockDetail(@PathVariable("id") Long id, Model model) {
        var block = quarryBlockService.getBlockWithDetails(id);
        model.addAttribute("block", block);
        Long quarryId = block.getQuarry() != null ? block.getQuarry().getId() : null;
        var breakdown = quarryId != null
                ? blockCostCalculationService.quarryBreakdown(quarryId, java.time.YearMonth.now().toString())
                : null;
        BigDecimal expensePerTon = breakdown != null ? breakdown.getExpensePerTon() : null;
        model.addAttribute("calculatedExtractionCost",
                blockCostCalculationService.calculatedExtractionCost(block, expensePerTon));
        model.addAttribute("calculatedTotalCost",
                blockCostCalculationService.calculatedTotalCost(block, expensePerTon));
        model.addAttribute("blockMarketValue", blockCostCalculationService.blockMarketValue(block));
        model.addAttribute("movements", quarryBlockService.getMovements(id));
        model.addAttribute("marks", blockCustomerMarkService.listForBlock(id));
        model.addAttribute("customers", blockCustomerMarkService.customers());
        model.addAttribute("weightWarning", quarryBlockService.isWeightDeviationWarning(block));
        model.addAttribute("canDelete", quarryBlockService.canDeleteBlock(block));

        List<FileStorageDto> attachedFiles = fileStorageService.getFilesForEntity("BLOCK", id).stream()
                .map(FileStorageDto::fromEntity)
                .toList();
        model.addAttribute("attachedFiles", attachedFiles);
        model.addAttribute("imageFiles", attachedFiles.stream().filter(FileStorageDto::isImage).toList());
        model.addAttribute("documentFiles", attachedFiles.stream().filter(f -> !f.isImage()).toList());

        return "erp/blocks/detail";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Locale locale, Model model) {
        var block = quarryBlockService.getBlockWithDetails(id);
        model.addAttribute("block", block);
        model.addAttribute("currentLocationType", block.getCurrentLocation() != null ? block.getCurrentLocation().getLocationType() : StockLocationType.PRODUCTION_YARD);
        model.addAttribute("isEdit", true);

        List<FileStorageDto> attachedFiles = fileStorageService.getFilesForEntity("BLOCK", id).stream()
                .map(FileStorageDto::fromEntity)
                .toList();
        model.addAttribute("attachedFiles", attachedFiles);

        populateBlockForm(model, locale);
        model.addAttribute("pageTitle", "Blok Düzenle");
        return "erp/blocks/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public String updateBlock(@PathVariable("id") Long id,
                              @RequestParam("quarryId") Long quarryId,
                              @RequestParam("blockCode") String blockCode,
                              @RequestParam(value = "locationType", required = false) StockLocationType locationType,
                              @RequestParam(value = "extractionDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate extractionDate,
                              @RequestParam("widthCm") int widthCm,
                              @RequestParam("lengthCm") int lengthCm,
                              @RequestParam("heightCm") int heightCm,
                              @RequestParam(value = "actualWeightKg", required = false) BigDecimal actualWeightKg,
                              @RequestParam(value = "stoneType", required = false) String stoneType,
                              @RequestParam(value = "colorTone", required = false) String colorTone,
                              @RequestParam("qualityGrade") QualityGrade qualityGrade,
                              @RequestParam(value = "crackLevel", defaultValue = "0") int crackLevel,
                              @RequestParam(value = "unitMarketValuePerTon", required = false) BigDecimal unitMarketValuePerTon,
                              @RequestParam(value = "notes", required = false) String notes,
                              @RequestParam(value = "photoUrls", required = false) String photoUrls,
                              @RequestParam(value = "fileIds", required = false) List<Long> fileIds,
                              @RequestParam(value = "quarrySection", required = false) String quarrySection,
                              Locale locale,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        try {
            quarryBlockService.updateBlock(id, quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                    actualWeightKg, stoneType, colorTone, qualityGrade, crackLevel, unitMarketValuePerTon, notes, photoUrls,
                    locationType, fileIds, quarrySection);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.block.update.success", null, locale));
            return "redirect:/blocks";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            try {
                var block = quarryBlockService.getBlockWithDetails(id);
                model.addAttribute("block", block);
                model.addAttribute("currentLocationType", block.getCurrentLocation() != null ? block.getCurrentLocation().getLocationType() : StockLocationType.PRODUCTION_YARD);
                List<FileStorageDto> attachedFiles = fileStorageService.getFilesForEntity("BLOCK", id).stream()
                        .map(FileStorageDto::fromEntity)
                        .toList();
                model.addAttribute("attachedFiles", attachedFiles);
            } catch (Exception ignored) {
            }
            model.addAttribute("isEdit", true);
            populateBlockForm(model, locale);
            return "erp/blocks/form";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public String deleteBlock(@PathVariable("id") Long id,
                              Locale locale,
                              RedirectAttributes redirectAttributes) {
        try {
            Block block = quarryBlockService.getBlockById(id);
            String code = block.getBlockCode();
            quarryBlockService.deleteBlock(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.block.delete.success", new Object[]{code}, locale));
            return "redirect:/blocks";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            return "redirect:/blocks/" + id;
        }
    }

    @PostMapping("/{id}/api/delete")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public @ResponseBody BackEndResponse deleteBlockApi(@PathVariable("id") Long id, Locale locale) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Deleting block {}", id);
            Block block = quarryBlockService.getBlockById(id);
            String code = block.getBlockCode();
            quarryBlockService.deleteBlock(id);

            ResponseEntity<Void> resp = ResponseEntity.ok().build();
            ber.setResponse(resp);
            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage(messageSource.getMessage("erp.block.delete.success", new Object[]{code}, locale));
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
        } catch (Exception e) {
            log.error("A serious error occurred while deleting block {}", id, e);
            ber = buildFatalResponse(ber, serviceStatus, status, "deleteBlock", Constants.ERR_FATAL);
        }

        return ber;
    }

    @GetMapping("/{id}/transfer-to-factory")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public String showTransferForm(@PathVariable("id") Long id,
                                   Locale locale,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        var block = quarryBlockService.getBlockWithDetails(id);
        if (redirectIfBlockCannotLeaveQuarry(block, locale, redirectAttributes, "error.block.move.not_at_quarry")) {
            return "redirect:/blocks/" + id;
        }
        if (!isInDispatchYard(block)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("error.block.dispatch.not_in_dispatch_yard", null, locale));
            return "redirect:/blocks/" + id;
        }
        populateBlockOperationPage(model, block);
        model.addAttribute("pageTitle", "Fabrikaya Sevk - " + block.getBlockCode());
        return "erp/blocks/transfer";
    }

    @PostMapping("/{id}/transfer-to-factory")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public String transferToFactoryForm(@PathVariable("id") Long id,
                                        @RequestParam("transportCost") BigDecimal transportCost,
                                        Locale locale,
                                        RedirectAttributes redirectAttributes) {
        try {
            quarryBlockService.transferToFactory(id, transportCost);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.block.transfer.success", null, locale));
            return "redirect:/blocks/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            return "redirect:/blocks/" + id + "/transfer-to-factory";
        }
    }

    @PostMapping("/{id}/api/transfer-to-factory")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public @ResponseBody BackEndResponse transferToFactoryApi(@PathVariable("id") Long id,
                                                              @RequestParam("transportCost") BigDecimal transportCost) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Transferring block {} to factory with cost {}", id, transportCost);
            quarryBlockService.transferToFactory(id, transportCost);

            ResponseEntity<Void> resp = ResponseEntity.ok().build();
            ber.setResponse(resp);
            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage("Transfer to factory successful");
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
        } catch (Exception e) {
            log.error("A serious error occurred in transferToFactory block {}", id, e);
            ber = buildFatalResponse(ber, serviceStatus, status, "transferToFactory", Constants.ERR_FATAL);
        }

        return ber;
    }

    @PostMapping("/{id}/sell")
    @PreAuthorize(Constants.PRE_AUTH_SALES_WRITE)
    public @ResponseBody BackEndResponse sellBlock(@PathVariable("id") Long id,
                                                   @RequestParam("customerId") Long customerId,
                                                   @RequestParam("salePrice") BigDecimal salePrice) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Selling block externally with id {} to customer {}", id, customerId);
            quarryBlockService.sellBlockExternally(id, customerId, salePrice, LocalDate.now(), null);

            ResponseEntity<Void> resp = ResponseEntity.ok().build();
            ber.setResponse(resp);
            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage("Sell block successful");
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
        } catch (Exception e) {
            log.error("A serious error occurred in sellBlock {}", id, e);
            ber = buildFatalResponse(ber, serviceStatus, status, "sellBlock", Constants.ERR_FATAL);
        }

        return ber;
    }

    @GetMapping("/{id}/move")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public String showMoveForm(@PathVariable("id") Long id,
                               @RequestParam(value = "targetType", required = false) StockLocationType targetType,
                               Locale locale,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        var block = quarryBlockService.getBlockWithDetails(id);
        if (redirectIfBlockCannotLeaveQuarry(block, locale, redirectAttributes, "error.block.move.not_at_quarry")) {
            return "redirect:/blocks/" + id;
        }
        StockLocationType selectedTarget = resolveMoveTarget(block, targetType);
        populateBlockOperationPage(model, block);
        model.addAttribute("selectedTargetType", selectedTarget);
        model.addAttribute("pageTitle", "Saha Taşıma - " + block.getBlockCode());
        return "erp/blocks/move";
    }

    @PostMapping("/{id}/move")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public String moveToYard(@PathVariable("id") Long id,
                             @RequestParam("targetType") StockLocationType targetType,
                             @RequestParam(value = "description", required = false) String description,
                             Locale locale,
                             RedirectAttributes redirectAttributes) {
        try {
            quarryBlockService.moveToYard(id, targetType, description);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.block.move.success", null, locale));
            return "redirect:/blocks/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            String targetQuery = targetType != null ? "?targetType=" + targetType.name() : "";
            return "redirect:/blocks/" + id + "/move" + targetQuery;
        }
    }

    @PostMapping("/{id}/api/move")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public @ResponseBody BackEndResponse moveToYardApi(@PathVariable("id") Long id,
                                                       @RequestParam("targetType") StockLocationType targetType,
                                                       @RequestParam(value = "description", required = false) String description) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Moving block {} to yard {}", id, targetType);
            quarryBlockService.moveToYard(id, targetType, description);

            ResponseEntity<Void> resp = ResponseEntity.ok().build();
            ber.setResponse(resp);
            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage("Move to yard successful");
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
        } catch (Exception e) {
            log.error("A serious error occurred in moveToYard block {}", id, e);
            ber = buildFatalResponse(ber, serviceStatus, status, "moveToYard", Constants.ERR_FATAL);
        }

        return ber;
    }

    @GetMapping("/{id}/sell")
    @PreAuthorize(Constants.PRE_AUTH_SALES_WRITE)
    public String showSellForm(@PathVariable("id") Long id,
                               Locale locale,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        var block = quarryBlockService.getBlockWithDetails(id);
        if (redirectIfBlockCannotLeaveQuarry(block, locale, redirectAttributes, "error.block.sell.not_at_quarry")) {
            return "redirect:/blocks/" + id;
        }
        if (!isInDispatchYard(block)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("error.block.sell.not_in_dispatch_yard", null, locale));
            return "redirect:/blocks/" + id;
        }
        model.addAttribute("block", block);
        model.addAttribute("customers", blockCustomerMarkService.customers());
        model.addAttribute("marks", blockCustomerMarkService.listForBlock(id));
        model.addAttribute("pageTitle", "Blok Satışı - " + block.getBlockCode());
        return "erp/blocks/sell";
    }

    @PostMapping("/{id}/sell-to-customer")
    @PreAuthorize(Constants.PRE_AUTH_SALES_WRITE)
    public String sellToCustomer(@PathVariable("id") Long id,
                                 @RequestParam("customerId") Long customerId,
                                 @RequestParam("salePrice") BigDecimal salePrice,
                                 @RequestParam(value = "saleDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate saleDate,
                                 @RequestParam(value = "saleNotes", required = false) String saleNotes,
                                 Locale locale,
                                 RedirectAttributes redirectAttributes) {
        try {
            quarryBlockService.sellBlockExternally(id, customerId, salePrice, saleDate, saleNotes);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.block.sell.success", null, locale));
            return "redirect:/blocks/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            return "redirect:/blocks/" + id + "/sell";
        }
    }

    @PostMapping("/{id}/mark")
    @PreAuthorize(Constants.PRE_AUTH_SALES_WRITE)
    public String markBlock(@PathVariable("id") Long id,
                            @RequestParam("customerId") Long customerId,
                            @RequestParam(value = "markedAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate markedAt,
                            @RequestParam(value = "validUntil", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validUntil,
                            @RequestParam("offerPrice") BigDecimal offerPrice,
                            @RequestParam(value = "currency", defaultValue = "TRY") String currency,
                            Locale locale,
                            RedirectAttributes redirectAttributes) {
        blockCustomerMarkService.markBlock(id, customerId, markedAt, validUntil, offerPrice, currency);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.block.mark.success", null, locale));
        return "redirect:/blocks/" + id;
    }

    @PostMapping("/marks/{markId}/sell")
    @PreAuthorize(Constants.PRE_AUTH_SALES_WRITE)
    public String convertMark(@PathVariable("markId") Long markId,
                              @RequestParam("blockId") Long blockId,
                              Locale locale,
                              RedirectAttributes redirectAttributes) {
        blockCustomerMarkService.convertMarkToSale(markId);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.block.mark.sale.success", null, locale));
        return "redirect:/blocks/" + blockId;
    }

    @PostMapping("/quarries")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public String saveQuarry(@RequestParam(value = "id", required = false) Long id,
                             @RequestParam("code") String code,
                             @RequestParam("name") String name,
                             @RequestParam(value = "location", required = false) String location,
                             @RequestParam("specificGravity") BigDecimal specificGravity,
                             @RequestParam(value = "licenseNo", required = false) String licenseNo,
                             Locale locale,
                             RedirectAttributes redirectAttributes) {
        quarryBlockService.saveQuarry(id, code, name, location, specificGravity, licenseNo);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.block.quarry.save.success", null, locale));
        return "redirect:/blocks";
    }

    private void populateBlockForm(Model model, Locale locale) {
        model.addAttribute("quarries", quarryBlockService.getAllQuarries());
        model.addAttribute("qualityGrades", QualityGrade.values());
        model.addAttribute("stoneCatalog", com.ozerler.marble.domain.StoneTypeCatalog.getAll());
        model.addAttribute("quarryYards", quarryYards());
        model.addAttribute("pageTitle", messageSource.getMessage("erp.block.title.create", null, locale));
    }

    private void populateBlockOperationPage(Model model, Block block) {
        model.addAttribute("block", block);
        model.addAttribute("quarryYards", quarryYards());
    }

    private StockLocationType[] quarryYards() {
        return new StockLocationType[]{StockLocationType.PRODUCTION_YARD, StockLocationType.DISPATCH_YARD};
    }

    private boolean redirectIfBlockCannotLeaveQuarry(Block block, Locale locale,
                                                     RedirectAttributes redirectAttributes,
                                                     String notAtQuarryMessageKey) {
        if (block.getStatus() == BlockStatus.SOLD) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("erp.block.already_sold",
                            new Object[]{block.getSoldCustomer() != null ? block.getSoldCustomer().getCompanyName() : ""},
                            locale));
            return true;
        }
        if (!block.getCanonicalStatus().isAtQuarry()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage(notAtQuarryMessageKey, null, locale));
            return true;
        }
        return false;
    }

    private boolean isInDispatchYard(Block block) {
        return block.getCurrentLocation() != null
                && block.getCurrentLocation().getLocationType() == StockLocationType.DISPATCH_YARD;
    }

    private StockLocationType resolveMoveTarget(Block block, StockLocationType requested) {
        if (requested == StockLocationType.PRODUCTION_YARD || requested == StockLocationType.DISPATCH_YARD) {
            return requested;
        }
        if (block.getCurrentLocation() != null
                && block.getCurrentLocation().getLocationType() == StockLocationType.PRODUCTION_YARD) {
            return StockLocationType.DISPATCH_YARD;
        }
        return StockLocationType.PRODUCTION_YARD;
    }
}
