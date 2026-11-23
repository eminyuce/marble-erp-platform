package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.dto.SalesOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.view.PartyView;
import com.ozerler.marble.dto.view.SalesOrderView;
import com.ozerler.marble.exception.SalesOrderNotFoundException;
import com.ozerler.marble.mapper.CustomerMapper;
import com.ozerler.marble.mapper.SalesOrderMapper;
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
import com.ozerler.marble.util.GridPages;
import com.ozerler.marble.util.MessageUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SalesService {

    private static final int ORDER_NO_RANGE = 99999;

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final SlabRepository slabRepository;
    private final BlockRepository blockRepository;
    private final MessageSource messageSource;
    private final SalesOrderMapper salesOrderMapper;
    private final CustomerMapper customerMapper;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
                // best effort: fall back to the default below
            }
        }
        return MessageUtils.getMessage(code, args);
    }

    public record SalesSummaryDto(
            long totalOrders,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal remainingBalance) {
    }

    @Transactional(readOnly = true)
    public SalesSummaryDto getSalesSummary() {
        List<SalesOrder> all = BoundedReads.all(salesOrderRepository);
        long total = all.size();
        BigDecimal totalAmt =
                all.stream()
                        .map(order -> order.getTotalAmount())
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        BigDecimal paidAmt =
                all.stream()
                        .map(order -> order.getPaidAmount())
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        BigDecimal remaining = totalAmt.subtract(paidAmt);
        return new SalesSummaryDto(total, totalAmt, paidAmt, remaining);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<SalesOrderDto> getSalesOrdersPaged(int page, int size,
                                                                String search, String sortField, String sortDir) {
        return getSalesOrdersPaged(page, size, search, null, sortField, sortDir);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<SalesOrderDto> getSalesOrdersPaged(
            int page, int size, String search, String status, String sortField, String sortDir) {
        Page<SalesOrder> orderPage = GridPages.execute(page, size, sortField, sortDir, GridPages.SALES_ORDER_SORTS,
                pageable -> salesOrderRepository.searchSalesOrders(GridPages.normalizeSearch(search), pageable));
        List<SalesOrder> orders = orderPage.getContent();
        if (status != null && !status.isBlank()) {
            orders =
                    orders.stream()
                            .filter(
                                    o ->
                                            o.getStatus() != null
                                                    && o.getStatus()
                                                            .name()
                                                            .equalsIgnoreCase(status))
                            .toList();
        }
        List<SalesOrderDto> dtos = orders.stream()
                .map(SalesOrderDto::fromEntity)
                .collect(Collectors.toList());

        return TabulatorResponse.of(dtos, orderPage.getTotalPages(), orderPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<PartyView> getAllCustomers() {
        return BoundedReads.all(customerRepository, Sort.by("companyName")).stream()
                .map(customerMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalesOrderView getOrder(Long id) {
        try {
            return salesOrderMapper.toDto(getOrderById(id));
        } catch (IllegalArgumentException ex) {
            throw new SalesOrderNotFoundException(id);
        }
    }

    @Transactional
    public String createOrderNo(String orderNo, Long customerId, LocalDate deliveryDate, String notes) {
        return createSalesOrder(orderNo, customerId, deliveryDate, notes).getOrderNo();
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

        String orderNoValue;
        if (orderNo != null) {
            orderNoValue = orderNo.trim();
        } else {
            orderNoValue = "SAT-" + System.currentTimeMillis();
        }
        SalesOrder order = SalesOrder.builder()
                .orderNo(orderNoValue)
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
                .quantity(Objects.requireNonNullElse(quantity, BigDecimal.ONE))
                .unit(Objects.requireNonNullElse(unit, "m2"))
                .unitPrice(Objects.requireNonNullElse(unitPrice, BigDecimal.ZERO))
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
        return "SAT-" + LocalDate.now().getYear() + "-" + String.format("%05d", (int) (Math.random() * ORDER_NO_RANGE));
    }
}
