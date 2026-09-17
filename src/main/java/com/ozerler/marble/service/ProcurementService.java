package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.PurchaseOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.PurchaseOrder;
import com.ozerler.marble.model.PurchaseOrderItem;
import com.ozerler.marble.model.Supplier;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.model.enums.PurchaseItemType;
import com.ozerler.marble.model.enums.PurchaseOrderStatus;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.PurchaseOrderRepository;
import com.ozerler.marble.repository.SupplierRepository;
import com.ozerler.marble.util.GridPages;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcurementService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProjectRepository projectRepository;
    private final org.springframework.context.MessageSource messageSource;
    private final ExpenseService expenseService;
    private final CostCenterRepository costCenterRepository;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
            }
        }
        return com.ozerler.marble.util.MessageUtils.getMessage(code, args);
    }

    public record ProcurementSummaryDto(long totalOrders, BigDecimal totalAmount, long pendingDeliveries, long completedDeliveries) {}

    @Transactional(readOnly = true)
    public ProcurementSummaryDto getProcurementSummary() {
        List<PurchaseOrder> all = purchaseOrderRepository.findAll();
        long total = all.size();
        BigDecimal totalAmt = all.stream().map(PurchaseOrder::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        long pending = all.stream().filter(p -> p.getStatus() != null && ("DRAFT".equalsIgnoreCase(p.getStatus().name()) || "SENT".equalsIgnoreCase(p.getStatus().name()) || "CONFIRMED".equalsIgnoreCase(p.getStatus().name()))).count();
        long completed = all.stream().filter(p -> p.getStatus() != null && "DELIVERED".equalsIgnoreCase(p.getStatus().name())).count();
        return new ProcurementSummaryDto(total, totalAmt, pending, completed);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<PurchaseOrderDto> getPurchaseOrdersPaged(int page, int size,
                                                                      String search, String sortField, String sortDir) {
        return getPurchaseOrdersPaged(page, size, search, null, sortField, sortDir);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<PurchaseOrderDto> getPurchaseOrdersPaged(int page, int size,
                                                                      String search, String status, String sortField, String sortDir) {
        Page<PurchaseOrder> orderPage = GridPages.execute(page, size, sortField, sortDir, GridPages.PURCHASE_ORDER_SORTS,
                pageable -> purchaseOrderRepository.searchPurchaseOrders(GridPages.normalizeSearch(search), pageable));
        List<PurchaseOrder> orders = orderPage.getContent();
        if (status != null && !status.isBlank()) {
            orders = orders.stream().filter(o -> o.getStatus() != null && o.getStatus().name().equalsIgnoreCase(status)).toList();
        }
        List<PurchaseOrderDto> dtos = orders.stream()
                .map(PurchaseOrderDto::fromEntity)
                .collect(Collectors.toList());

        return TabulatorResponse.of(dtos, orderPage.getTotalPages(), orderPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAllByOrderByCompanyNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PurchaseOrder getOrderById(Long id) {
        Objects.requireNonNull(id, getMessage("error.purchase_order.id.required"));
        return purchaseOrderRepository.findWithDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.purchase_order.not_found", id)));
    }

    @Transactional(readOnly = true)
    public List<CostCenter> getCostCentersForUnit(BusinessUnit businessUnit) {
        if (businessUnit == null) {
            return List.of();
        }
        return costCenterRepository.findByBusinessUnit(businessUnit);
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(String poNumber, Long supplierId, Long projectId,
                                             BusinessUnit businessUnit, Long costCenterId,
                                             LocalDate expectedDelivery, String notes) {
        Objects.requireNonNull(supplierId, getMessage("error.supplier.required"));
        if (businessUnit == null) {
            throw new IllegalArgumentException(getMessage("error.purchase.business_unit.required"));
        }
        if (businessUnit == BusinessUnit.SITE && projectId == null) {
            throw new IllegalArgumentException(getMessage("error.purchase.site.project.required"));
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.supplier.not_found", supplierId)));

        Project project = null;
        if (projectId != null) {
            project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new IllegalArgumentException(getMessage("error.project.not_found", projectId)));
        }

        CostCenter costCenter = resolveCostCenter(businessUnit, costCenterId);

        PurchaseOrder order = PurchaseOrder.builder()
                .poNumber(poNumber != null ? poNumber.trim() : "SIP-" + System.currentTimeMillis())
                .supplier(supplier)
                .project(project)
                .businessUnit(businessUnit)
                .costCenter(costCenter)
                .orderDate(LocalDate.now())
                .expectedDelivery(expectedDelivery)
                .status(PurchaseOrderStatus.DRAFT)
                .notes(notes)
                .build();

        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrderItem addItemToOrder(Long orderId, String itemName, String itemType,
                                            BigDecimal quantity, String unit, BigDecimal unitPrice) {
        PurchaseOrder order = getOrderById(orderId);

        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(order)
                .itemName(itemName)
                .itemType(PurchaseItemType.fromCode(itemType))
                .quantity(quantity != null ? quantity : BigDecimal.ONE)
                .unit(unit != null ? unit : "ADET")
                .unitPrice(unitPrice != null ? unitPrice : BigDecimal.ZERO)
                .build();

        item.calculateLineTotal();
        order.getItems().add(item);
        order.recalculateTotal();
        purchaseOrderRepository.save(order);

        return item;
    }

    @Transactional
    public void updateStatus(Long orderId, PurchaseOrderStatus newStatus) {
        PurchaseOrder order = getOrderById(orderId);
        if (newStatus == PurchaseOrderStatus.CONFIRMED && order.getStatus() == PurchaseOrderStatus.DRAFT) {
            postPurchaseExpense(order);
        }
        order.setStatus(newStatus);
        purchaseOrderRepository.save(order);
    }

    public String generatePoNumber() {
        return "SIP-" + LocalDate.now().getYear() + "-" + String.format("%05d", (int) (Math.random() * 99999));
    }

    private CostCenter resolveCostCenter(BusinessUnit unit, Long costCenterId) {
        if (costCenterId != null) {
            CostCenter selected = costCenterRepository.findById(costCenterId)
                    .orElseThrow(() -> new IllegalArgumentException(getMessage("error.cost_center.not_found", costCenterId)));
            if (selected.getBusinessUnit() != unit) {
                throw new IllegalArgumentException(getMessage("error.cost_center.unit.mismatch", unit.getLabel()));
            }
            return selected;
        }
        return costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(unit)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.cost_center.unit.missing", unit.getLabel())));
    }

    private void postPurchaseExpense(PurchaseOrder order) {
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BusinessUnit unit = order.getBusinessUnit();
        if (unit == null) {
            throw new IllegalArgumentException(getMessage("error.purchase.business_unit.required"));
        }
        if (unit == BusinessUnit.SITE && order.getProject() == null) {
            throw new IllegalArgumentException(getMessage("error.purchase.site.project.required"));
        }
        CostCenter center = order.getCostCenter() != null
                ? order.getCostCenter()
                : resolveCostCenter(unit, null);
        String period = YearMonth.now().toString();
        expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                center.getId(), ExpenseType.MATERIAL, null, unit, order.getTotalAmount(),
                Constants.CURRENCY_TRY, order.getPoNumber(), LocalDate.now(), LocalDate.now(),
                period, period, null, null, null, order.getProject(), null, null, null, order.getPoNumber(),
                "Satın alma: " + order.getPoNumber()));
    }
}
