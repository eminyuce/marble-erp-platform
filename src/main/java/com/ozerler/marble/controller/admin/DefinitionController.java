package com.ozerler.marble.controller.admin;

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

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.CostCenterDto;
import com.ozerler.marble.dto.CustomerDto;
import com.ozerler.marble.dto.MachineDto;
import com.ozerler.marble.dto.QuarryDto;
import com.ozerler.marble.dto.StockLocationDto;
import com.ozerler.marble.dto.SupplierDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.form.CostCenterForm;
import com.ozerler.marble.dto.form.CustomerForm;
import com.ozerler.marble.dto.form.MachineForm;
import com.ozerler.marble.dto.form.QuarryForm;
import com.ozerler.marble.dto.form.StockLocationForm;
import com.ozerler.marble.dto.form.SupplierForm;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CustomerType;
import com.ozerler.marble.model.enums.MachineType;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.model.enums.SupplierType;
import com.ozerler.marble.service.MasterDataService;
import com.ozerler.marble.web.CostCenterGrid;
import com.ozerler.marble.web.CustomerGrid;
import com.ozerler.marble.web.GridRequests;
import com.ozerler.marble.web.MachineGrid;
import com.ozerler.marble.web.QuarryGrid;
import com.ozerler.marble.web.StockLocationGrid;
import com.ozerler.marble.web.SupplierGrid;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin/definitions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class DefinitionController extends AbstractController {

    private final MasterDataService masterDataService;

    @Getter
    @Setter
    public static class DefinitionPageQuery {
        private Integer page;
        private Integer size;
        private String search;
        private String sort;
        private String dir;
        private String sortField;
        private String sortDir;

        int pageNo() {
            return GridRequests.page(page);
        }

        int pageSize() {
            return GridRequests.size(size);
        }

        String sortKey() {
            return GridRequests.sort(sort, sortField);
        }

        String sortDirection() {
            return GridRequests.direction(dir, sortDir);
        }
    }

    @GetMapping
    public String index() {
        return "redirect:/admin/definitions/machines";
    }

    // =========================================================================
    // MACHINES
    // =========================================================================

    @GetMapping("/machines")
    public String listMachines(@ModelAttribute DefinitionPageQuery query,
                               @RequestParam(value = "unit", required = false) BusinessUnit unit,
                               Model model) {
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("machineTypes", java.util.Arrays.stream(MachineType.values()).filter(t -> !t.isElectric()).toList());
        model.addAttribute("currentSection", "definitions-machines");
        model.addAttribute("grid", MachineGrid.view(
                masterDataService.getMachinesPaged(query.pageNo(), query.pageSize(), query.getSearch(), unit,
                        query.sortKey(), query.sortDirection()),
                query.pageNo(), query.pageSize(), query.sortKey(), query.sortDirection()));
        return "admin/definitions/machines";
    }

    @GetMapping("/machines/create")
    public String createMachineForm(Model model) {
        populateMachineForm(model, new MachineForm(), false);
        return "admin/definitions/machine-form";
    }

    @GetMapping("/machines/{id}/edit")
    public String editMachineForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            populateMachineForm(model, masterDataService.machineForm(id), true);
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
    public String saveMachine(@ModelAttribute MachineForm machine, Model model, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveMachineForm(machine);
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
            MachineForm machine = masterDataService.toggleMachineForm(id);
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
    public String listStockLocations(@ModelAttribute DefinitionPageQuery query,
                                     @RequestParam(value = "unit", required = false) BusinessUnit unit,
                                     Model model) {
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("locationTypes", StockLocationType.values());
        model.addAttribute("currentSection", "definitions-locations");
        model.addAttribute("grid", StockLocationGrid.view(
                masterDataService.getStockLocationsPaged(query.pageNo(), query.pageSize(), query.getSearch(), unit,
                        query.sortKey(), query.sortDirection()),
                query.pageNo(), query.pageSize(), query.sortKey(), query.sortDirection()));
        return "admin/definitions/stock-locations";
    }

    @GetMapping("/stock-locations/create")
    public String createStockLocationForm(Model model) {
        populateStockLocationForm(model, new StockLocationForm(), false);
        return "admin/definitions/stock-location-form";
    }

    @GetMapping("/stock-locations/{id}/edit")
    public String editStockLocationForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            populateStockLocationForm(model, masterDataService.stockLocationForm(id), true);
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
    public String saveStockLocation(@ModelAttribute StockLocationForm location, Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveStockLocationForm(location);
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
            StockLocationForm loc = masterDataService.toggleStockLocationForm(id);
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
    public String listQuarries(@ModelAttribute DefinitionPageQuery query, Model model) {
        model.addAttribute("currentSection", "definitions-quarries");
        model.addAttribute("grid", QuarryGrid.view(
                masterDataService.getQuarriesPaged(query.pageNo(), query.pageSize(), query.getSearch(),
                        query.sortKey(), query.sortDirection()),
                query.pageNo(), query.pageSize(), query.sortKey(), query.sortDirection()));
        return "admin/definitions/quarries";
    }

    @GetMapping("/quarries/create")
    public String createQuarryForm(Model model) {
        populateQuarryForm(model, new QuarryForm(), false);
        return "admin/definitions/quarry-form";
    }

    @GetMapping("/quarries/{id}/edit")
    public String editQuarryForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            populateQuarryForm(model, masterDataService.quarryForm(id), true);
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
    public String saveQuarry(@ModelAttribute QuarryForm quarry, Model model, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveQuarryForm(quarry);
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
    public String listCustomers(@ModelAttribute DefinitionPageQuery query,
                                @RequestParam(value = "type", required = false) CustomerType type,
                                @RequestParam(value = "unit", required = false) BusinessUnit unit,
                                Model model) {
        model.addAttribute("customerTypes", CustomerType.values());
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("selectedUnit", unit != null ? unit.name() : "");
        model.addAttribute("currentSection", "definitions-customers");
        model.addAttribute("grid", CustomerGrid.view(
                masterDataService.getCustomersPaged(query.pageNo(), query.pageSize(), query.getSearch(), type, unit,
                        query.sortKey(), query.sortDirection()),
                query.pageNo(), query.pageSize(), query.sortKey(), query.sortDirection()));
        return "admin/definitions/customers";
    }

    @GetMapping("/customers/create")
    public String createCustomerForm(@RequestParam(value = "unit", required = false) BusinessUnit unit, Model model) {
        CustomerForm customer = new CustomerForm();
        if (unit != null) {
            customer.setBusinessUnit(unit);
            if (unit == BusinessUnit.SITE) {
                customer.setCustomerType(CustomerType.CONSTRUCTION);
            }
        }
        populateCustomerForm(model, customer, false);
        return "admin/definitions/customer-form";
    }

    @GetMapping("/customers/{id}/edit")
    public String editCustomerForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            populateCustomerForm(model, masterDataService.customerForm(id), true);
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
            @RequestParam(value = "unit", required = false) BusinessUnit unit,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {
        return masterDataService.getCustomersPaged(page, size, search, type, unit, sortField, sortDir);
    }

    @PostMapping("/customers/save")
    public String saveCustomer(@ModelAttribute CustomerForm customer, Model model, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveCustomerForm(customer);
            addSuccessFlash(redirectAttributes, "Müşteri (" + customer.getCompanyName() + ") başarıyla kaydedildi.");
            String redirectUrl = "redirect:/admin/definitions/customers";
            if (customer.getBusinessUnit() != null) {
                redirectUrl += "?unit=" + customer.getBusinessUnit().name();
            }
            return redirectUrl;
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
    public String listSuppliers(@ModelAttribute DefinitionPageQuery query,
                                @RequestParam(value = "type", required = false) SupplierType type,
                                @RequestParam(value = "unit", required = false) BusinessUnit unit,
                                Model model) {
        model.addAttribute("supplierTypes", SupplierType.values());
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("currentSection", "definitions-suppliers");
        model.addAttribute("grid", SupplierGrid.view(
                masterDataService.getSuppliersPaged(query.pageNo(), query.pageSize(), query.getSearch(), type, unit,
                        query.sortKey(), query.sortDirection()),
                query.pageNo(), query.pageSize(), query.sortKey(), query.sortDirection()));
        return "admin/definitions/suppliers";
    }

    @GetMapping("/suppliers/create")
    public String createSupplierForm(Model model) {
        populateSupplierForm(model, new SupplierForm(), false);
        return "admin/definitions/supplier-form";
    }

    @GetMapping("/suppliers/{id}/edit")
    public String editSupplierForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            populateSupplierForm(model, masterDataService.supplierForm(id), true);
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
            @RequestParam(value = "unit", required = false) BusinessUnit unit,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {
        return masterDataService.getSuppliersPaged(page, size, search, type, unit, sortField, sortDir);
    }

    @PostMapping("/suppliers/save")
    public String saveSupplier(@ModelAttribute SupplierForm supplier, Model model, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveSupplierForm(supplier);
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
    public String listCostCenters(@ModelAttribute DefinitionPageQuery query,
                                  @RequestParam(value = "unit", required = false) BusinessUnit unit,
                                  Model model) {
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("currentSection", "definitions-cost-centers");
        model.addAttribute("grid", CostCenterGrid.view(
                masterDataService.getCostCentersPaged(query.pageNo(), query.pageSize(), query.getSearch(), unit,
                        query.sortKey(), query.sortDirection()),
                query.pageNo(), query.pageSize(), query.sortKey(), query.sortDirection()));
        return "admin/definitions/cost-centers";
    }

    @GetMapping("/cost-centers/create")
    public String createCostCenterForm(Model model) {
        populateCostCenterForm(model, new CostCenterForm(), false);
        return "admin/definitions/cost-center-form";
    }

    @GetMapping("/cost-centers/{id}/edit")
    public String editCostCenterForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            populateCostCenterForm(model, masterDataService.costCenterForm(id), true);
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
    public String saveCostCenter(@ModelAttribute CostCenterForm costCenter, Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            masterDataService.saveCostCenterForm(costCenter);
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

    private void populateMachineForm(Model model, MachineForm machine, boolean isEdit) {
        model.addAttribute("machine", machine);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("machineTypes", java.util.Arrays.stream(MachineType.values()).filter(t -> !t.isElectric()).toList());
        model.addAttribute("currentSection", "definitions-machines");
    }

    private void populateStockLocationForm(Model model, StockLocationForm location, boolean isEdit) {
        model.addAttribute("location", location);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("locationTypes", StockLocationType.values());
        model.addAttribute("currentSection", "definitions-locations");
    }

    private void populateQuarryForm(Model model, QuarryForm quarry, boolean isEdit) {
        model.addAttribute("quarry", quarry);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("currentSection", "definitions-quarries");
    }

    private void populateCustomerForm(Model model, CustomerForm customer, boolean isEdit) {
        model.addAttribute("customer", customer);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("customerTypes", CustomerType.values());
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("currentSection", "definitions-customers");
    }

    private void populateSupplierForm(Model model, SupplierForm supplier, boolean isEdit) {
        model.addAttribute("supplier", supplier);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("supplierTypes", SupplierType.values());
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("currentSection", "definitions-suppliers");
    }

    private void populateCostCenterForm(Model model, CostCenterForm costCenter, boolean isEdit) {
        model.addAttribute("costCenter", costCenter);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("currentSection", "definitions-cost-centers");
    }

    private void addSuccessFlash(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute("successMessage", message);
    }

    private void addErrorFlash(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute("errorMessage", message);
    }
}
