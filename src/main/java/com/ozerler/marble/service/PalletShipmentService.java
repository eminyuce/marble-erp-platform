package com.ozerler.marble.service;

import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.MaterialLot;
import com.ozerler.marble.model.Pallet;
import com.ozerler.marble.model.PalletItem;
import com.ozerler.marble.model.PalletLocationMovement;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.Shipment;
import com.ozerler.marble.model.ShipmentItem;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.enums.MaterialLotStatus;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.MaterialLotRepository;
import com.ozerler.marble.repository.PalletItemRepository;
import com.ozerler.marble.repository.PalletLocationMovementRepository;
import com.ozerler.marble.repository.PalletRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.ShipmentItemRepository;
import com.ozerler.marble.repository.ShipmentRepository;
import com.ozerler.marble.repository.StockLocationRepository;
import com.ozerler.marble.util.MessageUtils;
import com.ozerler.marble.util.UniqueCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.StockItemRepository;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PalletShipmentService {

    private final PalletRepository palletRepository;
    private final PalletItemRepository palletItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final MaterialLotRepository materialLotRepository;
    private final StockItemRepository stockItemRepository;
    private final SlabRepository slabRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final StockLocationRepository stockLocationRepository;
    private final PalletLocationMovementRepository palletLocationMovementRepository;

    @Transactional
    public Pallet createPallet(String palletCode, Long customerId, Long projectId, String warehouseLocation, BusinessUnit department) {
        if (customerId == null && projectId == null) {
            throw new IllegalArgumentException("Müşteri veya şantiye seçilmelidir");
        }
        Customer customer = customerId != null ? customerRepository.findById(customerId).orElse(null) : null;
        Project project = projectId != null ? projectRepository.findById(projectId).orElse(null) : null;
        StockLocation yard = stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.PALLET_STOCK_YARD)
                .orElse(null);
        Pallet pallet = palletRepository.save(Pallet.builder()
                .palletCode(palletCode != null && !palletCode.isBlank()
                        ? palletCode
                        : UniqueCodes.yearly("PAL", palletRepository::existsByPalletCode))
                .warehouseLocation(warehouseLocation)
                .currentLocation(yard)
                .department(department != null ? department : BusinessUnit.FACTORY)
                .status("PREPARING")
                .customer(customer)
                .project(project)
                .build());
        recordPalletMovement(pallet, null, yard, "Palet oluşturuldu — Paletli Stok (" + (department != null ? department.getDisplayName() : "Fabrika") + ")");
        return pallet;
    }

    @Transactional
    public Pallet createPallet(String palletCode, Long customerId, Long projectId, String warehouseLocation) {
        return createPallet(palletCode, customerId, projectId, warehouseLocation, BusinessUnit.FACTORY);
    }

    @Transactional
    public PalletItem addLot(Long palletId, Long materialLotId, Integer quantity, BigDecimal areaM2) {
        return addItem(palletId, materialLotId, null, null, null, null, null, null, quantity, areaM2);
    }

    @Transactional
    public PalletItem addItem(Long palletId, Long materialLotId, Long stockItemId, Long slabId,
                              String productName, BigDecimal widthCm, BigDecimal lengthCm, BigDecimal thicknessCm,
                              Integer quantity, BigDecimal areaM2) {
        Pallet pallet = palletRepository.findById(palletId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.pallet.not_found", palletId)));
        if ("SHIPPED".equalsIgnoreCase(pallet.getStatus())) {
            throw new IllegalStateException("Sevk edilmiş palete yeni ürün eklenemez.");
        }

        MaterialLot lot = materialLotId != null ? materialLotRepository.findById(materialLotId).orElse(null) : null;
        StockItem stockItem = stockItemId != null ? stockItemRepository.findById(stockItemId).orElse(null) : null;
        Slab slab = slabId != null ? slabRepository.findById(slabId).orElse(null) : null;

        if (lot != null) {
            lot.setStatus(MaterialLotStatus.PALLETIZED);
            if (widthCm == null) widthCm = lot.getWidthCm();
            if (lengthCm == null) lengthCm = lot.getLengthCm();
            if (thicknessCm == null) thicknessCm = lot.getThicknessCm();
            if (productName == null || productName.isBlank()) {
                productName = lot.getLotCode() + (lot.getProductForm() != null ? " — " + lot.getProductForm().getLabel() : "");
            }
        } else if (stockItem != null) {
            if (widthCm == null) widthCm = stockItem.getWidthCm();
            if (lengthCm == null) lengthCm = stockItem.getLengthCm();
            if (thicknessCm == null) thicknessCm = stockItem.getThicknessCm();
            if (productName == null || productName.isBlank()) {
                productName = stockItem.getItemCode() + " (" + (stockItem.getStoneType() != null ? stockItem.getStoneType() : "Ebatlı") + ")";
            }
        } else if (slab != null) {
            if (widthCm == null) widthCm = slab.getWidthCm();
            if (lengthCm == null) lengthCm = slab.getLengthCm();
            if (thicknessCm == null) thicknessCm = slab.getThicknessCm();
            if (productName == null || productName.isBlank()) {
                productName = slab.getSlabCode() + " (Plaka)";
            }
        }

        int qty = quantity != null && quantity > 0 ? quantity : 1;
        BigDecimal calculatedArea = areaM2;
        if ((calculatedArea == null || calculatedArea.compareTo(BigDecimal.ZERO) <= 0) && widthCm != null && lengthCm != null) {
            calculatedArea = widthCm.multiply(lengthCm).divide(new BigDecimal("10000"), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(qty));
        }
        if (calculatedArea == null || calculatedArea.compareTo(BigDecimal.ZERO) <= 0) {
            calculatedArea = lot != null ? lot.getTotalAreaM2() : (slab != null ? slab.getSurfaceAreaM2() : BigDecimal.ZERO);
        }

        PalletItem item = palletItemRepository.save(PalletItem.builder()
                .pallet(pallet)
                .materialLot(lot)
                .stockItem(stockItem)
                .slab(slab)
                .productName(productName)
                .widthCm(widthCm)
                .lengthCm(lengthCm)
                .thicknessCm(thicknessCm)
                .quantity(qty)
                .areaM2(calculatedArea != null ? calculatedArea : BigDecimal.ZERO)
                .build());

        pallet.setStatus("READY");
        return item;
    }

    @Transactional
    public Shipment createShipment(Long palletId, Long customerId, Long projectId, String waybillNo,
                                   String vehiclePlate, String driverName, BigDecimal freightCost) {
        if (customerId == null && projectId == null) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.shipment.target.required"));
        }
        Pallet pallet = palletRepository.findById(palletId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.pallet.not_found", palletId)));
        Customer customer = customerId != null ? customerRepository.findById(customerId).orElse(null) : null;
        Project project = projectId != null ? projectRepository.findById(projectId).orElse(null) : null;
        Shipment shipment = shipmentRepository.save(Shipment.builder()
                .waybillNo(waybillNo != null && !waybillNo.isBlank()
                        ? waybillNo
                        : UniqueCodes.yearly("IRS", shipmentRepository::existsByWaybillNo))
                .project(project)
                .customer(customer)
                .vehiclePlate(vehiclePlate)
                .driverName(driverName)
                .departureTime(LocalDateTime.now())
                .freightCost(freightCost != null ? freightCost : BigDecimal.ZERO)
                .deliveryStatus("IN_TRANSIT")
                .build());
        shipmentItemRepository.save(ShipmentItem.builder()
                .shipment(shipment)
                .pallet(pallet)
                .quantity(1)
                .build());
        StockLocation from = pallet.getCurrentLocation();
        pallet.setStatus("SHIPPED");
        palletItemRepository.findByPalletId(palletId)
                .forEach(item -> item.getMaterialLot().setStatus(MaterialLotStatus.SHIPPED));
        recordPalletMovement(pallet, from, from, "Palet sevk edildi"
                + (shipment.getWaybillNo() != null ? " — " + shipment.getWaybillNo() : ""));
        return shipment;
    }

    @Transactional(readOnly = true)
    public List<Pallet> palletsByLocation(StockLocationType locationType) {
        if (locationType == null) {
            return pallets();
        }
        return palletRepository.findByCurrentLocation_LocationType(locationType);
    }

    private void recordPalletMovement(Pallet pallet, StockLocation from, StockLocation to, String description) {
        if (pallet == null || to == null) {
            return;
        }
        palletLocationMovementRepository.save(PalletLocationMovement.builder()
                .pallet(pallet)
                .fromLocation(from)
                .toLocation(to)
                .description(description)
                .build());
    }

    @Transactional(readOnly = true)
    public List<Pallet> pallets() {
        List<Pallet> pallets = palletRepository.findAllWithLocationAndSlabs();
        if (!pallets.isEmpty()) {
            List<PalletItem> items = palletItemRepository.findByPalletIn(pallets);
            Map<Long, List<PalletItem>> itemsByPallet = items.stream()
                    .filter(i -> i.getPallet() != null)
                    .collect(Collectors.groupingBy(i -> i.getPallet().getId()));
            for (Pallet p : pallets) {
                p.setItems(itemsByPallet.getOrDefault(p.getId(), new ArrayList<>()));
            }
        }
        return pallets;
    }

    @Transactional(readOnly = true)
    public List<Pallet> shippablePallets() {
        return pallets().stream()
                .filter(p -> !"SHIPPED".equalsIgnoreCase(p.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Shipment> shipments() {
        return shipmentRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Customer> customers() {
        return customerRepository.findAllByOrderByCompanyNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Project> projects() {
        return projectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<MaterialLot> availableLots() {
        return materialLotRepository.findByStatus(MaterialLotStatus.AVAILABLE);
    }
}
