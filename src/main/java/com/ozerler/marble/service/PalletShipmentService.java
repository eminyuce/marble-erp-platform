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
import java.util.List;

@Service
@RequiredArgsConstructor
public class PalletShipmentService {

    private final PalletRepository palletRepository;
    private final PalletItemRepository palletItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final MaterialLotRepository materialLotRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final StockLocationRepository stockLocationRepository;
    private final PalletLocationMovementRepository palletLocationMovementRepository;

    @Transactional
    public Pallet createPallet(String palletCode, Long customerId, Long projectId, String warehouseLocation) {
        if (customerId == null && projectId == null) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.shipment.target.required"));
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
                .status("PREPARING")
                .customer(customer)
                .project(project)
                .build());
        recordPalletMovement(pallet, null, yard, "Palet oluşturuldu — Paletli Stok");
        return pallet;
    }

    @Transactional
    public PalletItem addLot(Long palletId, Long materialLotId, Integer quantity, BigDecimal areaM2) {
        Pallet pallet = palletRepository.findById(palletId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.pallet.not_found", palletId)));
        MaterialLot lot = materialLotRepository.findById(materialLotId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.material_lot.not_found", materialLotId)));
        lot.setStatus(MaterialLotStatus.PALLETIZED);
        PalletItem item = palletItemRepository.save(PalletItem.builder()
                .pallet(pallet)
                .materialLot(lot)
                .quantity(quantity != null ? quantity : 1)
                .areaM2(areaM2 != null ? areaM2 : lot.getTotalAreaM2())
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
        return palletRepository.findAllWithLocationAndSlabs();
    }

    @Transactional(readOnly = true)
    public List<Shipment> shipments() {
        return shipmentRepository.findAll();
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
