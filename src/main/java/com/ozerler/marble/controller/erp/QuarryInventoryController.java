package com.ozerler.marble.controller.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.view.MachineFuelEntryView;
import com.ozerler.marble.dto.view.MachineRef;
import com.ozerler.marble.dto.view.StockItemView;
import com.ozerler.marble.model.enums.QuarryCategory;
import com.ozerler.marble.service.QuarryInventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/quarry")
@RequiredArgsConstructor
@Slf4j
public class QuarryInventoryController extends AbstractController {

    private final QuarryInventoryService quarryInventoryService;

    // ─────────────────────────────────────────────────────────────
    // 1. MAZOT DEPOSU
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/fuel")
    public String fuelIndex(Model model) {
        StockItemView tank = quarryInventoryService.getOrCreateFuelTankStockItemView();
        BigDecimal currentLiters;
        if (tank.getQuantity() != null) {
            currentLiters = tank.getQuantity();
        } else {
            currentLiters = BigDecimal.ZERO;
        }
        List<MachineRef> machines = quarryInventoryService.getQuarryMachinesViews();
        List<MachineFuelEntryView> entries = quarryInventoryService.getRecentFuelEntryViews();

        model.addAttribute("tank", tank);
        model.addAttribute("currentLiters", currentLiters);
        model.addAttribute("machines", machines);
        model.addAttribute("entries", entries);
        model.addAttribute("activeSection", "quarry");
        model.addAttribute("activeSubSection", "fuel");

        return "quarry/fuel";
    }

