package com.ozerler.marble.service;

import com.ozerler.marble.dto.PurchaseOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.PurchaseOrder;
import com.ozerler.marble.model.PurchaseOrderItem;
import com.ozerler.marble.model.Supplier;
import com.ozerler.marble.model.enums.PurchaseItemType;
import com.ozerler.marble.model.enums.PurchaseOrderStatus;
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

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
            }
        }
        return com.ozerler.marble.util.MessageUtils.getMessage(code, args);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<PurchaseOrderDto> getPurchaseOrdersPaged(int page, int size,
                                                                      String search, String sortField, String sortDir) {
        Page<PurchaseOrder> orderPage = GridPages.execute(page, size, sortField, sortDir, GridPages.PURCHASE_ORDER_SORTS,
                pageable -> purchaseOrderRepository.searchPurchaseOrders(GridPages.normalizeSearch(search), pageable));
        List<PurchaseOrderDto> dtos = orderPage.getContent().stream()
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

    @Transactional
    public PurchaseOrder createPurchaseOrder(String poNumber, Long supplierId, Long projectId,
                                             LocalDate expectedDelivery, String notes) {
        Objects.requireNonNull(supplierId, getMessage("error.supplier.required"));

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.supplier.not_found", supplierId)));

        Project project = null;
        if (projectId != null) {
            project = projectRepository.findById(projectId).orElse(null);
        }

        PurchaseOrder order = PurchaseOrder.builder()
                .poNumber(poNumber != null ? poNumber.trim() : "SIP-" + System.currentTimeMillis())
                .supplier(supplier)
                .project(project)
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
        order.setStatus(newStatus);
        purchaseOrderRepository.save(order);
    }

    public String generatePoNumber() {
        return "SIP-" + LocalDate.now().getYear() + "-" + String.format("%05d", (int) (Math.random() * 99999));
    }
}
