package com.ozerler.marble.service;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.BlockCustomerMark;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.SalesOrder;
import com.ozerler.marble.model.SalesOrderItem;
import com.ozerler.marble.model.enums.BlockMarkStatus;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.SalesOrderStatus;
import com.ozerler.marble.repository.BlockCustomerMarkRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.SalesOrderRepository;
import com.ozerler.marble.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BlockCustomerMarkService {

    private final BlockCustomerMarkRepository markRepository;
    private final CustomerRepository customerRepository;
    private final QuarryBlockService quarryBlockService;
    private final SalesOrderRepository salesOrderRepository;

    @Transactional
    public BlockCustomerMark markBlock(Long blockId, Long customerId, LocalDate markedAt,
                                       LocalDate validUntil, BigDecimal offerPrice, String currency) {
        Block block = quarryBlockService.getBlockById(blockId);
        if (!block.getCanonicalStatus().isAtQuarry()) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.block.mark.not_at_quarry"));
        }
        markRepository.findFirstByBlockIdAndStatus(blockId, BlockMarkStatus.ACTIVE).ifPresent(existing -> {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.block.mark.already_active"));
        });
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.customer.not_found", customerId)));
        BlockCustomerMark mark = BlockCustomerMark.builder()
                .block(block)
                .customer(customer)
                .markedAt(markedAt != null ? markedAt : LocalDate.now())
                .validUntil(validUntil)
                .offerPrice(offerPrice != null ? offerPrice : BigDecimal.ZERO)
                .currency(currency != null ? currency : "TRY")
                .status(BlockMarkStatus.ACTIVE)
                .stockLocationSnapshot(block.getCurrentLocation())
                .build();
        BlockCustomerMark saved = markRepository.save(mark);
        block.setStatus(BlockStatus.MARKED);
        return saved;
    }

    @Transactional
    public SalesOrder convertMarkToSale(Long markId) {
        BlockCustomerMark mark = markRepository.findById(markId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.block.mark.not_found", markId)));
        if (mark.getStatus() != BlockMarkStatus.ACTIVE) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.block.mark.not_active"));
        }
        Block block = mark.getBlock();
        SalesOrder order = SalesOrder.builder()
                .orderNo(String.format("SO-%d-%d", Year.now().getValue(), System.currentTimeMillis() % 100000))
                .customer(mark.getCustomer())
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.CONFIRMED)
                .notes("Ocak blok satışı: " + block.getBlockCode())
                .build();
        SalesOrderItem item = SalesOrderItem.builder()
                .salesOrder(order)
                .block(block)
                .description(block.getBlockCode())
                .quantity(BigDecimal.ONE)
                .unit("BLOK")
                .unitPrice(mark.getOfferPrice())
                .build();
        item.calculateLineTotal();
        order.getItems().add(item);
        order.setTotalAmount(item.getLineTotal());
        SalesOrder saved = salesOrderRepository.save(order);
        mark.setStatus(BlockMarkStatus.CONVERTED_TO_SALE);
        mark.setSalesOrderItem(saved.getItems().getFirst());
        block.setStatus(BlockStatus.SOLD);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<BlockCustomerMark> listActive() {
        return markRepository.findByStatusOrderByMarkedAtDesc(BlockMarkStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<BlockCustomerMark> listForBlock(Long blockId) {
        Objects.requireNonNull(blockId);
        return markRepository.findByBlockIdOrderByMarkedAtDesc(blockId);
    }

    @Transactional(readOnly = true)
    public List<Customer> customers() {
        return customerRepository.findAllByOrderByCompanyNameAsc();
    }
}
