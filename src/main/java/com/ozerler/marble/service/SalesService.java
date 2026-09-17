package com.ozerler.marble.service;

import com.ozerler.marble.dto.SalesOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.SalesOrderStatus;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.SalesOrderRepository;
import com.ozerler.marble.repository.SlabRepository;
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
public class SalesService {

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final SlabRepository slabRepository;
    private final BlockRepository blockRepository;
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

    public record SalesSummaryDto(long totalOrders, BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal remainingBalance) {}

    @Transactional(readOnly = true)
    public SalesSummaryDto getSalesSummary() {
        List<SalesOrder> all = salesOrderRepository.findAll();
        long total = all.size();
        BigDecimal totalAmt = all.stream().map(SalesOrder::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidAmt = all.stream().map(SalesOrder::getPaidAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = totalAmt.subtract(paidAmt);
        return new SalesSummaryDto(total, totalAmt, paidAmt, remaining);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<SalesOrderDto> getSalesOrdersPaged(int page, int size,
                                                                String search, String sortField, String sortDir) {
        return getSalesOrdersPaged(page, size, search, null, sortField, sortDir);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<SalesOrderDto> getSalesOrdersPaged(int page, int size,
                                                                String search, String status, String sortField, String sortDir) {
        Page<SalesOrder> orderPage = GridPages.execute(page, size, sortField, sortDir, GridPages.SALES_ORDER_SORTS,
                pageable -> salesOrderRepository.searchSalesOrders(GridPages.normalizeSearch(search), pageable));
        List<SalesOrder> orders = orderPage.getContent();
        if (status != null && !status.isBlank()) {
            orders = orders.stream().filter(o -> o.getStatus() != null && o.getStatus().name().equalsIgnoreCase(status)).toList();
        }
        List<SalesOrderDto> dtos = orders.stream()
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
        Objects.requireNonNull(id, getMessage("error.sales_order.id.required"));
        return salesOrderRepository.findWithDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.sales_order.not_found", id)));
    }

    @Transactional
    public SalesOrder createSalesOrder(String orderNo, Long customerId, LocalDate deliveryDate, String notes) {
        Objects.requireNonNull(customerId, getMessage("error.customer.required"));

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.customer.not_found", customerId)));

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

    public String generateOrderNo() {
        return "SAT-" + LocalDate.now().getYear() + "-" + String.format("%05d", (int) (Math.random() * 99999));
    }
}
