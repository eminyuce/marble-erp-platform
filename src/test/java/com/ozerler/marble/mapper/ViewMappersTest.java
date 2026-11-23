package com.ozerler.marble.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.ozerler.marble.dto.EmailTemplateDto;
import com.ozerler.marble.dto.view.BlockMarkView;
import com.ozerler.marble.dto.view.BlockMovementView;
import com.ozerler.marble.dto.view.CheckRef;
import com.ozerler.marble.dto.view.CollectionRef;
import com.ozerler.marble.dto.view.FactoryOperationView;
import com.ozerler.marble.dto.view.InvoiceView;
import com.ozerler.marble.dto.view.MachineFuelEntryView;
import com.ozerler.marble.dto.view.RoleView;
import com.ozerler.marble.dto.view.SiteViews;
import com.ozerler.marble.dto.view.StockMovementView;
import com.ozerler.marble.dto.view.WorkOrderView;
import com.ozerler.marble.dto.view.WorkshopOperationView;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.BlockCustomerMark;
import com.ozerler.marble.model.BlockLocationMovement;
import com.ozerler.marble.model.CheckRecord;
import com.ozerler.marble.model.CollectionRecord;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.EmailTemplate;
import com.ozerler.marble.model.FactoryOperation;
import com.ozerler.marble.model.FactoryWorkOrder;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.InvoiceItem;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.MachineFuelEntry;
import com.ozerler.marble.model.OperationWorkOrder;
import com.ozerler.marble.model.OperationWorkOrderItem;
import com.ozerler.marble.model.ProjectLocation;
import com.ozerler.marble.model.Role;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.StockMovement;
import com.ozerler.marble.model.WorkshopOperation;
import com.ozerler.marble.model.enums.BlockMarkStatus;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CheckStatus;
import com.ozerler.marble.model.enums.StockLocationType;

/**
 * Checks that the generated mappers only copy values: nested references go through the other mappers,
 * derived figures stay empty for the service to fill, and list-row mappings skip the child collections.
 */
@SpringJUnitConfig(ViewMappersTest.MapperConfig.class)
class ViewMappersTest {

    @Autowired
    private BlockMapper blockMapper;
    @Autowired
    private CollectionMapper collectionMapper;
    @Autowired
    private FactoryOperationMapper factoryOperationMapper;
    @Autowired
    private InvoiceMapper invoiceMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private StockMovementMapper stockMovementMapper;
    @Autowired
    private SiteMapper siteMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private MachineFuelMapper machineFuelMapper;
    @Autowired
    private WorkshopOperationMapper workshopOperationMapper;
    @Autowired
    private EmailTemplateMapper emailTemplateMapper;

    private static Customer customer() {
        Customer customer = Customer.builder().customerCode("C-1").companyName("Atlas").build();
        customer.setId(7L);
        return customer;
    }

    @Test
    @DisplayName("block marks and movements copy the customer and both locations")
    void blockMarkAndMovement() {
        BlockCustomerMark mark =
                BlockCustomerMark.builder()
                        .id(1L)
                        .customer(customer())
                        .markedAt(LocalDate.of(2026, 1, 1))
                        .offerPrice(new BigDecimal("12.50"))
                        .currency("USD")
                        .status(BlockMarkStatus.ACTIVE)
                        .build();
        BlockMarkView markView = blockMapper.toMark(mark);
        Assertions.assertThat(markView.getCustomer().getCompanyName()).isEqualTo("Atlas");
        Assertions.assertThat(markView.getOfferPrice()).isEqualByComparingTo("12.50");
        Assertions.assertThat(markView.getStatus()).isEqualTo(BlockMarkStatus.ACTIVE);

        StockLocation from =
                StockLocation.builder()
                        .name("Üretim")
                        .locationType(StockLocationType.PRODUCTION_YARD)
                        .build();
        StockLocation to = StockLocation.builder().name("Sevk").locationType(StockLocationType.DISPATCH_YARD).build();
        BlockMovementView movement = blockMapper.toMovement(BlockLocationMovement.builder()
                .id(2L).fromLocation(from).toLocation(to).description("taşındı").build());
        Assertions.assertThat(movement.getFromLocation().getName()).isEqualTo("Üretim");
        Assertions.assertThat(movement.getToLocation().getLocationType()).isEqualTo(StockLocationType.DISPATCH_YARD);
        Assertions.assertThat(movement.getDescription()).isEqualTo("taşındı");
    }

