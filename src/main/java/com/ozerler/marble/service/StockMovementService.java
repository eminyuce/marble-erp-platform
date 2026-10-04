package com.ozerler.marble.service;

import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockMovementType;
import com.ozerler.marble.model.enums.TargetDestination;
import com.ozerler.marble.repository.StockMovementRepository;
import com.ozerler.marble.util.UniqueCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockMovementService {

    private final StockMovementRepository movementRepository;

    @Transactional
    public StockMovement recordMovement(StockMovementType type,
                                       BusinessUnit sourceDept,
                                       BusinessUnit targetDept,
                                       TargetDestination targetDest,
                                       StockLocation fromLoc,
                                       StockLocation toLoc,
                                       Block block,
                                       Slab slab,
                                       StockItem stockItem,
                                       String itemDescription,
                                       BigDecimal quantity,
                                       String quantityUnit,
                                       BigDecimal tonnage,
                                       Customer customer,
                                       OperationWorkOrder workOrder,
                                       Invoice invoice,
                                       String notes) {

        String code = UniqueCodes.yearly("SM", movementRepository::existsByMovementCode);

        StockMovement movement = StockMovement.builder()
                .movementCode(code)
                .movementDate(LocalDateTime.now())
                .movementType(type)
                .sourceDepartment(sourceDept)
                .targetDepartment(targetDept)
                .targetDestination(targetDest)
                .fromLocation(fromLoc)
                .toLocation(toLoc)
                .block(block)
                .slab(slab)
                .stockItem(stockItem)
                .itemDescription(itemDescription)
                .quantity(quantity != null ? quantity : BigDecimal.ONE)
                .quantityUnit(quantityUnit != null ? quantityUnit : "adet")
                .tonnage(tonnage)
                .customer(customer)
                .workOrder(workOrder)
                .invoice(invoice)
                .notes(notes)
                .build();

        return movementRepository.save(movement);
    }

    @Transactional
    public StockMovement recordBlockQuarryProduction(Block block, StockLocation yard) {
        BigDecimal ton = block.getActualTonnage() != null ? block.getActualTonnage() : block.getApproximateTonnage();
        return recordMovement(
                StockMovementType.PRODUCTION,
                BusinessUnit.QUARRY,
                BusinessUnit.QUARRY,
                TargetDestination.OTHER,
                null,
                yard,
                block,
                null,
                null,
                "Blok Üretimi: " + block.getBlockCode() + " (" + (block.getStoneType() != null ? block.getStoneType() : "") + ")",
                BigDecimal.ONE,
                "adet",
                ton,
                block.getAssignedCustomer(),
                null,
                null,
                "Ocakta blok üretildi - Tahmini: " + block.getApproximateTonnage() + " ton, Gerçek: " + (block.getActualTonnage() != null ? block.getActualTonnage() + " ton" : "Girilmedi")
        );
    }

    @Transactional
    public StockMovement recordBlockDispatch(Block block, StockLocation fromLoc, StockLocation toLoc,
                                            TargetDestination targetDest, Customer customer, Invoice invoice, String notes) {
        BigDecimal ton = block.getActualTonnage() != null ? block.getActualTonnage() : block.getApproximateTonnage();
        BusinessUnit targetDept = targetDest == TargetDestination.FACTORY ? BusinessUnit.FACTORY
                : (targetDest == TargetDestination.WORKSHOP ? BusinessUnit.WORKSHOP : BusinessUnit.QUARRY);

        return recordMovement(
                targetDest == TargetDestination.CUSTOMER ? StockMovementType.OUTGOING : StockMovementType.TRANSFER,
                BusinessUnit.QUARRY,
                targetDept,
                targetDest,
                fromLoc,
                toLoc,
                block,
                null,
                null,
                "Blok Sevkiyatı: " + block.getBlockCode() + " -> " + (targetDest != null ? targetDest.getLabel() : "Fabrika"),
                BigDecimal.ONE,
                "adet",
                ton,
                customer,
                null,
                invoice,
                notes != null ? notes : "Ocaktan sevk edildi"
        );
    }

    @Transactional
    public StockMovement recordBlockMovement(Block block, StockLocation fromLoc, StockLocation toLoc, String notes) {
        BigDecimal ton = block.getActualTonnage() != null ? block.getActualTonnage() : block.getApproximateTonnage();
        return recordMovement(
                StockMovementType.TRANSFER,
                fromLoc != null ? fromLoc.getBusinessUnit() : BusinessUnit.QUARRY,
                toLoc != null ? toLoc.getBusinessUnit() : BusinessUnit.FACTORY,
                TargetDestination.FACTORY,
                fromLoc,
                toLoc,
                block,
                null,
                null,
                "Blok Transferi: " + block.getBlockCode(),
                BigDecimal.ONE,
                "adet",
                ton,
                block.getAssignedCustomer(),
                null,
                null,
                notes != null ? notes : "Fabrika stoğuna kabul edildi"
        );
    }

    @Transactional
    public StockMovement recordBlockCuttingStart(Block block, String notes) {
        BigDecimal ton = block.getActualTonnage() != null ? block.getActualTonnage() : block.getApproximateTonnage();
        return recordMovement(
                StockMovementType.PRODUCTION_CONSUMPTION,
                BusinessUnit.FACTORY,
                BusinessUnit.FACTORY,
                TargetDestination.FACTORY,
                block.getCurrentLocation(),
                null,
                block,
                null,
                null,
                "Blok Kesime Alındı: " + block.getBlockCode(),
                BigDecimal.ONE,
                "adet",
                ton,
                block.getAssignedCustomer(),
                null,
                null,
                notes != null ? notes : "Fabrika kesim sürecine alındı, kesilmemiş blok stokundan düşüldü"
        );
    }

    @Transactional
    public StockMovement recordSlabProduction(Slab slab, Block block, Customer customer) {
        return recordMovement(
                StockMovementType.PRODUCTION,
                BusinessUnit.FACTORY,
                BusinessUnit.FACTORY,
                TargetDestination.FACTORY,
                null,
                null,
                block,
                slab,
                null,
                "Plaka Üretimi: " + slab.getSlabCode(),
                slab.getSurfaceAreaM2(),
                "m2",
                null,
                customer,
                null,
                null,
                "Plaka Stok Sahasına eklendi" + (customer != null ? " (" + customer.getCompanyName() + ")" : " (Genel Stok)")
        );
    }

    @Transactional
    public StockMovement recordSizedItemProduction(StockItem item, Block block, Customer customer) {
        return recordMovement(
                StockMovementType.PRODUCTION,
                BusinessUnit.FACTORY,
                BusinessUnit.FACTORY,
                TargetDestination.FACTORY,
                null,
                item.getStockLocation(),
                block,
                null,
                item,
                "Ebatlı Ürün Üretimi: " + item.getItemCode() + " - " + item.getDescription(),
                item.getQuantity(),
                item.getUnit(),
                null,
                customer,
                null,
                null,
                "Fabrika Ebatlı Stok Sahasına eklendi" + (customer != null ? " (" + customer.getCompanyName() + ")" : " (Genel Stok)")
        );
    }

    @Transactional
    public StockMovement recordWorkOrderConsumption(OperationWorkOrder order, StockItem item, Slab slab, Block block, BigDecimal qty, String unit) {
        return recordMovement(
                StockMovementType.PRODUCTION_CONSUMPTION,
                order.getDepartment() == BusinessUnit.WORKSHOP ? BusinessUnit.FACTORY : order.getDepartment(),
                order.getDepartment(),
                TargetDestination.WORKSHOP,
                null,
                null,
                block,
                slab,
                item,
                "İş Emri Stok Tüketimi: " + order.getOrderNo(),
                qty,
                unit,
                null,
                order.getCustomer(),
                order,
                null,
                "İş emri (" + order.getOrderNo() + ") için stoktan hammadde kullanıldı ve düşüldü"
        );
    }

    @Transactional(readOnly = true)
    public Page<StockMovement> searchMovements(StockMovementType type, BusinessUnit sourceDept, BusinessUnit targetDept,
                                              Long customerId, LocalDateTime fromDate, LocalDateTime toDate, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return movementRepository.searchMovements(type, sourceDept, targetDept, customerId, fromDate, toDate, pageable);
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getRecentQuarryDispatches() {
        return movementRepository.findRecentBySourceDepartment(BusinessUnit.QUARRY, LocalDateTime.now().minusDays(30));
    }
}
