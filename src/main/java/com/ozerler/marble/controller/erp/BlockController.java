package com.ozerler.marble.controller.erp;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.BlockCodeAvailabilityDto;
import com.ozerler.marble.dto.BlockDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.service.BlockCustomerMarkService;
import com.ozerler.marble.service.ExpenseService;
import com.ozerler.marble.service.MachineFuelService;
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
import java.time.YearMonth;
import java.util.Locale;

@Slf4j
@Controller
@RequestMapping("/blocks")
@RequiredArgsConstructor
public class BlockController extends AbstractController {

    private final QuarryBlockService quarryBlockService;
    private final BlockCustomerMarkService blockCustomerMarkService;
    private final MachineFuelService machineFuelService;
    private final ExpenseService expenseService;
    private final CostCenterRepository costCenterRepository;
    private final MessageSource messageSource;

    @GetMapping
    public String blocksIndex(Model model) {
        model.addAttribute("quarries", quarryBlockService.getAllQuarries());
        model.addAttribute("summary", quarryBlockService.quarrySummary());
        model.addAttribute("quarryMachines", machineFuelService.quarryMachines());
        model.addAttribute("fuelEntries", machineFuelService.listAll());
        model.addAttribute("costCenters", costCenterRepository.findByBusinessUnit(BusinessUnit.QUARRY));
        model.addAttribute("customers", blockCustomerMarkService.customers());
        model.addAttribute("expenseTypes", new ExpenseType[]{
                ExpenseType.DIESEL, ExpenseType.ELECTRICITY, ExpenseType.DIRECT_LABOR,
                ExpenseType.FIXTURE_CONSUMABLE, ExpenseType.OVERHEAD});
        model.addAttribute("currentPeriod", YearMonth.now().toString());
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
        populateBlockForm(model, locale);
        return "erp/blocks/form";
    }

    @PostMapping("/create")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
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
                              Locale locale,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        try {
            quarryBlockService.registerBlock(quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                    actualWeightKg, stoneType, colorTone, qualityGrade, crackLevel, extractionCost, notes, photoUrls);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.block.create.success", null, locale));
            return "redirect:/blocks";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            model.addAttribute("isEdit", false);
            populateBlockForm(model, locale);
            return "erp/blocks/form";
        }
    }

    @GetMapping("/{id}")
    public String blockDetail(@PathVariable("id") Long id, Model model) {
        var block = quarryBlockService.getBlockWithDetails(id);
        model.addAttribute("block", block);
        model.addAttribute("movements", quarryBlockService.getMovements(id));
        model.addAttribute("marks", blockCustomerMarkService.listForBlock(id));
        model.addAttribute("customers", blockCustomerMarkService.customers());
        model.addAttribute("quarryYards", new StockLocationType[]{
                StockLocationType.PRODUCTION_YARD, StockLocationType.DISPATCH_YARD});
        model.addAttribute("weightWarning", quarryBlockService.isWeightDeviationWarning(block));
        return "erp/blocks/detail";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Locale locale, Model model) {
        var block = quarryBlockService.getBlockById(id);
        model.addAttribute("block", block);
        model.addAttribute("isEdit", true);
        populateBlockForm(model, locale);
        model.addAttribute("pageTitle", "Blok Düzenle");
        return "erp/blocks/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public String updateBlock(@PathVariable("id") Long id,
                              @RequestParam("quarryId") Long quarryId,
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
                              Locale locale,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        try {
            quarryBlockService.updateBlock(id, quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                    actualWeightKg, stoneType, colorTone, qualityGrade, crackLevel, extractionCost, notes, photoUrls);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.block.update.success", null, locale));
            return "redirect:/blocks";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            try {
                model.addAttribute("block", quarryBlockService.getBlockById(id));
            } catch (Exception ignored) {
            }
            model.addAttribute("isEdit", true);
            populateBlockForm(model, locale);
            return "erp/blocks/form";
        }
    }

    @PostMapping("/{id}/transfer-to-factory")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public @ResponseBody BackEndResponse transferToFactory(@PathVariable("id") Long id,
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
                                                   @RequestParam("customerId") Long customerId) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Selling block externally with id {} to customer {}", id, customerId);
            quarryBlockService.sellBlockExternally(id, customerId);

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

    @PostMapping("/{id}/move")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public String moveToYard(@PathVariable("id") Long id,
                             @RequestParam("targetType") StockLocationType targetType,
                             @RequestParam(value = "description", required = false) String description,
                             Locale locale,
                             RedirectAttributes redirectAttributes) {
        quarryBlockService.moveToYard(id, targetType, description);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.block.move.success", null, locale));
        return "redirect:/blocks/" + id;
    }

    @PostMapping("/{id}/sell-to-customer")
    @PreAuthorize(Constants.PRE_AUTH_SALES_WRITE)
    public String sellToCustomer(@PathVariable("id") Long id,
                                 @RequestParam("customerId") Long customerId,
                                 Locale locale,
                                 RedirectAttributes redirectAttributes) {
        quarryBlockService.sellBlockExternally(id, customerId);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.block.sell.success", null, locale));
        return "redirect:/blocks/" + id;
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

    @PostMapping("/fuel")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public String recordFuel(@RequestParam("machineId") Long machineId,
                             @RequestParam(value = "entryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entryDate,
                             @RequestParam("litres") BigDecimal litres,
                             @RequestParam("pricePerLitre") BigDecimal pricePerLitre,
                             @RequestParam(value = "receiptNo", required = false) String receiptNo,
                             @RequestParam(value = "issuedBy", required = false) String issuedBy,
                             @RequestParam(value = "receivedBy", required = false) String receivedBy,
                             @RequestParam(value = "notes", required = false) String notes,
                             Locale locale,
                             RedirectAttributes redirectAttributes) {
        machineFuelService.recordFuel(machineId, entryDate, litres, pricePerLitre, receiptNo, issuedBy, receivedBy, notes);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.block.fuel.success", null, locale));
        return "redirect:/blocks";
    }

    @PostMapping("/expenses")
    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    public String recordExpense(@RequestParam("centerId") Long centerId,
                                @RequestParam("expenseType") ExpenseType expenseType,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam(value = "documentNo", required = false) String documentNo,
                                @RequestParam(value = "invoiceDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invoiceDate,
                                @RequestParam(value = "expensePeriod", required = false) String expensePeriod,
                                @RequestParam(value = "description", required = false) String description,
                                Locale locale,
                                RedirectAttributes redirectAttributes) {
        expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                centerId, expenseType, null, BusinessUnit.QUARRY, amount, Constants.CURRENCY_TRY,
                documentNo, invoiceDate, LocalDate.now(), expensePeriod, YearMonth.now().toString(),
                null, null, null, null, null, null, null, description));
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.block.expense.success", null, locale));
        return "redirect:/blocks";
    }

    private void populateBlockForm(Model model, Locale locale) {
        model.addAttribute("quarries", quarryBlockService.getAllQuarries());
        model.addAttribute("qualityGrades", QualityGrade.values());
        model.addAttribute("pageTitle", messageSource.getMessage("erp.block.title.create", null, locale));
    }
}
