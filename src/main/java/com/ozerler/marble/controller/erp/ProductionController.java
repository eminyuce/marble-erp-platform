package com.ozerler.marble.controller.erp;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.ProductionOrderDto;
import com.ozerler.marble.dto.SlabDto;
import com.ozerler.marble.dto.SlabLabelDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.ChamferStatus;
import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.QuantityUnit;
import com.ozerler.marble.model.enums.ScrapReasonCode;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.SurfaceFinish;
import com.ozerler.marble.service.FactoryProductionService;
import com.ozerler.marble.service.PalletShipmentService;
import com.ozerler.marble.service.ProductionService;
import com.ozerler.marble.service.QuarryBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Locale;

@Controller
@RequestMapping("/production")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;
    private final FactoryProductionService factoryProductionService;
    private final PalletShipmentService palletShipmentService;
    private final QuarryBlockService quarryBlockService;
    private final MessageSource messageSource;

    @GetMapping
    public String productionIndex() {
        return "erp/production/index";
    }

    @GetMapping("/api/orders")
    @ResponseBody
    public TabulatorResponse<ProductionOrderDto> getOrdersData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {

        return productionService.getOrdersPaged(page, size, search, sortField, sortDir);
    }

    @GetMapping("/create")
    public String showCreateForm(Locale locale, Model model) {
        populateProductionForm(model, locale);
        return "erp/production/order-form";
    }

    @PostMapping("/create")
    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    public String recordCutting(@RequestParam("blockId") Long blockId,
                                @RequestParam(value = "orderNo", required = false) String orderNo,
                                @RequestParam(value = "processType", defaultValue = "GANGSAW_CUTTING") FactoryProcessType processType,
                                @RequestParam(value = "machineId", required = false) Long machineId,
                                @RequestParam("machineName") String machineName,
                                @RequestParam("durationHours") BigDecimal durationHours,
                                @RequestParam("electricityKwh") BigDecimal electricityKwh,
                                @RequestParam("bladeWearMm") BigDecimal bladeWearMm,
                                @RequestParam("directCuttingExpense") BigDecimal directCuttingExpense,
                                @RequestParam("operatorName") String operatorName,
                                @RequestParam(value = "notes", required = false) String notes,
                                @RequestParam(value = "slabCountGradeExtra", defaultValue = "0") int extraCount,
                                @RequestParam(value = "slabCountGradeA", defaultValue = "0") int slabCountGradeA,
                                @RequestParam(value = "slabCountGradeB", defaultValue = "0") int slabCountGradeB,
                                @RequestParam(value = "slabCountGradeC", defaultValue = "0") int slabCountGradeC,
                                @RequestParam("slabWidthCm") BigDecimal slabWidthCm,
                                @RequestParam("slabLengthCm") BigDecimal slabLengthCm,
                                @RequestParam("thicknessCm") BigDecimal thicknessCm,
                                @RequestParam(value = "inputQuantity", required = false) BigDecimal inputQuantity,
                                @RequestParam(value = "outputQuantity", required = false) BigDecimal outputQuantity,
                                @RequestParam(value = "wasteQuantity", required = false) BigDecimal wasteQuantity,
                                @RequestParam(value = "wasteUnit", required = false) QuantityUnit wasteUnit,
                                @RequestParam(value = "scrapReason", required = false) ScrapReasonCode scrapReason,
                                @RequestParam(value = "scrapWeightKg", required = false) BigDecimal scrapWeightKg,
                                @RequestParam(value = "scrapNotes", required = false) String scrapNotes,
                                Locale locale,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        try {
            factoryProductionService.recordCutting(new FactoryProductionService.CuttingRequest(
                    blockId, orderNo, machineId, machineName, processType, durationHours, electricityKwh,
                    bladeWearMm, directCuttingExpense, operatorName, notes, extraCount,
                    slabCountGradeA, slabCountGradeB, slabCountGradeC, slabWidthCm, slabLengthCm, thicknessCm,
                    inputQuantity, outputQuantity, wasteQuantity, wasteUnit, scrapReason, scrapWeightKg, scrapNotes));

            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.production.gangsaw.cut.success", null, locale));
            return "redirect:/production";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            populateProductionForm(model, locale);
            return "erp/production/order-form";
        }
    }

    @GetMapping("/accept")
    public String acceptPage(Model model) {
        populateAcceptPage(model);
        return "erp/production/accept";
    }

    @PostMapping("/accept")
    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    public String acceptBlock(@RequestParam("blockId") Long blockId,
                              @RequestParam(value = "machineId", required = false) Long machineId,
                              @RequestParam(value = "responsibleName", required = false) String responsibleName,
                              Locale locale,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            factoryProductionService.acceptBlock(blockId, machineId, responsibleName);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.production.accept.success", null, locale));
            return "redirect:/production/accept";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            populateAcceptPage(model);
            return "erp/production/accept";
        }
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable("id") Long id, Model model) {
        ProductionOrder order = productionService.getOrderWithDetails(id);
        model.addAttribute("order", order);
        return "erp/production/detail";
    }

    @GetMapping("/orders/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Locale locale, Model model) {
        ProductionOrder order = productionService.getOrderById(id);
        populateProductionForm(model, locale);
        model.addAttribute("record", order);
        model.addAttribute("isEdit", true);
        model.addAttribute("pageTitle", "Fabrika İş Emri Düzenle: " + order.getOrderNo());
        return "erp/production/order-form";
    }

    @PostMapping("/orders/{id}/edit")
    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    public String updateOrder(@PathVariable("id") Long id,
                              @RequestParam("machineName") String machineName,
                              @RequestParam("operatorName") String operatorName,
                              @RequestParam("durationHours") BigDecimal durationHours,
                              @RequestParam("electricityKwh") BigDecimal electricityKwh,
                              @RequestParam("bladeWearMm") BigDecimal bladeWearMm,
                              @RequestParam(value = "notes", required = false) String notes,
                              Locale locale,
                              RedirectAttributes redirectAttributes) {
        factoryProductionService.updateProductionOrder(id, machineName, operatorName, durationHours,
                electricityKwh, bladeWearMm, notes);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.production.order.update.success", null, locale));
        return "redirect:/production/orders/" + id;
    }

    @GetMapping("/polish")
    public String polishForm(Model model) {
        model.addAttribute("workOrders", factoryProductionService.listWorkOrderSummaries());
        model.addAttribute("factoryMachines", factoryProductionService.factoryMachines());
        model.addAttribute("processTypes", new FactoryProcessType[]{
                FactoryProcessType.SLAB_POLISHING, FactoryProcessType.STRIP_POLISHING, FactoryProcessType.BRIDGE_SAW_SIZING});
        model.addAttribute("chamferStatuses", ChamferStatus.values());
        return "erp/production/polish";
    }

    @PostMapping("/polish")
    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    public String recordPolish(@RequestParam("workOrderId") Long workOrderId,
                               @RequestParam("processType") FactoryProcessType processType,
                               @RequestParam(value = "machineId", required = false) Long machineId,
                               @RequestParam("operatorName") String operatorName,
                               @RequestParam("inputM2") BigDecimal inputM2,
                               @RequestParam("outputM2") BigDecimal outputM2,
                               @RequestParam("wasteM2") BigDecimal wasteM2,
                               @RequestParam(value = "chamferStatus", required = false) ChamferStatus chamferStatus,
                               @RequestParam(value = "notes", required = false) String notes,
                               Locale locale,
                               RedirectAttributes redirectAttributes) {
        factoryProductionService.recordSurfaceOperation(workOrderId, processType, machineId, operatorName,
                inputM2, outputM2, wasteM2, chamferStatus, notes);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.production.polish.success", null, locale));
        return "redirect:/production/polish";
    }

    @GetMapping("/pallets")
    public String pallets(Model model) {
        model.addAttribute("pallets", palletShipmentService.pallets());
        model.addAttribute("shipments", palletShipmentService.shipments());
        model.addAttribute("lots", palletShipmentService.availableLots());
        model.addAttribute("customers", palletShipmentService.customers());
        model.addAttribute("projects", palletShipmentService.projects());
        return "erp/production/pallets";
    }

    @PostMapping("/pallets")
    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    public String createPallet(@RequestParam(value = "palletCode", required = false) String palletCode,
                               @RequestParam(value = "customerId", required = false) Long customerId,
                               @RequestParam(value = "projectId", required = false) Long projectId,
                               @RequestParam(value = "warehouseLocation", required = false) String warehouseLocation,
                               Locale locale,
                               RedirectAttributes redirectAttributes) {
        palletShipmentService.createPallet(palletCode, customerId, projectId, warehouseLocation);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.production.pallet.success", null, locale));
        return "redirect:/production/pallets";
    }

    @PostMapping("/pallets/{id}/items")
    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    public String addPalletItem(@PathVariable("id") Long palletId,
                                @RequestParam("materialLotId") Long materialLotId,
                                @RequestParam(value = "quantity", required = false) Integer quantity,
                                @RequestParam(value = "areaM2", required = false) BigDecimal areaM2) {
        palletShipmentService.addLot(palletId, materialLotId, quantity, areaM2);
        return "redirect:/production/pallets";
    }

    @PostMapping("/shipments")
    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    public String createShipment(@RequestParam("palletId") Long palletId,
                                 @RequestParam(value = "customerId", required = false) Long customerId,
                                 @RequestParam(value = "projectId", required = false) Long projectId,
                                 @RequestParam(value = "waybillNo", required = false) String waybillNo,
                                 @RequestParam("vehiclePlate") String vehiclePlate,
                                 @RequestParam("driverName") String driverName,
                                 @RequestParam(value = "freightCost", required = false) BigDecimal freightCost,
                                 Locale locale,
                                 RedirectAttributes redirectAttributes) {
        palletShipmentService.createShipment(palletId, customerId, projectId, waybillNo, vehiclePlate, driverName, freightCost);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.production.shipment.success", null, locale));
        return "redirect:/production/pallets";
    }

    @GetMapping("/tablet")
    public String tablet(@RequestParam(value = "operatorName", required = false) String operatorName, Model model) {
        model.addAttribute("operations", factoryProductionService.tabletQueue(operatorName));
        model.addAttribute("operatorName", operatorName);
        model.addAttribute("workOrders", factoryProductionService.listWorkOrderSummaries());
        model.addAttribute("processTypes", FactoryProcessType.values());
        model.addAttribute("factoryMachines", factoryProductionService.factoryMachines());
        return "erp/production/tablet";
    }

    @PostMapping("/tablet/plan")
    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    public String planTablet(@RequestParam("workOrderId") Long workOrderId,
                             @RequestParam("processType") FactoryProcessType processType,
                             @RequestParam(value = "machineId", required = false) Long machineId,
                             @RequestParam(value = "operatorName", required = false) String operatorName,
                             Locale locale,
                             RedirectAttributes redirectAttributes) {
        factoryProductionService.planOperation(workOrderId, processType, machineId, operatorName);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.production.plan.success", null, locale));
        return "redirect:/production/tablet";
    }

    @PostMapping("/tablet/{id}/start")
    @PreAuthorize(Constants.PRE_AUTH_OPERATOR_WRITE)
    public String startOp(@PathVariable("id") Long id,
                          @RequestParam(value = "operatorName", required = false) String operatorName,
                          Locale locale,
                          RedirectAttributes redirectAttributes) {
        factoryProductionService.startAssignedOperation(id, operatorName);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.production.tablet.start.success", null, locale));
        return "redirect:/production/tablet";
    }

    @PostMapping("/tablet/{id}/finish")
    @PreAuthorize(Constants.PRE_AUTH_OPERATOR_WRITE)
    public String finishOp(@PathVariable("id") Long id,
                           @RequestParam("inputQuantity") BigDecimal input,
                           @RequestParam("outputQuantity") BigDecimal output,
                           @RequestParam("wasteQuantity") BigDecimal waste,
                           @RequestParam(value = "wasteUnit", required = false) QuantityUnit wasteUnit,
                           @RequestParam(value = "chamferStatus", required = false) ChamferStatus chamferStatus,
                           Locale locale,
                           RedirectAttributes redirectAttributes) {
        factoryProductionService.finishAssignedOperation(id, input, output, waste, wasteUnit, chamferStatus);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.production.tablet.finish.success", null, locale));
        return "redirect:/production/tablet";
    }

    @GetMapping("/slabs")
    public String slabsView() {
        return "erp/production/slabs";
    }

    @GetMapping("/api/slabs")
    @ResponseBody
    public TabulatorResponse<SlabDto> getSlabsData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {

        return productionService.getSlabsPaged(page, size, search, sortField, sortDir);
    }

    @GetMapping("/slabs/{id}")
    public String slabDetail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("slab", productionService.getSlabWithDetails(id));
        return "erp/production/slab-detail";
    }

    @GetMapping("/slabs/{id}/edit")
    public String showSlabEditForm(@PathVariable("id") Long id, Locale locale, Model model) {
        populateSlabEditForm(model, productionService.getSlabWithDetails(id), locale);
        return "erp/production/slab-form";
    }

    @PostMapping("/slabs/{id}/edit")
    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    public String updateSlab(@PathVariable("id") Long id,
                             @RequestParam("slabCode") String slabCode,
                             @RequestParam("thicknessCm") BigDecimal thicknessCm,
                             @RequestParam("widthCm") BigDecimal widthCm,
                             @RequestParam("lengthCm") BigDecimal lengthCm,
                             @RequestParam("surfaceFinish") SurfaceFinish surfaceFinish,
                             @RequestParam("qualityGrade") QualityGrade qualityGrade,
                             @RequestParam(value = "glossLevel", required = false) Integer glossLevel,
                             @RequestParam("costPerM2") BigDecimal costPerM2,
                             @RequestParam("status") SlabStatus status,
                             Locale locale,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            productionService.updateSlab(id, slabCode, thicknessCm, widthCm, lengthCm,
                    surfaceFinish, qualityGrade, glossLevel, costPerM2, status);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.slab.update.success", null, locale));
            return "redirect:/production/slabs";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            try {
                populateSlabEditForm(model, productionService.getSlabWithDetails(id), locale);
            } catch (Exception ignored) {
                model.addAttribute("pageTitle", messageSource.getMessage("erp.slab.title.edit", null, locale));
            }
            return "erp/production/slab-form";
        }
    }

    @GetMapping("/slabs/{id}/label")
    public String slabLabel(@PathVariable("id") Long id, Model model) {
        SlabLabelDto label = productionService.getSlabLabelData(id, "http://localhost:8080/passport/");

        model.addAttribute("slab", label.getSlab());
        model.addAttribute("qrCodeBase64", label.getQrCodeBase64());
        model.addAttribute("blockCode", label.getBlockCode());
        model.addAttribute("stoneType", label.getStoneType());
        model.addAttribute("quarryName", label.getQuarryName());

        return "erp/production/slab-label";
    }

    private void populateAcceptPage(Model model) {
        model.addAttribute("workOrders", factoryProductionService.listWorkOrderSummaries());
        model.addAttribute("dispatchedBlocks", quarryBlockService.getDispatchedBlocks());
        model.addAttribute("factoryMachines", factoryProductionService.factoryMachines());
    }

    private void populateProductionForm(Model model, Locale locale) {
        model.addAttribute("availableBlocks", quarryBlockService.getAvailableBlocksForProduction());
        model.addAttribute("scrapReasons", ScrapReasonCode.values());
        model.addAttribute("factoryMachines", factoryProductionService.factoryMachines());
        model.addAttribute("cuttingTypes", new FactoryProcessType[]{
                FactoryProcessType.GANGSAW_CUTTING, FactoryProcessType.ST_CUTTING});
        model.addAttribute("wasteUnits", QuantityUnit.values());
        model.addAttribute("pageTitle", messageSource.getMessage("erp.production.title.create", null, locale));
    }

    private void populateSlabEditForm(Model model, Slab slab, Locale locale) {
        model.addAttribute("record", slab);
        model.addAttribute("slab", slab);
        model.addAttribute("qualityGrades", QualityGrade.values());
        model.addAttribute("surfaceFinishes", SurfaceFinish.values());
        model.addAttribute("slabStatuses", SlabStatus.values());
        model.addAttribute("pageTitle", messageSource.getMessage("erp.slab.title.edit", null, locale)
                + ": " + slab.getSlabCode());
    }
}
