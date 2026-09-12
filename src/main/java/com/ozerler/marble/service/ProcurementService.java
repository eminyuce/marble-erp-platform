package com.ozerler.marble.service;

import com.ozerler.marble.dto.PurchaseOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.PurchaseOrder;
import com.ozerler.marble.model.PurchaseOrderItem;
import com.ozerler.marble.model.Supplier;
import com.ozerler.marble.model.enums.PurchaseOrderStatus;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.PurchaseOrderRepository;
import com.ozerler.marble.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public TabulatorResponse<PurchaseOrderDto> getPurchaseOrdersPaged(int page, int size,
                                                                       String search, String sortField, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sortField != null && !sortField.isBlank()) {
            Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(dir, sortField);
        }

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size > 0 ? size : DEFAULT_PAGE_SIZE, sort);

        Page<PurchaseOrder> orderPage = purchaseOrderRepository.searchPurchaseOrders(search, pageable);
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
        Objects.requireNonNull(id, "Sipariş ID boş olamaz");
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Satın alma siparişi bulunamadı: " + id));
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(String poNumber, Long supplierId, Long projectId,
                                              LocalDate expectedDelivery, String notes) {
        Objects.requireNonNull(supplierId, "Tedarikçi seçilmelidir");

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Tedarikçi bulunamadı: " + supplierId));

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
                .itemType(itemType != null ? itemType : "CONSUMABLE")
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
