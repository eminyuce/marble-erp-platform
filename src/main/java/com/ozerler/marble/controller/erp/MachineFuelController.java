package com.ozerler.marble.controller.erp;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.model.MachineFuelEntry;
import com.ozerler.marble.service.MachineFuelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/machines/fuel")
@RequiredArgsConstructor
public class MachineFuelController extends AbstractController {

    private final MachineFuelService machineFuelService;
    private final MessageSource messageSource;

    @GetMapping
    public String index(@RequestParam(value = "period", required = false) String period, Model model) {
        String currentPeriod = (period != null && !period.isBlank()) ? period : YearMonth.now().toString();
        model.addAttribute("period", currentPeriod);
        model.addAttribute("fuelEntries", machineFuelService.listByPeriod(currentPeriod));
        model.addAttribute("monthlyReport", machineFuelService.getMonthlyReport(currentPeriod));
        model.addAttribute("machines", machineFuelService.quarryAndFactoryMachines());

        var entries = machineFuelService.listByPeriod(currentPeriod);
        BigDecimal totalLitres = entries.stream().map(entry -> entry.getLitres()).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        BigDecimal totalAmount = entries.stream().map(entry -> entry.getTotalAmount()).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        long machineCount = entries.stream().map(e -> e.getMachine().getId()).distinct().count();

        model.addAttribute("summaryTotalLitres", totalLitres);
        model.addAttribute("summaryTotalAmount", totalAmount);
        model.addAttribute("summaryEntryCount", entries.size());
        model.addAttribute("summaryMachineCount", machineCount);

        return "erp/machines/fuel-index";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public com.ozerler.marble.dto.TabulatorResponse<com.ozerler.marble.dto.MachineFuelDto> getFuelData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "unit", required = false) String unit,
            @RequestParam(value = "machineId", required = false) Long machineId,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {
        return machineFuelService.getFuelPaged(page, size, search, period, unit, machineId, sortField, sortDir);
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("machines", machineFuelService.quarryAndFactoryMachines());
        model.addAttribute("defaultDate", LocalDate.now());
        return "erp/machines/fuel-form";
    }

    @PostMapping("/create")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public String createFuelEntry(@RequestParam("machineId") Long machineId,
                                  @RequestParam(value = "entryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entryDate,
                                  @RequestParam("litres") BigDecimal litres,
                                  @RequestParam("pricePerLitre") BigDecimal pricePerLitre,
                                  @RequestParam(value = "workingHoursOrKm", required = false) BigDecimal workingHoursOrKm,
                                  @RequestParam(value = "receiptNo", required = false) String receiptNo,
                                  @RequestParam(value = "issuedBy", required = false) String issuedBy,
                                  @RequestParam(value = "receivedBy", required = false) String receivedBy,
                                  @RequestParam(value = "notes", required = false) String notes,
                                  Locale locale,
                                  RedirectAttributes redirectAttributes) {
        try {
            MachineFuelEntry entry = machineFuelService.recordFuel(
                    machineId, entryDate, litres, pricePerLitre, workingHoursOrKm, receiptNo, issuedBy, receivedBy, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Mazot dolum kaydı başarıyla oluşturuldu: " + entry.getMachine().getName() + " (" + litres + " L)");
            return "redirect:/machines/fuel";
        } catch (Exception e) {
            log.error("Error creating fuel entry", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            return "redirect:/machines/fuel/create";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    public String deleteFuelEntry(@PathVariable("id") Long id,
                                  Locale locale,
                                  RedirectAttributes redirectAttributes) {
        try {
            machineFuelService.deleteFuel(id);
            redirectAttributes.addFlashAttribute("successMessage", "Mazot kaydı silindi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
        }
        return "redirect:/machines/fuel";
    }
}
