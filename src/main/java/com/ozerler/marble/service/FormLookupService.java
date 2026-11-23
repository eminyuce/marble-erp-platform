package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.dto.option.ProjectOption;
import com.ozerler.marble.dto.option.QuarryOption;
import com.ozerler.marble.dto.view.BlockView;
import com.ozerler.marble.dto.view.CostCenterRef;
import com.ozerler.marble.dto.view.InvoiceView;
import com.ozerler.marble.dto.view.PartyView;
import com.ozerler.marble.dto.view.ProjectView;
import com.ozerler.marble.dto.view.QuarryRef;
import com.ozerler.marble.dto.view.SalesOrderView;
import com.ozerler.marble.dto.view.SlabView;
import com.ozerler.marble.dto.view.StockItemView;
import com.ozerler.marble.exception.BlockNotFoundException;
import com.ozerler.marble.exception.CustomerNotFoundException;
import com.ozerler.marble.exception.InvoiceNotFoundException;
import com.ozerler.marble.exception.ProjectNotFoundException;
import com.ozerler.marble.exception.SlabNotFoundException;
import com.ozerler.marble.mapper.BlockMapper;
import com.ozerler.marble.mapper.CostCenterMapper;
import com.ozerler.marble.mapper.CustomerMapper;
import com.ozerler.marble.mapper.InvoiceMapper;
import com.ozerler.marble.mapper.ProjectMapper;
import com.ozerler.marble.mapper.QuarryMapper;
import com.ozerler.marble.mapper.SalesOrderMapper;
import com.ozerler.marble.mapper.SlabMapper;
import com.ozerler.marble.mapper.StockItemMapper;
import com.ozerler.marble.mapper.SupplierMapper;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.SalesOrderItem;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.StockProductType;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.InvoiceRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.QuarryRepository;
import com.ozerler.marble.repository.SalesOrderItemRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.StockItemRepository;
import com.ozerler.marble.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

