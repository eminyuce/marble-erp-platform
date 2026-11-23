package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.OperationWorkOrder;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.StockMovement;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockMovementType;
import com.ozerler.marble.model.enums.TargetDestination;
import com.ozerler.marble.repository.StockMovementRepository;
import com.ozerler.marble.util.UniqueCodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockMovementService {

    private final StockMovementRepository movementRepository;
    private final com.ozerler.marble.mapper.StockMovementMapper stockMovementMapper;
    private final com.ozerler.marble.mapper.InvoiceMapper invoiceMapper;

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
                : targetDest == TargetDestination.WORKSHOP ? BusinessUnit.WORKSHOP : BusinessUnit.QUARRY;

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
    public StockMovement recordWorkshopSizedProduction(StockItem item, Slab sourceSlab, StockItem sourceItem, Customer customer) {
        return recordMovement(
                StockMovementType.PRODUCTION,
                BusinessUnit.WORKSHOP,
                BusinessUnit.WORKSHOP,
                TargetDestination.WORKSHOP,
                null,
                item.getStockLocation(),
                item.getSourceBlock(),
                sourceSlab,
                item,
                "Atölye Ebatlı Ürün Üretimi: " + item.getItemCode() + " - " + item.getDescription(),
                item.getQuantity(),
                item.getUnit(),
                null,
                customer,
                null,
                null,
                "Atölye Ebatlı Stok Sahasına eklendi" + (customer != null ? " (" + customer.getCompanyName() + ")" : " (Genel Stok)")
        );
    }

    @Transactional
    public StockMovement recordWorkshopWaste(StockItem item, Project project, BigDecimal wasteQty, String unit, String reason) {
        return recordMovement(
                StockMovementType.SCRAP,
                BusinessUnit.WORKSHOP,
                project != null ? BusinessUnit.SITE : BusinessUnit.WORKSHOP,
                project != null ? TargetDestination.SITE : TargetDestination.WORKSHOP,
                item != null ? item.getStockLocation() : null,
                null,
                item != null ? item.getSourceBlock() : null,
                null,
                item,
                "Atölye Üretim/Ebatlama Fire Çıkışı: " + (item != null ? item.getItemCode() : "Hammadde"),
                wasteQty,
                unit != null ? unit : "m2",
                null,
                null,
                null,
                null,
                reason != null ? reason : "Atölyede kesim firesi zayi olarak düşüldü ve maliyetleştirildi"
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
                                              Long customerId, LocalDateTime fromDate, LocalDateTime toDate,
                                              Pageable pageable) {
        return movementRepository.searchMovements(type, sourceDept, targetDept, customerId, fromDate, toDate, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockMovement> searchMovements(StockMovementType type, BusinessUnit sourceDept, BusinessUnit targetDept,
                                              Long customerId, LocalDateTime fromDate, LocalDateTime toDate, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return searchMovements(type, sourceDept, targetDept, customerId, fromDate, toDate, pageable);
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getRecentQuarryDispatches() {
        return movementRepository.findRecentBySourceDepartment(BusinessUnit.QUARRY, LocalDateTime.now().minusDays(30));
    }

    /**
     * Builds grid rows inside the read transaction (moved from the controller unchanged).
     *
     * @return grid rows
     */
    @Transactional(readOnly = true)
    public com.ozerler.marble.dto.TabulatorResponse<java.util.Map<String, Object>> quarryDispatchRows(int page, int size) {
        int pageIndex = Math.max(0, page - 1);
        Page<StockMovement> pageData = searchMovements(
                null, BusinessUnit.QUARRY, null, null, null, null,
                PageRequest.of(pageIndex, size));

        java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        List<java.util.Map<String, Object>> list = pageData.getContent().stream().map(m -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", m.getId());
            map.put("movementDate", m.getMovementDate() != null ? m.getMovementDate().format(dateFmt) : "—");
            map.put("productName", m.getBlock() != null ? m.getBlock().getBlockCode() : m.getItemDescription() != null ? m.getItemDescription() : "Mermer Blok");
            map.put("blockId", m.getBlock() != null ? m.getBlock().getId() : null);
            map.put("tonnage", m.getTonnage());
            map.put("quantity", m.getQuantity());
            map.put("unit", m.getQuantityUnit() != null ? m.getQuantityUnit() : "ton");
            map.put("source", "Ocak");
            map.put("targetDestination", m.getTargetDestination() != null ? m.getTargetDestination().getDisplayName() : m.getTargetDepartment() != null ? m.getTargetDepartment().getDisplayName() : "Fabrika");
            map.put("customerName", m.getCustomer() != null ? m.getCustomer().getCompanyName() : "—");
            map.put("invoiceNo", m.getInvoice() != null ? m.getInvoice().getInvoiceNumber() : null);
            map.put("invoiceId", m.getInvoice() != null ? m.getInvoice().getId() : null);
            map.put("notes", m.getNotes() != null ? m.getNotes() : "—");
            return map;
        }).toList();

        return com.ozerler.marble.dto.TabulatorResponse.of(list, pageData.getTotalPages(), pageData.getTotalElements());
    }

    /**
     * Quarry dispatch report page as views. The invoice is copied only when the query already loaded it.
     *
     * @param pageable page request
     * @return movement views
     */
    @Transactional(readOnly = true)
    public Page<com.ozerler.marble.dto.view.StockMovementView> quarryDispatchViews(
            Pageable pageable) {
        return searchMovements(null, BusinessUnit.QUARRY, null, null, null, null, pageable).map(movement -> {
            com.ozerler.marble.dto.view.StockMovementView view = stockMovementMapper.toDto(movement);
            if (jakarta.persistence.Persistence.getPersistenceUtil().isLoaded(movement, "invoice")
                    && movement.getInvoice() != null) {
                view.setInvoice(invoiceMapper.toRef(movement.getInvoice()));
            }
            return view;
        });
    }
}