    @Test
    @DisplayName("block view leaves the derived figures to the service")
    void blockViewLeavesDerivedFieldsEmpty() {
        Block block = Block.builder().id(3L).blockCode("B-3").widthCm(100).lengthCm(200).heightCm(150).build();
        Assertions.assertThat(blockMapper.toDto(block).getApproximateTonnage()).isNull();
        Assertions.assertThat(blockMapper.toDto(block).getLength()).isEqualTo(200);
    }

    @Test
    @DisplayName("checks and collections copy the customer, status and invoice reference")
    void checksAndCollections() {
        CheckRef check = collectionMapper.toRef(CheckRecord.builder().id(5L).checkNo("CHK-5").bankName("Banka")
                .customer(customer()).amount(BigDecimal.TEN).status(CheckStatus.PORTFOLIO)
                .businessUnit(BusinessUnit.FACTORY).build());
        Assertions.assertThat(check.getCustomer().getId()).isEqualTo(7L);
        Assertions.assertThat(check.getEffectiveBusinessUnit()).isEqualTo(BusinessUnit.FACTORY);
        Assertions.assertThat(check.getStatus()).isEqualTo(CheckStatus.PORTFOLIO);

        Invoice invoice = Invoice.builder().invoiceNo("INV-9").build();
        invoice.setId(9L);
        CollectionRef collection = collectionMapper.toRef(CollectionRecord.builder().id(6L).collectionNo("THS-6")
                .customer(customer()).invoice(invoice).amount(BigDecimal.ONE).build());
        Assertions.assertThat(collection.getInvoice().getInvoiceNo()).isEqualTo("INV-9");
        Assertions.assertThat(collection.getCustomer().getCompanyName()).isEqualTo("Atlas");
    }

    @Test
    @DisplayName("invoice list rows skip lines and collections")
    void invoiceRowSkipsChildren() {
        Invoice invoice = Invoice.builder().invoiceNo("INV-1").customer(customer()).build();
        invoice.getItems().add(InvoiceItem.builder().productName("Kalem").build());
        InvoiceView row = invoiceMapper.toRow(invoice);
        Assertions.assertThat(row.getInvoiceNumber()).isEqualTo("INV-1");
        Assertions.assertThat(row.getItems()).isNull();
        Assertions.assertThat(invoiceMapper.toDto(invoice).getItems()).hasSize(1);
    }

    @Test
    @DisplayName("work order rows copy the display name and skip items and history")
    void workOrderRowSkipsChildren() {
        OperationWorkOrder order = OperationWorkOrder.builder().orderNo("WO-1").customer(customer())
                .quantity(new BigDecimal("3")).build();
        order.getItems().add(OperationWorkOrderItem.builder().notes("kalem").build());
        WorkOrderView row = workOrderMapper.toRow(order);
        Assertions.assertThat(row.getOrderNumber()).isEqualTo("WO-1");
        Assertions.assertThat(row.getCustomerDisplayName()).isEqualTo("Atlas");
        Assertions.assertThat(row.getQuantity()).isEqualByComparingTo("3");
        Assertions.assertThat(row.getItems()).isNull();
        Assertions.assertThat(row.getCustomerNameForView()).isNull();
    }