/**
 * Form and select data for ERP screens. Controllers ask this service instead of repositories.
 * Lists and detail graphs are view DTOs. Derived labels are copied here after MapStruct
 * transfers the stored fields. Controllers never receive entities from this service.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FormLookupService {

    private final CustomerRepository customerRepository;
    private final BlockViews blockViews;
    private final SupplierRepository supplierRepository;
    private final ProjectRepository projectRepository;
    private final QuarryRepository quarryRepository;
    private final CostCenterRepository costCenterRepository;
    private final BlockRepository blockRepository;
    private final SlabRepository slabRepository;
    private final StockItemRepository stockItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final BlockMapper blockMapper;
    private final SlabMapper slabMapper;
    private final StockItemMapper stockItemMapper;
    private final InvoiceMapper invoiceMapper;
    private final CustomerMapper customerMapper;
    private final QuarryMapper quarryMapper;
    private final CostCenterMapper costCenterMapper;
    private final SupplierMapper supplierMapper;
    private final ProjectMapper projectMapper;
    private final SalesOrderMapper salesOrderMapper;

    public List<PartyView> customers() {
        return BoundedReads.all(customerRepository).stream().map(customerMapper::toDto).toList();
    }

    public List<PartyView> customersByCompanyName() {
        return BoundedReads.all(customerRepository, Sort.by("companyName")).stream().map(customerMapper::toDto).toList();
    }

    public PartyView getCustomer(Long id) {
        Customer customer = customerRepository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
        return customerMapper.toDto(customer);
    }

    public PartyView findCustomer(Long id) {
        if (id == null) {
            return null;
        }
        return customerRepository.findById(id).map(customerMapper::toDto).orElse(null);
    }

    public List<PartyView> suppliers() {
        return BoundedReads.all(supplierRepository).stream().map(supplierMapper::toDto).toList();
    }

    public List<ProjectOption> projects() {
        return BoundedReads.all(projectRepository).stream()
                .map(project -> new ProjectOption(project.getId(), project.getName(), project.getProjectCode()))
                .toList();
    }

    public ProjectView getProject(Long id) {
        return projectMapper.toDto(projectRepository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id)));
    }

    public List<ProjectView> projectsForCustomer(Long customerId) {
        return projectRepository.findByCustomerIdOrderByStartDateDesc(customerId).stream()
                .map(projectMapper::toDto).toList();
    }

    public List<QuarryRef> quarries() {
        return BoundedReads.all(quarryRepository).stream().map(quarryMapper::toDto).toList();
    }

    public QuarryOption getQuarry(Long id) {
        return quarryRepository.findById(id)
                .map(quarry -> new QuarryOption(quarry.getId(), quarry.getName(), quarry.getLocation(), quarry.getSpecificGravity()))
                .orElseThrow(() -> new IllegalArgumentException("Ocak bulunamadı: " + id));
    }

    public List<CostCenterRef> costCenters() {
        return BoundedReads.all(costCenterRepository).stream().map(costCenterMapper::toDto).toList();
    }

    public List<CostCenterRef> costCenters(BusinessUnit unit) {
        return costCenterRepository.findByBusinessUnit(unit).stream().map(costCenterMapper::toDto).toList();
    }

    public Optional<Long> firstCostCenterId(BusinessUnit unit) {
        return costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(unit).map(CostCenter::getId);
    }

    public Optional<Long> anyCostCenterId() {
        return BoundedReads.all(costCenterRepository).stream().findFirst().map(CostCenter::getId);
    }

    public List<BlockView> blocksWithQuarry() {
        return blockRepository.findAllWithQuarry().stream().map(this::blockView).toList();
    }

    public List<Block> blocksWithQuarryEntities() {
        return blockRepository.findAllWithQuarry();
    }

    public BlockView getBlock(Long id) {
        return blockView(blockRepository.findById(id).orElseThrow(() -> new BlockNotFoundException(id)));
    }

    public BlockView findBlock(Long id) {
        if (id == null) {
            return null;
        }
        return blockRepository.findById(id).map(this::blockView).orElse(null);
    }

    public List<BlockView> blocksInStatus(List<BlockStatus> statuses) {
        return blockRepository.findByStatusInOrderByBlockCodeAsc(statuses).stream().map(this::blockView).toList();
    }

    public BigDecimal estimatedTonnage() {
        return blockRepository.sumTotalEstimatedTonnage();
    }

    public BigDecimal actualTonnage() {
        return blockRepository.sumTotalActualTonnage();
    }

    public List<SlabView> slabsByStatus(SlabStatus status) {
        return slabRepository.findByStatus(status).stream().map(slabMapper::toDto).toList();
    }

    public List<Slab> slabsByStatusEntities(SlabStatus status) {
        return slabRepository.findByStatus(status);
    }

    public List<SlabView> slabsInStatus(List<SlabStatus> statuses) {
        return slabRepository.findByStatusIn(statuses).stream().map(slabMapper::toDto).toList();
    }

    public SlabView getSlab(Long id) {
        return slabMapper.toDto(slabRepository.findById(id).orElseThrow(() -> new SlabNotFoundException(id)));
    }

    public SlabView findSlab(Long id) {
        if (id == null) {
            return null;
        }
        return slabRepository.findById(id).map(slabMapper::toDto).orElse(null);
    }

    public SlabView findSlabWithDetails(Long id) {
        if (id == null) {
            return null;
        }
        return slabRepository.findWithDetailsById(id).map(slabMapper::toDto).orElse(null);
    }

    public SlabView getSlabWithDetails(Long id) {
        return slabMapper.toDto(slabRepository.findWithDetailsById(id).orElseThrow(() -> new SlabNotFoundException(id)));
    }

    public List<StockItemView> stockItems() {
        return BoundedReads.all(stockItemRepository).stream().map(stockItemMapper::toDto).toList();
    }

    public StockItemView findStockItem(Long id) {
        if (id == null) {
            return null;
        }
        return stockItemRepository.findById(id).map(stockItemMapper::toDto).orElse(null);
    }

    public StockItemView findStockItemWithCustomer(Long id) {
        if (id == null) {
            return null;
        }
        return stockItemRepository.findByIdWithCustomer(id).map(stockItemMapper::toDto).orElse(null);
    }

    public StockItemView getStockItem(Long id) {
        return stockItemMapper.toDto(stockItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stok kartı bulunamadı: " + id)));
    }

    public StockItemView getStockItemWithCustomer(Long id) {
        return stockItemMapper.toDto(stockItemRepository.findByIdWithCustomer(id)
                .orElseThrow(() -> new IllegalArgumentException("Stok kartı bulunamadı: " + id)));
    }

    public List<StockItemView> stockByTypeAndStatus(StockProductType type, String status) {
        return stockItemRepository.findByProductTypeAndStatus(type, status).stream().map(stockItemMapper::toDto).toList();
    }

    public List<StockItemView> positiveStock() {
        return stockItemRepository.findPositiveQuantityWithLocation().stream().map(stockItemMapper::toDto).toList();
    }

    public List<StockItem> positiveStockEntities() {
        return stockItemRepository.findPositiveQuantityWithLocation();
    }

    public List<StockItemView> stockForProject(Long projectId) {
        return stockItemRepository.findByProjectId(projectId).stream().map(stockItemMapper::toDto).toList();
    }

    public List<InvoiceView> invoices() {
        return BoundedReads.all(invoiceRepository).stream().map(this::invoiceView).toList();
    }

    public List<InvoiceView> invoicesWithCustomer() {
        return invoiceRepository.findAllWithCustomer().stream().map(this::invoiceView).toList();
    }

    public InvoiceView getInvoiceWithCustomer(Long id) {
        return invoiceView(invoiceRepository.findByIdWithCustomer(id).orElseThrow(() -> new InvoiceNotFoundException(id)));
    }

    public InvoiceView findInvoiceWithCustomer(Long id) {
        if (id == null) {
            return null;
        }
        return invoiceRepository.findByIdWithCustomer(id).map(this::invoiceView).orElse(null);
    }

    public SalesOrderView.LineView getSalesOrderItem(Long id) {
        SalesOrderItem item = salesOrderItemRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş kalemi bulunamadı: " + id));
        return salesOrderMapper.toLine(item);
    }

    public SalesOrderView.LineView findSalesOrderItem(Long id) {
        if (id == null) {
            return null;
        }
        return salesOrderItemRepository.findByIdWithDetails(id).map(salesOrderMapper::toLine).orElse(null);
    }

    private BlockView blockView(Block block) {
        return blockViews.toView(block);
    }

    private InvoiceView invoiceView(Invoice invoice) {
        InvoiceView view = invoiceMapper.toDto(invoice);
        view.setInternalTransfer(invoice.isInternalTransfer());
        if (view.getItems() != null && invoice.getItems() != null) {
            for (int i = 0; i < view.getItems().size() && i < invoice.getItems().size(); i++) {
                view.getItems().get(i).setUnitLabel(invoice.getItems().get(i).getUnitLabel());
            }
        }
        return view;
    }


}
