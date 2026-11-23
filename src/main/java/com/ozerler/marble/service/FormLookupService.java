package com.ozerler.marble.service;

import com.ozerler.marble.dto.option.CostCenterOption;
import com.ozerler.marble.dto.option.PartyOption;
import com.ozerler.marble.dto.option.ProjectOption;
import com.ozerler.marble.dto.option.QuarryOption;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.Quarry;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Form and select data for ERP screens. Controllers ask this service instead of repositories.
 * Party, project, quarry, and cost-center lists are read models. Aggregates that the screen
 * edits (blocks, slabs, stock, invoices) stay entities because the template reads their graph.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FormLookupService {

    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final ProjectRepository projectRepository;
    private final QuarryRepository quarryRepository;
    private final CostCenterRepository costCenterRepository;
    private final BlockRepository blockRepository;
    private final SlabRepository slabRepository;
    private final StockItemRepository stockItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;

    public List<PartyOption> customers() {
        return partyOptions(BoundedReads.all(customerRepository));
    }

    public List<PartyOption> customersByCompanyName() {
        return partyOptions(BoundedReads.all(customerRepository, Sort.by("companyName")));
    }

    public Optional<Customer> customer(Long id) {
        return customerRepository.findById(id);
    }

    public List<PartyOption> suppliers() {
        return BoundedReads.all(supplierRepository).stream()
                .map(supplier -> new PartyOption(supplier.getId(), supplier.getCompanyName()))
                .toList();
    }

    public List<ProjectOption> projects() {
        return BoundedReads.all(projectRepository).stream()
                .map(project -> new ProjectOption(project.getId(), project.getName(), project.getProjectCode()))
                .toList();
    }

    public Optional<Project> project(Long id) {
        return projectRepository.findById(id);
    }

    public List<Project> projectsForCustomer(Long customerId) {
        return projectRepository.findByCustomerIdOrderByStartDateDesc(customerId);
    }

    public List<QuarryOption> quarries() {
        return BoundedReads.all(quarryRepository).stream()
                .map(quarry -> new QuarryOption(
                        quarry.getId(), quarry.getName(), quarry.getLocation(), quarry.getSpecificGravity()))
                .toList();
    }

    public Optional<Quarry> quarry(Long id) {
        return quarryRepository.findById(id);
    }

    public List<CostCenterOption> costCenters() {
        return BoundedReads.all(costCenterRepository).stream()
                .map(FormLookupService::costCenterOption)
                .toList();
    }

    public List<CostCenterOption> costCenters(BusinessUnit unit) {
        return costCenterRepository.findByBusinessUnit(unit).stream()
                .map(FormLookupService::costCenterOption)
                .toList();
    }

    public Optional<CostCenter> firstCostCenter(BusinessUnit unit) {
        return costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(unit);
    }

    public Optional<CostCenter> anyCostCenter() {
        return BoundedReads.all(costCenterRepository).stream().findFirst();
    }

    public List<Block> blocksWithQuarry() {
        return blockRepository.findAllWithQuarry();
    }

    public Optional<Block> block(Long id) {
        return blockRepository.findById(id);
    }

    public List<Block> blocksInStatus(List<BlockStatus> statuses) {
        return blockRepository.findByStatusInOrderByBlockCodeAsc(statuses);
    }

    public java.math.BigDecimal estimatedTonnage() {
        return blockRepository.sumTotalEstimatedTonnage();
    }

    public java.math.BigDecimal actualTonnage() {
        return blockRepository.sumTotalActualTonnage();
    }

    public List<Slab> slabsByStatus(SlabStatus status) {
        return slabRepository.findByStatus(status);
    }

    public List<Slab> slabsInStatus(List<SlabStatus> statuses) {
        return slabRepository.findByStatusIn(statuses);
    }

    public Optional<Slab> slab(Long id) {
        return slabRepository.findById(id);
    }

    public Optional<Slab> slabWithDetails(Long id) {
        return slabRepository.findWithDetailsById(id);
    }

    public List<StockItem> stockItems() {
        return BoundedReads.all(stockItemRepository);
    }

    public Optional<StockItem> stockItem(Long id) {
        return stockItemRepository.findById(id);
    }

    public Optional<StockItem> stockItemWithCustomer(Long id) {
        return stockItemRepository.findByIdWithCustomer(id);
    }

    public List<StockItem> stockByTypeAndStatus(StockProductType type, String status) {
        return stockItemRepository.findByProductTypeAndStatus(type, status);
    }

    public List<StockItem> positiveStock() {
        return stockItemRepository.findPositiveQuantityWithLocation();
    }

    public List<StockItem> stockForProject(Long projectId) {
        return stockItemRepository.findByProjectId(projectId);
    }

    public List<Invoice> invoices() {
        return BoundedReads.all(invoiceRepository);
    }

    public List<Invoice> invoicesWithCustomer() {
        return invoiceRepository.findAllWithCustomer();
    }

    public Optional<Invoice> invoiceWithCustomer(Long id) {
        return invoiceRepository.findByIdWithCustomer(id);
    }

    public Optional<SalesOrderItem> salesOrderItem(Long id) {
        return salesOrderItemRepository.findByIdWithDetails(id);
    }

    private static List<PartyOption> partyOptions(List<Customer> customers) {
        return customers.stream()
                .map(customer -> new PartyOption(customer.getId(), customer.getCompanyName()))
                .toList();
    }

    private static CostCenterOption costCenterOption(CostCenter center) {
        return new CostCenterOption(center.getId(), center.getCode(), center.getName());
    }
}
