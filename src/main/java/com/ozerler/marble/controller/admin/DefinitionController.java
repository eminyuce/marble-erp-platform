package com.ozerler.marble.controller.admin;

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.CostCenterDto;
import com.ozerler.marble.dto.CustomerDto;
import com.ozerler.marble.dto.MachineDto;
import com.ozerler.marble.dto.QuarryDto;
import com.ozerler.marble.dto.StockLocationDto;
import com.ozerler.marble.dto.SupplierDto;
import com.ozerler.marble.dto.TabulatorResponse;
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

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/definitions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class DefinitionController extends AbstractController {

    private final MasterDataService masterDataService;

    @GetMapping
    public String index() {
        return "redirect:/admin/definitions/machines";
    }

    // =========================================================================
    // MACHINES
    // =========================================================================

    @GetMapping("/machines")
    public String listMachines(Model model) {
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("machineTypes", MachineType.values());
        model.addAttribute("currentSection", "definitions-machines");
        return "admin/definitions/machines";
    }

    @GetMapping("/machines/create")
    public String createMachineForm(Model model) {
        populateMachineForm(model, newMachine(), false);
        return "admin/definitions/machine-form";
    }

    @GetMapping("/machines/{id}/edit")
    public String editMachineForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            populateMachineForm(model, masterDataService.getMachineById(id), true);
            return "admin/definitions/machine-form";
        } catch (IllegalArgumentException e) {
            addErrorFlash(redirectAttributes, e.getMessage());
            return "redirect:/admin/definitions/machines";
        }
    }

    @GetMapping("/machines/api/data")
    @ResponseBody
    public TabulatorResponse<MachineDto> machinesData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "unit", required = false) BusinessUnit unit,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {
        return masterDataService.getMachinesPaged(page, size, search, unit, sortField, sortDir);
    }

    @PostMapping("/machines/save")
    public String saveMachine(@ModelAttribute Machine machine, Model model, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveMachine(machine);
            addSuccessFlash(redirectAttributes, "Makine (" + machine.getCode() + ") başarıyla kaydedildi.");
            return "redirect:/admin/definitions/machines";
        } catch (Exception e) {
            log.error("Error saving machine", e);
            populateMachineForm(model, machine, machine.getId() != null);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/definitions/machine-form";
        }
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
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("locationTypes", StockLocationType.values());
        model.addAttribute("currentSection", "definitions-locations");
        return "admin/definitions/stock-locations";
    }

    @GetMapping("/stock-locations/create")
    public String createStockLocationForm(Model model) {
        populateStockLocationForm(model, newStockLocation(), false);
        return "admin/definitions/stock-location-form";
    }

    @GetMapping("/stock-locations/{id}/edit")
    public String editStockLocationForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            populateStockLocationForm(model, masterDataService.getStockLocationById(id), true);
            return "admin/definitions/stock-location-form";
        } catch (IllegalArgumentException e) {
            addErrorFlash(redirectAttributes, e.getMessage());
            return "redirect:/admin/definitions/stock-locations";
        }
    }

    @GetMapping("/stock-locations/api/data")
    @ResponseBody
    public TabulatorResponse<StockLocationDto> stockLocationsData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "unit", required = false) BusinessUnit unit,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {
        return masterDataService.getStockLocationsPaged(page, size, search, unit, sortField, sortDir);
    }

    @PostMapping("/stock-locations/save")
    public String saveStockLocation(@ModelAttribute StockLocation location, Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveStockLocation(location);
            addSuccessFlash(redirectAttributes, "Stok sahası (" + location.getCode() + ") başarıyla kaydedildi.");
            return "redirect:/admin/definitions/stock-locations";
        } catch (Exception e) {
            log.error("Error saving stock location", e);
            populateStockLocationForm(model, location, location.getId() != null);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/definitions/stock-location-form";
        }
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
        model.addAttribute("currentSection", "definitions-quarries");
        return "admin/definitions/quarries";
    }

    @GetMapping("/quarries/create")
    public String createQuarryForm(Model model) {
        populateQuarryForm(model, newQuarry(), false);
        return "admin/definitions/quarry-form";
    }

    @GetMapping("/quarries/{id}/edit")
    public String editQuarryForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            populateQuarryForm(model, masterDataService.getQuarryById(id), true);
            return "admin/definitions/quarry-form";
        } catch (IllegalArgumentException e) {
            addErrorFlash(redirectAttributes, e.getMessage());
            return "redirect:/admin/definitions/quarries";
        }
    }

    @GetMapping("/quarries/api/data")
    @ResponseBody
    public TabulatorResponse<QuarryDto> quarriesData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {
        return masterDataService.getQuarriesPaged(page, size, search, sortField, sortDir);
    }

    @PostMapping("/quarries/save")
    public String saveQuarry(@ModelAttribute Quarry quarry, Model model, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveQuarry(quarry);
            addSuccessFlash(redirectAttributes, "Ocak (" + quarry.getName() + ") başarıyla kaydedildi.");
            return "redirect:/admin/definitions/quarries";
        } catch (Exception e) {
            log.error("Error saving quarry", e);
            populateQuarryForm(model, quarry, quarry.getId() != null);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/definitions/quarry-form";
        }
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
        model.addAttribute("customerTypes", CustomerType.values());
        model.addAttribute("currentSection", "definitions-customers");
        return "admin/definitions/customers";
    }

    @GetMapping("/customers/create")
    public String createCustomerForm(Model model) {
        populateCustomerForm(model, newCustomer(), false);
        return "admin/definitions/customer-form";
    }

    @GetMapping("/customers/{id}/edit")
    public String editCustomerForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            populateCustomerForm(model, masterDataService.getCustomerById(id), true);
            return "admin/definitions/customer-form";
        } catch (IllegalArgumentException e) {
            addErrorFlash(redirectAttributes, e.getMessage());
            return "redirect:/admin/definitions/customers";
        }
    }

    @GetMapping("/customers/api/data")
    @ResponseBody
    public TabulatorResponse<CustomerDto> customersData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "type", required = false) CustomerType type,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {
        return masterDataService.getCustomersPaged(page, size, search, type, sortField, sortDir);
    }

    @PostMapping("/customers/save")
    public String saveCustomer(@ModelAttribute Customer customer, Model model, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveCustomer(customer);
            addSuccessFlash(redirectAttributes, "Müşteri (" + customer.getCompanyName() + ") başarıyla kaydedildi.");
            return "redirect:/admin/definitions/customers";
        } catch (Exception e) {
            log.error("Error saving customer", e);
            populateCustomerForm(model, customer, customer.getId() != null);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/definitions/customer-form";
        }
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
        model.addAttribute("supplierTypes", SupplierType.values());
        model.addAttribute("currentSection", "definitions-suppliers");
        return "admin/definitions/suppliers";
    }

    @GetMapping("/suppliers/create")
    public String createSupplierForm(Model model) {
        populateSupplierForm(model, newSupplier(), false);
        return "admin/definitions/supplier-form";
    }

    @GetMapping("/suppliers/{id}/edit")
    public String editSupplierForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            populateSupplierForm(model, masterDataService.getSupplierById(id), true);
            return "admin/definitions/supplier-form";
        } catch (IllegalArgumentException e) {
            addErrorFlash(redirectAttributes, e.getMessage());
            return "redirect:/admin/definitions/suppliers";
        }
    }

    @GetMapping("/suppliers/api/data")
    @ResponseBody
    public TabulatorResponse<SupplierDto> suppliersData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "type", required = false) SupplierType type,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {
        return masterDataService.getSuppliersPaged(page, size, search, type, sortField, sortDir);
    }

    @PostMapping("/suppliers/save")
    public String saveSupplier(@ModelAttribute Supplier supplier, Model model, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveSupplier(supplier);
            addSuccessFlash(redirectAttributes, "Tedarikçi (" + supplier.getCompanyName() + ") başarıyla kaydedildi.");
            return "redirect:/admin/definitions/suppliers";
        } catch (Exception e) {
            log.error("Error saving supplier", e);
            populateSupplierForm(model, supplier, supplier.getId() != null);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/definitions/supplier-form";
        }
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
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("currentSection", "definitions-cost-centers");
        return "admin/definitions/cost-centers";
    }

    @GetMapping("/cost-centers/create")
    public String createCostCenterForm(Model model) {
        populateCostCenterForm(model, newCostCenter(), false);
        return "admin/definitions/cost-center-form";
    }

    @GetMapping("/cost-centers/{id}/edit")
    public String editCostCenterForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            populateCostCenterForm(model, masterDataService.getCostCenterById(id), true);
            return "admin/definitions/cost-center-form";
        } catch (IllegalArgumentException e) {
            addErrorFlash(redirectAttributes, e.getMessage());
            return "redirect:/admin/definitions/cost-centers";
        }
    }

    @GetMapping("/cost-centers/api/data")
    @ResponseBody
    public TabulatorResponse<CostCenterDto> costCentersData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "unit", required = false) BusinessUnit unit,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {
        return masterDataService.getCostCentersPaged(page, size, search, unit, sortField, sortDir);
    }

    @PostMapping("/cost-centers/save")
    public String saveCostCenter(@ModelAttribute CostCenter costCenter, Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveCostCenter(costCenter);
            addSuccessFlash(redirectAttributes, "Masraf merkezi (" + costCenter.getName() + ") başarıyla kaydedildi.");
            return "redirect:/admin/definitions/cost-centers";
        } catch (Exception e) {
            log.error("Error saving cost center", e);
            populateCostCenterForm(model, costCenter, costCenter.getId() != null);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/definitions/cost-center-form";
        }
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

    private void populateMachineForm(Model model, Machine machine, boolean isEdit) {
        model.addAttribute("machine", machine);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("machineTypes", MachineType.values());
        model.addAttribute("currentSection", "definitions-machines");
    }

    private void populateStockLocationForm(Model model, StockLocation location, boolean isEdit) {
        model.addAttribute("location", location);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("locationTypes", StockLocationType.values());
        model.addAttribute("currentSection", "definitions-locations");
    }

    private void populateQuarryForm(Model model, Quarry quarry, boolean isEdit) {
        model.addAttribute("quarry", quarry);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("currentSection", "definitions-quarries");
    }

    private void populateCustomerForm(Model model, Customer customer, boolean isEdit) {
        model.addAttribute("customer", customer);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("customerTypes", CustomerType.values());
        model.addAttribute("currentSection", "definitions-customers");
    }

    private void populateSupplierForm(Model model, Supplier supplier, boolean isEdit) {
        model.addAttribute("supplier", supplier);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("supplierTypes", SupplierType.values());
        model.addAttribute("currentSection", "definitions-suppliers");
    }

    private void populateCostCenterForm(Model model, CostCenter costCenter, boolean isEdit) {
        model.addAttribute("costCenter", costCenter);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("currentSection", "definitions-cost-centers");
    }

    private static Machine newMachine() {
        Machine machine = new Machine();
        machine.setActive(true);
        machine.setBusinessUnit(BusinessUnit.FACTORY);
        machine.setMachineType(MachineType.GANGSAW);
        return machine;
    }

    private static StockLocation newStockLocation() {
        StockLocation location = new StockLocation();
        location.setActive(true);
        location.setBusinessUnit(BusinessUnit.FACTORY);
        location.setLocationType(StockLocationType.FACTORY_BLOCK_YARD);
        return location;
    }

    private static Quarry newQuarry() {
        Quarry quarry = new Quarry();
        quarry.setSpecificGravity(new BigDecimal("2.70"));
        return quarry;
    }

    private static Customer newCustomer() {
        Customer customer = new Customer();
        customer.setCustomerType(CustomerType.CONSTRUCTION);
        return customer;
    }

    private static Supplier newSupplier() {
        Supplier supplier = new Supplier();
        supplier.setSupplierType(SupplierType.CONSUMABLE);
        return supplier;
    }

    private static CostCenter newCostCenter() {
        CostCenter costCenter = new CostCenter();
        costCenter.setBusinessUnit(BusinessUnit.FACTORY);
        costCenter.setMonthlyBudget(BigDecimal.ZERO);
        return costCenter;
    }

    private void addSuccessFlash(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute("successMessage", message);
    }

    private void addErrorFlash(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute("errorMessage", message);
    }
}