    @GetMapping("/fuel/dispense")
    public String dispenseFuelGet() {
        return "redirect:/quarry/fuel?action=dispense";
    }

    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    @PostMapping("/fuel/dispense")
    public String dispenseFuel(@RequestParam("machineId") Long machineId,
                               @RequestParam("litres") BigDecimal litres,
                               @RequestParam(value = "workingHoursOrKm", required = false) BigDecimal workingHoursOrKm,
                               @RequestParam(value = "receiptNo", required = false) String receiptNo,
                               @RequestParam(value = "issuedBy", required = false) String issuedBy,
                               @RequestParam(value = "receivedBy", required = false) String receivedBy,
                               @RequestParam(value = "notes", required = false) String notes,
                               RedirectAttributes redirectAttributes) {
        try {
            MachineFuelEntryView entry = quarryInventoryService.dispenseFuelView(
                    machineId, litres, LocalDate.now(), workingHoursOrKm, receiptNo, issuedBy, receivedBy, notes);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    String.format(
                            "%s makinesine %s litre mazot verildi. Tutar (KDV hariç net): %s TL ocağın giderine "
                                    + "yazıldı.",
                            entry.getMachine().getName(), litres, entry.getTotalAmount()));
        } catch (Exception e) {
            log.error("Mazot çıkış hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/quarry/fuel";
    }

    @GetMapping("/fuel/add")
    public String addFuelStockGet() {
        return "redirect:/quarry/fuel?action=add";
    }

    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    @PostMapping("/fuel/add")
    public String addFuelStock(@RequestParam("litres") BigDecimal litres,
                               @RequestParam(value = "netUnitPrice", required = false) BigDecimal netUnitPrice,
                               @RequestParam(value = "invoiceNo", required = false) String invoiceNo,
                               @RequestParam(value = "notes", required = false) String notes,
                               RedirectAttributes redirectAttributes) {
        try {
            String addFuelStockView;
            if (notes != null) {
                addFuelStockView = notes;
            } else {
                if (invoiceNo != null) {
                    addFuelStockView = "Fatura: " + invoiceNo;
                } else {
                    addFuelStockView = "Doğrudan Depo Girişi";
                }
            }
            StockItemView updated = quarryInventoryService.addFuelStockView(litres, netUnitPrice, null,
                    addFuelStockView);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    String.format(
                            "%s litre mazot depoya eklendi. Güncel stok: %s litre.",
                            litres, updated.getQuantity()));
        } catch (Exception e) {
            log.error("Mazot dolum hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/quarry/fuel";
    }

    // ─────────────────────────────────────────────────────────────
    // 2. SARF MALZEME & STOK KARTLARI (MADDE 1 & 2)
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/consumables")
    public String consumablesIndex(@RequestParam(value = "category", required = false) QuarryCategory category,
                                   Model model) {
        List<StockItemView> consumables = quarryInventoryService.getAllQuarryStockCardsViews(category);
        List<MachineRef> machines = quarryInventoryService.getQuarryMachinesViews();

        BigDecimal totalValue = consumables.stream()
                .filter(item -> item.getQuantity() != null && item.getUnitPrice() != null)
                .map(item -> item.getQuantity().multiply(item.getUnitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalQuantity = consumables.stream().map(StockItemView::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("consumables", consumables);
        model.addAttribute("totalValue", totalValue);
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("machines", machines);
        model.addAttribute("categories", QuarryCategory.values());
        Object valueValue;
        if (category != null) {
            valueValue = category.name();
        } else {
            valueValue = "";
        }
        model.addAttribute("selectedCategory", valueValue);
        model.addAttribute("activeSection", "quarry");
        model.addAttribute("activeSubSection", "consumables");

        return "quarry/consumables";
    }

    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    @PostMapping("/consumables/consume")
    public String consumeConsumable(@RequestParam("stockItemId") Long stockItemId,
                                    @RequestParam("quantity") BigDecimal quantity,
                                    @RequestParam(value = "machineId", required = false) Long machineId,
                                    @RequestParam(value = "notes", required = false) String notes,
                                    RedirectAttributes redirectAttributes) {
        try {
            StockItemView item = quarryInventoryService.consumeConsumableView(stockItemId, quantity, machineId, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("%s malzemesinden %s %s kullanıldı ve ocağın giderine yazıldı. Kalan stok: %s %s.",
                            item.getDescription(), quantity, item.getUnit(), item.getQuantity(), item.getUnit()));
        } catch (Exception e) {
            log.error("Sarf malzeme tüketim hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/quarry/consumables";
    }

    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    @PostMapping("/consumables/add")
    public String addConsumableStock(@RequestParam(value = "category", required = false) QuarryCategory category,
                                     @RequestParam(value = "itemCode", required = false) String itemCode,
                                     @RequestParam("description") String description,
                                     @RequestParam("quantity") BigDecimal quantity,
                                     @RequestParam(value = "unit", defaultValue = "adet") String unit,
                                     @RequestParam(value = "netUnitPrice", required = false) BigDecimal netUnitPrice,
                                     @RequestParam(value = "notes", required = false) String notes,
                                     RedirectAttributes redirectAttributes) {
        try {
            QuarryCategory cat;
            if (category != null) {
                cat = category;
            } else {
                cat = QuarryCategory.SARF_MALZEME;
            }
            StockItemView item = quarryInventoryService.createStockCardView(
                    cat, description, unit, quantity, netUnitPrice, itemCode, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("%s (%s %s) stok kartı ve depo girişi başarıyla kaydedildi.",
                            item.getDescription(), quantity, item.getUnit()));
        } catch (Exception e) {
            log.error("Stok ekleme hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        String valueValue;
        if (category != null) {
            valueValue = "?category=" + category.name();
        } else {
            valueValue = "";
        }
        return "redirect:/quarry/consumables" + valueValue;
    }

    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    @PostMapping("/stock-cards/new")
    public String createStockCard(@RequestParam("category") QuarryCategory category,
                                  @RequestParam("description") String description,
                                  @RequestParam(value = "unit", defaultValue = "adet") String unit,
                                  @RequestParam(value = "initialQuantity", required = false) BigDecimal initialQuantity,
                                  @RequestParam(value = "netUnitPrice", required = false) BigDecimal netUnitPrice,
                                  @RequestParam(value = "itemCode", required = false) String itemCode,
                                  @RequestParam(value = "notes", required = false) String notes,
                                  RedirectAttributes redirectAttributes) {
        try {
            StockItemView item = quarryInventoryService.createStockCardView(
                    category, description, unit, initialQuantity, netUnitPrice, itemCode, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Yeni stok kartı (%s - %s) başarıyla tanımlandı.",
                            item.getItemCode(), item.getDescription()));
        } catch (Exception e) {
            log.error("Stok kartı oluşturma hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        String valueValue;
        if (category != null) {
            valueValue = "?category=" + category.name();
        } else {
            valueValue = "";
        }
        return "redirect:/quarry/consumables" + valueValue;
    }

    // ─────────────────────────────────────────────────────────────
    // 3. OCAK MAKİNELERİ
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/machines")
    public String machinesIndex(Model model) {
        List<MachineRef> machines = quarryInventoryService.getQuarryMachinesViews();
        model.addAttribute("machines", machines);
        model.addAttribute("activeSection", "quarry");
        model.addAttribute("activeSubSection", "machines");
        return "quarry/machines";
    }

    @PreAuthorize(Constants.PRE_AUTH_QUARRY_WRITE)
    @PostMapping("/machines/create")
    public String createMachine(@RequestParam(value = "code", required = false) String code,
                                @RequestParam("name") String name,
                                @RequestParam(value = "notes", required = false) String notes,
                                RedirectAttributes redirectAttributes) {
        try {
            MachineRef m = quarryInventoryService.createQuarryMachineView(code, name, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Makine başarıyla tanımlandı: %s (%s)", m.getName(), m.getCode()));
        } catch (Exception e) {
            log.error("Makine oluşturma hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/quarry/machines";
    }
}