    @Test
    @DisplayName("factory operation copies machine and the work order block")
    void factoryOperation() {
        Machine machine = Machine.builder().id(4L).name("Silim").build();
        FactoryWorkOrder workOrder = FactoryWorkOrder.builder().orderNo("FWO-1")
                .block(Block.builder().id(8L).blockCode("B-8").build()).build();
        FactoryOperationView view = factoryOperationMapper.toDto(FactoryOperation.builder().id(1L).machine(machine)
                .workOrder(workOrder).operatorName("Ali").build());
        Assertions.assertThat(view.getMachine().getName()).isEqualTo("Silim");
        Assertions.assertThat(view.getWorkOrder().getBlock().getBlockCode()).isEqualTo("B-8");
        Assertions.assertThat(view.getWorkOrder().getBlock().getApproximateTonnage()).isNull();
    }

    @Test
    @DisplayName("stock movement leaves the invoice for the service to decide")
    void stockMovementIgnoresInvoice() {
        Invoice invoice = Invoice.builder().invoiceNo("INV-2").build();
        StockMovementView view = stockMovementMapper.toDto(StockMovement.builder().movementCode("M-1")
                .block(Block.builder().id(1L).blockCode("B-1").build()).customer(customer()).invoice(invoice)
                .notes("not").build());
        Assertions.assertThat(view.getBlock().getBlockCode()).isEqualTo("B-1");
        Assertions.assertThat(view.getCustomer().getCompanyName()).isEqualTo("Atlas");
        Assertions.assertThat(view.getInvoice()).isNull();
    }

    @Test
    @DisplayName("site location leaves status label and progress to the service")
    void siteLocationLeavesCalculations() {
        ProjectLocation location = ProjectLocation.builder().locationName("Lobi")
                .plannedAreaM2(BigDecimal.TEN).installedAreaM2(BigDecimal.ONE).build();
        SiteViews.LocationView view = siteMapper.toDto(location);
        Assertions.assertThat(view.getLocationName()).isEqualTo("Lobi");
        Assertions.assertThat(view.getProgressPercentage()).isNull();
        Assertions.assertThat(view.getStatusLabel()).isNull();
    }

    @Test
    @DisplayName("roles, fuel entries, workshop operations and email templates copy their fields")
    void simpleCopies() {
        RoleView role = roleMapper.toDto(Role.builder().id(1L).name("ROLE_ADMIN").description("Yönetici").build());
        Assertions.assertThat(role.getDescription()).isEqualTo("Yönetici");

        Machine machine = Machine.builder().id(2L).name("Kepçe").build();
        MachineFuelEntryView fuel = machineFuelMapper.toDto(MachineFuelEntry.builder().machine(machine)
                .litres(BigDecimal.TEN).totalAmount(new BigDecimal("420")).build());
        Assertions.assertThat(fuel.getMachine().getId()).isEqualTo(2L);
        Assertions.assertThat(fuel.getTotalAmount()).isEqualByComparingTo("420");

        WorkshopOperationView operation = workshopOperationMapper.toDto(WorkshopOperation.builder()
                .machine(machine).operatorName("Veli").startedAt(LocalDateTime.of(2026, 1, 1, 8, 0)).build());
        Assertions.assertThat(operation.getMachine().getName()).isEqualTo("Kepçe");
        Assertions.assertThat(operation.getOperatorName()).isEqualTo("Veli");

        EmailTemplate template = EmailTemplate.builder().templateKey("k").templateName("Şablon").build();
        template.setUpdatedDate(LocalDateTime.of(2026, 2, 3, 4, 5));
        EmailTemplateDto dto = emailTemplateMapper.toDto(template);
        Assertions.assertThat(dto.getTemplateName()).isEqualTo("Şablon");
        Assertions.assertThat(dto.getUpdatedAt()).isEqualTo(template.getUpdatedAt());
    }

    /**
     * Loads only the generated mapper beans, wired to each other as in the application.
     */
    @Configuration
    @ComponentScan(basePackageClasses = BlockMapper.class)
    static class MapperConfig {
    }
}
