package com.ozerler.marble.service;

import java.util.Objects;
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

    private static final int RECENT_DAYS = 30;

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
                .quantity(Objects.requireNonNullElse(quantity, BigDecimal.ONE))
                .quantityUnit(Objects.requireNonNullElse(quantityUnit, "adet"))
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
        BigDecimal ton;
        if (block.getActualTonnage() != null) {
            ton = block.getActualTonnage();
        } else {
            ton = block.getApproximateTonnage();
        }
        String value;
        if (block.getActualTonnage() != null) {
            value = block.getActualTonnage() + " ton";
        } else {
            value = "Girilmedi";
        }
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
                "Blok Üretimi: " + block.getBlockCode() + " (" + Objects.requireNonNullElse(block.getStoneType(), "") + ")",
                BigDecimal.ONE,
                "adet",
                ton,
                block.getAssignedCustomer(),
                null,
                null,
                "Ocakta blok üretildi - Tahmini: " + block.getApproximateTonnage() + " ton, Gerçek: " + value
        );
    }

    @Transactional
    public StockMovement recordBlockDispatch(Block block, StockLocation fromLoc, StockLocation toLoc,
                                            TargetDestination targetDest, Customer customer, Invoice invoice, String notes) {
        BigDecimal ton;
        if (block.getActualTonnage() != null) {
            ton = block.getActualTonnage();
        } else {
            ton = block.getApproximateTonnage();
        }
        BusinessUnit targetDept;
        if (targetDest == TargetDestination.FACTORY) {
            targetDept = BusinessUnit.FACTORY;
        } else {
            if (targetDest == TargetDestination.WORKSHOP) {
                targetDept = BusinessUnit.WORKSHOP;
            } else {
                targetDept = BusinessUnit.QUARRY;
            }
        }

        StockMovementType recordMovementValue;
        if (targetDest == TargetDestination.CUSTOMER) {
            recordMovementValue = StockMovementType.OUTGOING;
        } else {
            recordMovementValue = StockMovementType.TRANSFER;
        }
        String label;
        if (targetDest != null) {
            label = targetDest.getLabel();
        } else {
            label = "Fabrika";
        }
        return recordMovement(
                recordMovementValue,
                BusinessUnit.QUARRY,
                targetDept,
                targetDest,
                fromLoc,
                toLoc,
                block,
                null,
                null,
                "Blok Sevkiyatı: " + block.getBlockCode() + " -> " + label,
                BigDecimal.ONE,
                "adet",
                ton,
                customer,
                null,
                invoice,
                Objects.requireNonNullElse(notes, "Ocaktan sevk edildi")
        );
    }

    @Transactional
    public StockMovement recordBlockMovement(Block block, StockLocation fromLoc, StockLocation toLoc, String notes) {
        BigDecimal ton;
        if (block.getActualTonnage() != null) {
            ton = block.getActualTonnage();
        } else {
            ton = block.getApproximateTonnage();
        }
        BusinessUnit recordMovementValue;
        if (fromLoc != null) {
            recordMovementValue = fromLoc.getBusinessUnit();
        } else {
            recordMovementValue = BusinessUnit.QUARRY;
        }
        BusinessUnit recordMovementValue2;
        if (toLoc != null) {
            recordMovementValue2 = toLoc.getBusinessUnit();
        } else {
            recordMovementValue2 = BusinessUnit.FACTORY;
        }
        return recordMovement(
                StockMovementType.TRANSFER,
                recordMovementValue,
                recordMovementValue2,
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
                Objects.requireNonNullElse(notes, "Fabrika stoğuna kabul edildi")
        );
    }

    @Transactional
    public StockMovement recordBlockCuttingStart(Block block, String notes) {
        BigDecimal ton;
        if (block.getActualTonnage() != null) {
            ton = block.getActualTonnage();
        } else {
            ton = block.getApproximateTonnage();
        }
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
                Objects.requireNonNullElse(notes, "Fabrika kesim sürecine alındı, kesilmemiş blok stokundan düşüldü")
        );
    }

    @Transactional
    public StockMovement recordSlabProduction(Slab slab, Block block, Customer customer) {
        String value;
        if (customer != null) {
            value = " (" + customer.getCompanyName() + ")";
        } else {
            value = " (Genel Stok)";
        }
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
                "Plaka Stok Sahasına eklendi" + value
        );
    }

    @Transactional
    public StockMovement recordSizedItemProduction(StockItem item, Block block, Customer customer) {
        String value;
        if (customer != null) {
            value = " (" + customer.getCompanyName() + ")";
        } else {
            value = " (Genel Stok)";
        }
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
                "Fabrika Ebatlı Stok Sahasına eklendi" + value
        );
    }

    @Transactional
    public StockMovement recordWorkshopSizedProduction(StockItem item, Slab sourceSlab, StockItem sourceItem, Customer customer) {
        String value;
        if (customer != null) {
            value = " (" + customer.getCompanyName() + ")";
        } else {
            value = " (Genel Stok)";
        }
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
                "Atölye Ebatlı Stok Sahasına eklendi" + value
        );
    }

    @Transactional
    public StockMovement recordWorkshopWaste(StockItem item, Project project, BigDecimal wasteQty, String unit, String reason) {
        BusinessUnit recordMovementValue;
        if (project != null) {
            recordMovementValue = BusinessUnit.SITE;
        } else {
            recordMovementValue = BusinessUnit.WORKSHOP;
        }
        TargetDestination recordMovementValue2;
        if (project != null) {
            recordMovementValue2 = TargetDestination.SITE;
        } else {
            recordMovementValue2 = TargetDestination.WORKSHOP;
        }
        StockLocation recordMovementValue3;
        if (item != null) {
            recordMovementValue3 = item.getStockLocation();
        } else {
            recordMovementValue3 = null;
        }
        Block recordMovementValue4;
        if (item != null) {
            recordMovementValue4 = item.getSourceBlock();
        } else {
            recordMovementValue4 = null;
        }
        String itemCode;
        if (item != null) {
            itemCode = item.getItemCode();
        } else {
            itemCode = "Hammadde";
        }
        return recordMovement(
                StockMovementType.SCRAP,
                BusinessUnit.WORKSHOP,
                recordMovementValue,
                recordMovementValue2,
                recordMovementValue3,
                null,
                recordMovementValue4,
                null,
                item,
                "Atölye Üretim/Ebatlama Fire Çıkışı: " + itemCode,
                wasteQty,
                Objects.requireNonNullElse(unit, "m2"),
                null,
                null,
                null,
                null,
                Objects.requireNonNullElse(reason, "Atölyede kesim firesi zayi olarak düşüldü ve maliyetleştirildi")
        );
    }

    @Transactional
    public StockMovement recordWorkOrderConsumption(OperationWorkOrder order, StockItem item, Slab slab, Block block, BigDecimal qty, String unit) {
        BusinessUnit recordMovementValue;
        if (order.getDepartment() == BusinessUnit.WORKSHOP) {
            recordMovementValue = BusinessUnit.FACTORY;
        } else {
            recordMovementValue = order.getDepartment();
        }
        return recordMovement(
                StockMovementType.PRODUCTION_CONSUMPTION,
                recordMovementValue,
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
        return movementRepository.findRecentBySourceDepartment(BusinessUnit.QUARRY, LocalDateTime.now().minusDays(RECENT_DAYS));
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
            Object value;
            if (m.getMovementDate() != null) {
                value = m.getMovementDate().format(dateFmt);
            } else {
                value = "—";
            }
            map.put("movementDate", value);
            Object blockCode;
            if (m.getBlock() != null) {
                blockCode = m.getBlock().getBlockCode();
            } else {
                if (m.getItemDescription() != null) {
                    blockCode = m.getItemDescription();
                } else {
                    blockCode = "Mermer Blok";
                }
            }
            map.put("productName", blockCode);
            Long id;
            if (m.getBlock() != null) {
                id = m.getBlock().getId();
            } else {
                id = null;
            }
            map.put("blockId", id);
            map.put("tonnage", m.getTonnage());
            map.put("quantity", m.getQuantity());
            map.put("unit", Objects.requireNonNullElse(m.getQuantityUnit(), "ton"));
            map.put("source", "Ocak");
            Object displayName;
            if (m.getTargetDestination() != null) {
                displayName = m.getTargetDestination().getDisplayName();
            } else {
                if (m.getTargetDepartment() != null) {
                    displayName = m.getTargetDepartment().getDisplayName();
                } else {
                    displayName = "Fabrika";
                }
            }
            map.put("targetDestination", displayName);
            Object companyName;
            if (m.getCustomer() != null) {
                companyName = m.getCustomer().getCompanyName();
            } else {
                companyName = "—";
            }
            map.put("customerName", companyName);
            Object invoiceNumber;
            if (m.getInvoice() != null) {
                invoiceNumber = m.getInvoice().getInvoiceNumber();
            } else {
                invoiceNumber = null;
            }
            map.put("invoiceNo", invoiceNumber);
            Long idValue;
            if (m.getInvoice() != null) {
                idValue = m.getInvoice().getId();
            } else {
                idValue = null;
            }
            map.put("invoiceId", idValue);
            map.put("notes", Objects.requireNonNullElse(m.getNotes(), "—"));
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
