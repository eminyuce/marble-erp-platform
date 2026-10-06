package com.ozerler.marble.controller.erp;

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.MachineFuelEntry;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.enums.QuarryCategory;
import com.ozerler.marble.service.QuarryInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
        StockItem tank = quarryInventoryService.getOrCreateFuelTankStockItem();
        BigDecimal currentLiters = tank.getQuantity() != null ? tank.getQuantity() : BigDecimal.ZERO;
        List<Machine> machines = quarryInventoryService.getQuarryMachines();
        List<MachineFuelEntry> entries = quarryInventoryService.getRecentFuelEntries();

        model.addAttribute("tank", tank);
        model.addAttribute("currentLiters", currentLiters);
        model.addAttribute("machines", machines);
        model.addAttribute("entries", entries);
        model.addAttribute("activeSection", "quarry");
        model.addAttribute("activeSubSection", "fuel");

        return "quarry/fuel";
    }

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
            MachineFuelEntry entry = quarryInventoryService.dispenseFuel(
                    machineId, litres, LocalDate.now(), workingHoursOrKm, receiptNo, issuedBy, receivedBy, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("%s makinesine %s litre mazot verildi. Tutar (KDV hariç net): %s TL ocağın giderine yazıldı.",
                            entry.getMachine().getName(), litres, entry.getTotalAmount()));
        } catch (Exception e) {
            log.error("Mazot çıkış hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/quarry/fuel";
    }

    @PostMapping("/fuel/add")
    public String addFuelStock(@RequestParam("litres") BigDecimal litres,
                               @RequestParam(value = "netUnitPrice", required = false) BigDecimal netUnitPrice,
                               @RequestParam(value = "invoiceNo", required = false) String invoiceNo,
                               @RequestParam(value = "notes", required = false) String notes,
                               RedirectAttributes redirectAttributes) {
        try {
            StockItem updated = quarryInventoryService.addFuelStock(litres, netUnitPrice, null,
                    notes != null ? notes : (invoiceNo != null ? "Fatura: " + invoiceNo : "Doğrudan Depo Girişi"));
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("%s litre mazot depoya eklendi. Güncel stok: %s litre.", litres, updated.getQuantity()));
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
        List<StockItem> consumables = quarryInventoryService.getAllQuarryStockCards(category);
        List<Machine> machines = quarryInventoryService.getQuarryMachines();

        model.addAttribute("consumables", consumables);
        model.addAttribute("machines", machines);
        model.addAttribute("categories", QuarryCategory.values());
        model.addAttribute("selectedCategory", category != null ? category.name() : "");
        model.addAttribute("activeSection", "quarry");
        model.addAttribute("activeSubSection", "consumables");

        return "quarry/consumables";
    }

    @PostMapping("/consumables/consume")
    public String consumeConsumable(@RequestParam("stockItemId") Long stockItemId,
                                    @RequestParam("quantity") BigDecimal quantity,
                                    @RequestParam(value = "machineId", required = false) Long machineId,
                                    @RequestParam(value = "notes", required = false) String notes,
                                    RedirectAttributes redirectAttributes) {
        try {
            StockItem item = quarryInventoryService.consumeConsumable(stockItemId, quantity, machineId, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("%s malzemesinden %s %s kullanıldı ve ocağın giderine yazıldı. Kalan stok: %s %s.",
                            item.getDescription(), quantity, item.getUnit(), item.getQuantity(), item.getUnit()));
        } catch (Exception e) {
            log.error("Sarf malzeme tüketim hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/quarry/consumables";
    }

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
            QuarryCategory cat = category != null ? category : QuarryCategory.SARF_MALZEME;
            StockItem item = quarryInventoryService.createStockCard(
                    cat, description, unit, quantity, netUnitPrice, itemCode, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("%s (%s %s) stok kartı ve depo girişi başarıyla kaydedildi.",
                            item.getDescription(), quantity, item.getUnit()));
        } catch (Exception e) {
            log.error("Stok ekleme hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/quarry/consumables" + (category != null ? "?category=" + category.name() : "");
    }

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
            StockItem item = quarryInventoryService.createStockCard(
                    category, description, unit, initialQuantity, netUnitPrice, itemCode, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Yeni stok kartı (%s - %s) başarıyla tanımlandı.",
                            item.getItemCode(), item.getDescription()));
        } catch (Exception e) {
            log.error("Stok kartı oluşturma hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/quarry/consumables" + (category != null ? "?category=" + category.name() : "");
    }

    // ─────────────────────────────────────────────────────────────
    // 3. OCAK MAKİNELERİ
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/machines")
    public String machinesIndex(Model model) {
        List<Machine> machines = quarryInventoryService.getQuarryMachines();
        model.addAttribute("machines", machines);
        model.addAttribute("activeSection", "quarry");
        model.addAttribute("activeSubSection", "machines");
        return "quarry/machines";
    }

    @PostMapping("/machines/create")
    public String createMachine(@RequestParam(value = "code", required = false) String code,
                                @RequestParam("name") String name,
                                @RequestParam(value = "notes", required = false) String notes,
                                RedirectAttributes redirectAttributes) {
        try {
            Machine m = quarryInventoryService.createQuarryMachine(code, name, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Makine başarıyla tanımlandı: %s (%s)", m.getName(), m.getCode()));
        } catch (Exception e) {
            log.error("Makine oluşturma hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/quarry/machines";
    }
}
