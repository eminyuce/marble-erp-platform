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
import com.ozerler.marble.repository.BlockRepository;
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
    private final com.ozerler.marble.mapper.BlockMapper blockMapper;
    private final com.ozerler.marble.mapper.CustomerMapper customerMapper;
    private final BlockRepository blockRepository;
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
        block.setAssignedCustomer(customer);
        if (offerPrice != null && offerPrice.compareTo(BigDecimal.ZERO) > 0) {
            block.setSalePrice(offerPrice);
        }
        blockRepository.save(block);
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
        block.setSoldCustomer(mark.getCustomer());
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
        return BoundedReads.all(customerRepository, org.springframework.data.domain.Sort.by("companyName"));
    }

    /**
     * Marks a block for a customer and returns the new mark id.
     *
     * @param blockId    block id
     * @param customerId customer id
     * @param markedAt   mark date
     * @param validUntil optional expiry
     * @param offerPrice offer price
     * @param currency   currency code
     * @return id of the new mark
     */
    @Transactional
    public Long markBlockId(Long blockId, Long customerId, LocalDate markedAt, LocalDate validUntil,
                            BigDecimal offerPrice, String currency) {
        return markBlock(blockId, customerId, markedAt, validUntil, offerPrice, currency).getId();
    }

    /**
     * Same as {@code customers}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.view.PartyView> customerViews() {
        return customers().stream().map(customerMapper::toDto).toList();
    }

    /**
     * Same as {@code listForBlock}, copied into a view inside the transaction.
     *
     * @param blockId see {@code listForBlock}
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.view.BlockMarkView> listViewsForBlock(Long blockId) {
        return listForBlock(blockId).stream().map(blockMapper::toMark).toList();
    }
}
