package com.ozerler.marble.service;

import com.ozerler.marble.dto.SalesOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.SalesOrder;
import com.ozerler.marble.model.SalesOrderItem;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.SalesOrderStatus;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.SalesOrderRepository;
import com.ozerler.marble.repository.SlabRepository;
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
public class SalesService {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final SlabRepository slabRepository;
    private final BlockRepository blockRepository;

    @Transactional(readOnly = true)
    public TabulatorResponse<SalesOrderDto> getSalesOrdersPaged(int page, int size,
                                                                 String search, String sortField, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sortField != null && !sortField.isBlank()) {
            Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(dir, sortField);
        }

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size > 0 ? size : DEFAULT_PAGE_SIZE, sort);

        Page<SalesOrder> orderPage = salesOrderRepository.searchSalesOrders(search, pageable);
        List<SalesOrderDto> dtos = orderPage.getContent().stream()
                .map(SalesOrderDto::fromEntity)
                .collect(Collectors.toList());

        return TabulatorResponse.of(dtos, orderPage.getTotalPages(), orderPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAllByOrderByCompanyNameAsc();
    }

    @Transactional(readOnly = true)
    public SalesOrder getOrderById(Long id) {
        Objects.requireNonNull(id, "Sipariş ID boş olamaz");
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + id));
    }

    @Transactional
    public SalesOrder createSalesOrder(String orderNo, Long customerId, LocalDate deliveryDate, String notes) {
        Objects.requireNonNull(customerId, "Müşteri seçilmelidir");

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Müşteri bulunamadı: " + customerId));

        SalesOrder order = SalesOrder.builder()
                .orderNo(orderNo != null ? orderNo.trim() : "SAT-" + System.currentTimeMillis())
                .customer(customer)
                .orderDate(LocalDate.now())
                .deliveryDate(deliveryDate)
                .status(SalesOrderStatus.DRAFT)
                .notes(notes)
                .build();

        return salesOrderRepository.save(order);
    }

    @Transactional
    public SalesOrderItem addItemToOrder(Long orderId, String description, BigDecimal quantity,
                                         String unit, BigDecimal unitPrice, Long slabId, Long blockId) {
        SalesOrder order = getOrderById(orderId);

        SalesOrderItem item = SalesOrderItem.builder()
                .salesOrder(order)
                .description(description)
                .quantity(quantity != null ? quantity : BigDecimal.ONE)
                .unit(unit != null ? unit : "m2")
                .unitPrice(unitPrice != null ? unitPrice : BigDecimal.ZERO)
                .build();

        if (slabId != null) {
            Slab slab = slabRepository.findById(slabId).orElse(null);
            item.setSlab(slab);
        }
        if (blockId != null) {
            Block block = blockRepository.findById(blockId).orElse(null);
            item.setBlock(block);
        }

        item.calculateLineTotal();
        order.getItems().add(item);
        order.recalculateTotal();
        salesOrderRepository.save(order);

        return item;
    }

    @Transactional
    public void confirmOrder(Long orderId) {
        SalesOrder order = getOrderById(orderId);
        order.setStatus(SalesOrderStatus.CONFIRMED);
        salesOrderRepository.save(order);
    }
}
