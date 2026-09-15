package com.ozerler.marble.controller.admin;

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.*;
import com.ozerler.marble.service.MasterDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/definitions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class DefinitionController extends AbstractController {

    private final MasterDataService masterDataService;

    // =========================================================================
    // HUB / OVERVIEW
    // =========================================================================

    @GetMapping
    public String index(Model model) {
        model.addAttribute("counts", masterDataService.getSummaryCounts());
        model.addAttribute("currentSection", "definitions");
        return "admin/definitions/index";
    }

    // =========================================================================
    // MACHINES
    // =========================================================================

    @GetMapping("/machines")
    public String listMachines(Model model) {
        model.addAttribute("machines", masterDataService.getAllMachines());
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("machineTypes", MachineType.values());
        model.addAttribute("currentSection", "definitions-machines");
        return "admin/definitions/machines";
    }

    @PostMapping("/machines/save")
    public String saveMachine(@ModelAttribute Machine machine, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveMachine(machine);
            addSuccessFlash(redirectAttributes, "Makine (" + machine.getCode() + ") başarıyla kaydedildi.");
        } catch (Exception e) {
            log.error("Error saving machine", e);
            addErrorFlash(redirectAttributes, e.getMessage());
        }
        return "redirect:/admin/definitions/machines";
    }

    @PostMapping("/machines/{id}/toggle")
    public String toggleMachine(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Machine machine = masterDataService.toggleMachineActive(id);
            String statusText = machine.isActive() ? "aktif edildi" : "pasife alındı";
            addSuccessFlash(redirectAttributes, "Makine (" + machine.getCode() + ") " + statusText + ".");
        } catch (Exception e) {
            addErrorFlash(redirectAttributes, e.getMessage());
        }
        return "redirect:/admin/definitions/machines";
    }

    @PostMapping("/machines/{id}/delete")
    public String deleteMachine(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.deleteMachine(id);
            addSuccessFlash(redirectAttributes, "Makine başarıyla silindi.");
        } catch (Exception e) {
            addErrorFlash(redirectAttributes, e.getMessage());
        }
        return "redirect:/admin/definitions/machines";
    }

    // =========================================================================
    // STOCK LOCATIONS
    // =========================================================================

    @GetMapping("/stock-locations")
    public String listStockLocations(Model model) {
        model.addAttribute("locations", masterDataService.getAllStockLocations());
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("locationTypes", StockLocationType.values());
        model.addAttribute("currentSection", "definitions-locations");
        return "admin/definitions/stock-locations";
    }

    @PostMapping("/stock-locations/save")
    public String saveStockLocation(@ModelAttribute StockLocation location, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveStockLocation(location);
            addSuccessFlash(redirectAttributes, "Stok sahası (" + location.getCode() + ") başarıyla kaydedildi.");
        } catch (Exception e) {
            log.error("Error saving stock location", e);
            addErrorFlash(redirectAttributes, e.getMessage());
        }
        return "redirect:/admin/definitions/stock-locations";
    }

    @PostMapping("/stock-locations/{id}/toggle")
    public String toggleStockLocation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            StockLocation loc = masterDataService.toggleStockLocationActive(id);
            String statusText = loc.isActive() ? "aktif edildi" : "pasife alındı";
            addSuccessFlash(redirectAttributes, "Stok sahası (" + loc.getCode() + ") " + statusText + ".");
        } catch (Exception e) {
            addErrorFlash(redirectAttributes, e.getMessage());
        }
        return "redirect:/admin/definitions/stock-locations";
    }

    @PostMapping("/stock-locations/{id}/delete")
    public String deleteStockLocation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.deleteStockLocation(id);
            addSuccessFlash(redirectAttributes, "Stok sahası başarıyla silindi.");
        } catch (Exception e) {
            addErrorFlash(redirectAttributes, e.getMessage());
        }
        return "redirect:/admin/definitions/stock-locations";
    }

    // =========================================================================
    // QUARRIES
    // =========================================================================

    @GetMapping("/quarries")
    public String listQuarries(Model model) {
        model.addAttribute("quarries", masterDataService.getAllQuarries());
        model.addAttribute("currentSection", "definitions-quarries");
        return "admin/definitions/quarries";
    }

    @PostMapping("/quarries/save")
    public String saveQuarry(@ModelAttribute Quarry quarry, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveQuarry(quarry);
            addSuccessFlash(redirectAttributes, "Ocak (" + quarry.getName() + ") başarıyla kaydedildi.");
        } catch (Exception e) {
            log.error("Error saving quarry", e);
            addErrorFlash(redirectAttributes, e.getMessage());
        }
        return "redirect:/admin/definitions/quarries";
    }

    @PostMapping("/quarries/{id}/delete")
    public String deleteQuarry(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.deleteQuarry(id);
            addSuccessFlash(redirectAttributes, "Ocak kaydı başarıyla silindi.");
        } catch (Exception e) {
            addErrorFlash(redirectAttributes, e.getMessage());
        }
        return "redirect:/admin/definitions/quarries";
    }

    // =========================================================================
    // CUSTOMERS
    // =========================================================================

    @GetMapping("/customers")
    public String listCustomers(Model model) {
        model.addAttribute("customers", masterDataService.getAllCustomers());
        model.addAttribute("customerTypes", CustomerType.values());
        model.addAttribute("currentSection", "definitions-customers");
        return "admin/definitions/customers";
    }

    @PostMapping("/customers/save")
    public String saveCustomer(@ModelAttribute Customer customer, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveCustomer(customer);
            addSuccessFlash(redirectAttributes, "Müşteri (" + customer.getCompanyName() + ") başarıyla kaydedildi.");
        } catch (Exception e) {
            log.error("Error saving customer", e);
            addErrorFlash(redirectAttributes, e.getMessage());
        }
        return "redirect:/admin/definitions/customers";
    }

    @PostMapping("/customers/{id}/delete")
    public String deleteCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.deleteCustomer(id);
            addSuccessFlash(redirectAttributes, "Müşteri kaydı başarıyla silindi.");
        } catch (Exception e) {
            addErrorFlash(redirectAttributes, e.getMessage());
        }
        return "redirect:/admin/definitions/customers";
    }

    // =========================================================================
    // SUPPLIERS
    // =========================================================================

    @GetMapping("/suppliers")
    public String listSuppliers(Model model) {
        model.addAttribute("suppliers", masterDataService.getAllSuppliers());
        model.addAttribute("supplierTypes", SupplierType.values());
        model.addAttribute("currentSection", "definitions-suppliers");
        return "admin/definitions/suppliers";
    }

    @PostMapping("/suppliers/save")
    public String saveSupplier(@ModelAttribute Supplier supplier, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveSupplier(supplier);
            addSuccessFlash(redirectAttributes, "Tedarikçi (" + supplier.getCompanyName() + ") başarıyla kaydedildi.");
        } catch (Exception e) {
            log.error("Error saving supplier", e);
            addErrorFlash(redirectAttributes, e.getMessage());
        }
        return "redirect:/admin/definitions/suppliers";
    }

    @PostMapping("/suppliers/{id}/delete")
    public String deleteSupplier(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.deleteSupplier(id);
            addSuccessFlash(redirectAttributes, "Tedarikçi kaydı başarıyla silindi.");
        } catch (Exception e) {
            addErrorFlash(redirectAttributes, e.getMessage());
        }
        return "redirect:/admin/definitions/suppliers";
    }

    // =========================================================================
    // COST CENTERS
    // =========================================================================

    @GetMapping("/cost-centers")
    public String listCostCenters(Model model) {
        model.addAttribute("costCenters", masterDataService.getAllCostCenters());
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("currentSection", "definitions-cost-centers");
        return "admin/definitions/cost-centers";
    }

    @PostMapping("/cost-centers/save")
    public String saveCostCenter(@ModelAttribute CostCenter costCenter, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveCostCenter(costCenter);
            addSuccessFlash(redirectAttributes, "Masraf merkezi (" + costCenter.getName() + ") başarıyla kaydedildi.");
        } catch (Exception e) {
            log.error("Error saving cost center", e);
            addErrorFlash(redirectAttributes, e.getMessage());
        }
        return "redirect:/admin/definitions/cost-centers";
    }

    @PostMapping("/cost-centers/{id}/delete")
    public String deleteCostCenter(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.deleteCostCenter(id);
            addSuccessFlash(redirectAttributes, "Masraf merkezi başarıyla silindi.");
        } catch (Exception e) {
            addErrorFlash(redirectAttributes, e.getMessage());
        }
        return "redirect:/admin/definitions/cost-centers";
    }
}
