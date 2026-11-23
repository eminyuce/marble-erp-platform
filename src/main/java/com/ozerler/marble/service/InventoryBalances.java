package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.MachineFuelEntry;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.enums.StockProductType;
import com.ozerler.marble.repository.MachineFuelEntryRepository;
import com.ozerler.marble.repository.MachineRepository;
import com.ozerler.marble.repository.StockItemRepository;

/**
 * Shared stock-balance updates for quarry and factory inventory.
 * Callers keep their own movement text, cost-center lookup, and default prices.
 */
final class InventoryBalances {

    private InventoryBalances() {
    }

    record FuelDraft(Machine machine, LocalDate entryDate, BigDecimal litres, BigDecimal unitPrice,
                     BigDecimal workingHoursOrKm, String receiptNo, String issuedBy, String receivedBy,
                     String notes) {
    }

    record SavedFuel(MachineFuelEntry entry, BigDecimal totalNetCost) {
    }

    record StockWithdrawal(StockItem item, BigDecimal netCost, Machine machine) {
    }

    static StockItem addIncomingFuel(StockItem tank, BigDecimal litres, BigDecimal netUnitPrice,
                                     StockItemRepository stockItemRepository) {
        BigDecimal oldQty;
        if (tank.getQuantity() != null) {
            oldQty = tank.getQuantity();
        } else {
            oldQty = BigDecimal.ZERO;
        }
        BigDecimal newQty = oldQty.add(litres);
        tank.setQuantity(newQty);

        if (netUnitPrice != null && netUnitPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal oldCost = oldQty.multiply(Objects.requireNonNullElse(tank.getUnitPrice(), BigDecimal.ZERO));
            BigDecimal newCost = litres.multiply(netUnitPrice);
            BigDecimal avgPrice = newCost.add(oldCost).divide(newQty, 2, RoundingMode.HALF_UP);
            tank.setUnitPrice(avgPrice);
        }
        return stockItemRepository.save(tank);
    }

    static SavedFuel recordFuelEntry(MachineFuelEntryRepository fuelEntryRepository, FuelDraft draft) {
        BigDecimal totalNetCost = draft.litres().multiply(draft.unitPrice()).setScale(2, RoundingMode.HALF_UP);
        LocalDate entryDate;
        if (draft.entryDate() != null) {
            entryDate = draft.entryDate();
        } else {
            entryDate = LocalDate.now();
        }
        MachineFuelEntry entry = MachineFuelEntry.builder()
                .machine(draft.machine())
                .entryDate(entryDate)
                .litres(draft.litres())
                .pricePerLitre(draft.unitPrice())
                .workingHoursOrKm(draft.workingHoursOrKm())
                .receiptNo(draft.receiptNo())
                .issuedBy(draft.issuedBy())
                .receivedBy(draft.receivedBy())
                .notes(draft.notes())
                .build();
        entry.calculateTotal();
        return new SavedFuel(fuelEntryRepository.save(entry), totalNetCost);
    }

    static StockItem addIncomingConsumable(StockItemRepository stockItemRepository, String code, String description,
                                           String fallbackDescription, StockLocation location, String unit,
                                           BigDecimal netUnitPrice, String notes, BigDecimal quantity) {
        StockItem item = stockItemRepository.findByItemCode(code)
                .orElseGet(() -> {
                    String descriptionValue;
                    if (description != null) {
                        descriptionValue = description.trim();
                    } else {
                        descriptionValue = fallbackDescription;
                    }
                    String unitValue;
                    if (unit != null && !unit.isBlank()) {
                        unitValue = unit.trim();
                    } else {
                        unitValue = "adet";
                    }
                    return StockItem.builder()
                            .itemCode(code)
                            .description(descriptionValue)
                            .productType(StockProductType.CONSUMABLE)
                            .stockLocation(location)
                            .quantity(BigDecimal.ZERO)
                            .unit(unitValue)
                            .unitPrice(Objects.requireNonNullElse(netUnitPrice, BigDecimal.ZERO))
                            .status("AVAILABLE")
                            .productionDate(LocalDate.now())
                            .notes(notes)
                            .build();
                });

        BigDecimal oldQty;
        if (item.getQuantity() != null) {
            oldQty = item.getQuantity();
        } else {
            oldQty = BigDecimal.ZERO;
        }
        item.setQuantity(oldQty.add(quantity));
        if (netUnitPrice != null && netUnitPrice.compareTo(BigDecimal.ZERO) > 0) {
            item.setUnitPrice(netUnitPrice);
        }
        return stockItemRepository.save(item);
    }

    static StockWithdrawal withdrawConsumable(
            StockItemRepository stockItemRepository,
            MachineRepository machineRepository,
            Long stockItemId,
            BigDecimal quantity,
            Long machineId,
            String insufficientStockMessage) {
        Objects.requireNonNull(stockItemId, "Sarf malzeme seçilmelidir.");
        Objects.requireNonNull(quantity, "Kullanılan miktar girilmelidir.");
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Kullanılan miktar 0'dan büyük olmalıdır.");
        }

        StockItem item = stockItemRepository.findById(stockItemId)
                .orElseThrow(() -> new IllegalArgumentException("Sarf malzeme bulunamadı: " + stockItemId));

        BigDecimal currentStock;
        if (item.getQuantity() != null) {
            currentStock = item.getQuantity();
        } else {
            currentStock = BigDecimal.ZERO;
        }
        if (currentStock.compareTo(quantity) < 0) {
            throw new IllegalArgumentException(String.format(
                    insufficientStockMessage,
                    currentStock, item.getUnit(), quantity, item.getUnit()));
        }

        item.setQuantity(currentStock.subtract(quantity));
        StockItem saved = stockItemRepository.save(item);
        BigDecimal unitPrice;
        if (item.getUnitPrice() != null) {
            unitPrice = item.getUnitPrice();
        } else {
            unitPrice = BigDecimal.ZERO;
        }
        BigDecimal netCost = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        Machine machine;
        if (machineId != null) {
            machine = machineRepository.findById(machineId).orElse(null);
        } else {
            machine = null;
        }
        return new StockWithdrawal(saved, netCost, machine);
    }
}
