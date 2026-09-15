package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.model.*;
import com.ozerler.marble.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MasterDataService {

    private static final Set<String> CORE_STOCK_LOCATION_CODES = Set.of(
            Constants.STOCK_LOCATION_PRODUCTION_YARD,
            Constants.STOCK_LOCATION_DISPATCH_YARD,
            Constants.STOCK_LOCATION_FACTORY_BLOCK_YARD,
            Constants.STOCK_LOCATION_SLAB_STOCK_YARD,
            Constants.STOCK_LOCATION_PALLET_STOCK_YARD,
            Constants.STOCK_LOCATION_WORKSHOP_STOCK
    );

    private final MachineRepository machineRepository;
    private final StockLocationRepository stockLocationRepository;
    private final QuarryRepository quarryRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final CostCenterRepository costCenterRepository;

    private final MachineFuelEntryRepository machineFuelEntryRepository;
    private final BlockRepository blockRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final CostTransactionRepository costTransactionRepository;

    // =========================================================================
    // MACHINES
    // =========================================================================

    public List<Machine> getAllMachines() {
        return machineRepository.findAllByOrderByCodeAsc();
    }

    public Machine getMachineById(Long id) {
        return machineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Makine bulunamadı: ID " + id));
    }

    @Transactional
    public Machine saveMachine(Machine machine) {
        if (machine.getCode() == null || machine.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Makine kodu boş olamaz.");
        }
        if (machine.getName() == null || machine.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Makine adı boş olamaz.");
        }

        String cleanCode = machine.getCode().trim().toUpperCase();
        machineRepository.findByCode(cleanCode).ifPresent(existing -> {
            if (machine.getId() == null || !existing.getId().equals(machine.getId())) {
                throw new IllegalArgumentException("Bu makine kodu zaten kullanımda: " + cleanCode);
            }
        });

        machine.setCode(cleanCode);
        return machineRepository.save(machine);
    }

    @Transactional
    public Machine toggleMachineActive(Long id) {
        Machine machine = getMachineById(id);
        machine.setActive(!machine.isActive());
        log.info("Machine active status toggled. Code: {}, Active: {}", machine.getCode(), machine.isActive());
        return machineRepository.save(machine);
    }

    @Transactional
    public void deleteMachine(Long id) {
        Machine machine = getMachineById(id);
        if (machineFuelEntryRepository.existsByMachineId(id)) {
            throw new IllegalStateException("Bu makineye ait yakıt kayıtları mevcuttur. Silmek yerine pasife alınız.");
        }
        machineRepository.delete(machine);
        log.info("Machine deleted successfully. ID: {}, Code: {}", id, machine.getCode());
    }

    // =========================================================================
    // STOCK LOCATIONS
    // =========================================================================

    public List<StockLocation> getAllStockLocations() {
        return stockLocationRepository.findAllByOrderByCodeAsc();
    }

    public StockLocation getStockLocationById(Long id) {
        return stockLocationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stok sahası bulunamadı: ID " + id));
    }

    @Transactional
    public StockLocation saveStockLocation(StockLocation location) {
        if (location.getCode() == null || location.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Saha / Depo kodu boş olamaz.");
        }
        if (location.getName() == null || location.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Saha / Depo adı boş olamaz.");
        }

        String cleanCode = location.getCode().trim().toUpperCase();
        stockLocationRepository.findByCode(cleanCode).ifPresent(existing -> {
            if (location.getId() == null || !existing.getId().equals(location.getId())) {
                throw new IllegalArgumentException("Bu saha / depo kodu zaten kullanımda: " + cleanCode);
            }
        });

        location.setCode(cleanCode);
        return stockLocationRepository.save(location);
    }

    @Transactional
    public StockLocation toggleStockLocationActive(Long id) {
        StockLocation location = getStockLocationById(id);
        if (CORE_STOCK_LOCATION_CODES.contains(location.getCode()) && location.isActive()) {
            throw new IllegalStateException("Temel ERP operasyon sahası pasife alınamaz: " + location.getCode());
        }
        location.setActive(!location.isActive());
        log.info("Stock location active toggled. Code: {}, Active: {}", location.getCode(), location.isActive());
        return stockLocationRepository.save(location);
    }

    @Transactional
    public void deleteStockLocation(Long id) {
        StockLocation location = getStockLocationById(id);
        if (CORE_STOCK_LOCATION_CODES.contains(location.getCode())) {
            throw new IllegalStateException("Temel ERP operasyon sahası silinemez: " + location.getCode());
        }
        if (blockRepository.existsByCurrentLocationId(id)) {
            throw new IllegalStateException("Bu sahada kayıtlı bloklar bulunmaktadır. Silmek yerine pasife alınız.");
        }
        stockLocationRepository.delete(location);
        log.info("Stock location deleted successfully. ID: {}, Code: {}", id, location.getCode());
    }

    // =========================================================================
    // QUARRIES
    // =========================================================================

    public List<Quarry> getAllQuarries() {
        return quarryRepository.findAllByOrderByCodeAsc();
    }

    public Quarry getQuarryById(Long id) {
        return quarryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ocak bulunamadı: ID " + id));
    }

    @Transactional
    public Quarry saveQuarry(Quarry quarry) {
        if (quarry.getCode() == null || quarry.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Ocak kodu boş olamaz.");
        }
        if (quarry.getName() == null || quarry.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Ocak adı boş olamaz.");
        }

        String cleanCode = quarry.getCode().trim().toUpperCase();
        quarryRepository.findByCode(cleanCode).ifPresent(existing -> {
            if (quarry.getId() == null || !existing.getId().equals(quarry.getId())) {
                throw new IllegalArgumentException("Bu ocak kodu zaten kullanımda: " + cleanCode);
            }
        });

        quarry.setCode(cleanCode);
        return quarryRepository.save(quarry);
    }

    @Transactional
    public void deleteQuarry(Long id) {
        Quarry quarry = getQuarryById(id);
        if (blockRepository.existsByQuarryId(id)) {
            throw new IllegalStateException("Bu ocağa ait blok kayıtları bulunmaktadır. İlişkili veriler korunmalıdır.");
        }
        quarryRepository.delete(quarry);
        log.info("Quarry deleted successfully. ID: {}, Code: {}", id, quarry.getCode());
    }

    // =========================================================================
    // CUSTOMERS
    // =========================================================================

    public List<Customer> getAllCustomers() {
        return customerRepository.findAllByOrderByCompanyNameAsc();
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Müşteri bulunamadı: ID " + id));
    }

    @Transactional
    public Customer saveCustomer(Customer customer) {
        if (customer.getCustomerCode() == null || customer.getCustomerCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Müşteri kodu boş olamaz.");
        }
        if (customer.getCompanyName() == null || customer.getCompanyName().trim().isEmpty()) {
            throw new IllegalArgumentException("Firma adı boş olamaz.");
        }

        String cleanCode = customer.getCustomerCode().trim().toUpperCase();
        customerRepository.findByCustomerCode(cleanCode).ifPresent(existing -> {
            if (customer.getId() == null || !existing.getId().equals(customer.getId())) {
                throw new IllegalArgumentException("Bu müşteri kodu zaten kullanımda: " + cleanCode);
            }
        });

        customer.setCustomerCode(cleanCode);
        return customerRepository.save(customer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        if (salesOrderRepository.count() > 0) {
            boolean hasOrders = salesOrderRepository.findAll().stream()
                    .anyMatch(so -> customer.getCompanyName().equalsIgnoreCase(so.getCustomerName()));
            if (hasOrders) {
                throw new IllegalStateException("Bu müşteriye ait satış siparişleri mevcuttur. Müşteri silinemez.");
            }
        }
        customerRepository.delete(customer);
        log.info("Customer deleted successfully. ID: {}, Code: {}", id, customer.getCustomerCode());
    }

    // =========================================================================
    // SUPPLIERS
    // =========================================================================

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAllByOrderByCompanyNameAsc();
    }

    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tedarikçi bulunamadı: ID " + id));
    }

    @Transactional
    public Supplier saveSupplier(Supplier supplier) {
        if (supplier.getSupplierCode() == null || supplier.getSupplierCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Tedarikçi kodu boş olamaz.");
        }
        if (supplier.getCompanyName() == null || supplier.getCompanyName().trim().isEmpty()) {
            throw new IllegalArgumentException("Firma adı boş olamaz.");
        }

        String cleanCode = supplier.getSupplierCode().trim().toUpperCase();
        supplierRepository.findBySupplierCode(cleanCode).ifPresent(existing -> {
            if (supplier.getId() == null || !existing.getId().equals(supplier.getId())) {
                throw new IllegalArgumentException("Bu tedarikçi kodu zaten kullanımda: " + cleanCode);
            }
        });

        supplier.setSupplierCode(cleanCode);
        return supplierRepository.save(supplier);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = getSupplierById(id);
        boolean hasOrders = purchaseOrderRepository.findAll().stream()
                .anyMatch(po -> supplier.getId().equals(po.getSupplierId()) ||
                        supplier.getCompanyName().equalsIgnoreCase(po.getSupplierName()));
        if (hasOrders) {
            throw new IllegalStateException("Bu tedarikçiye ait satın alma siparişleri mevcuttur. Tedarikçi silinemez.");
        }
        supplierRepository.delete(supplier);
        log.info("Supplier deleted successfully. ID: {}, Code: {}", id, supplier.getSupplierCode());
    }

    // =========================================================================
    // COST CENTERS
    // =========================================================================

    public List<CostCenter> getAllCostCenters() {
        return costCenterRepository.findAllByOrderByCodeAsc();
    }

    public CostCenter getCostCenterById(Long id) {
        return costCenterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Masraf merkezi bulunamadı: ID " + id));
    }

    @Transactional
    public CostCenter saveCostCenter(CostCenter costCenter) {
        if (costCenter.getCode() == null || costCenter.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Masraf merkezi kodu boş olamaz.");
        }
        if (costCenter.getName() == null || costCenter.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Masraf merkezi adı boş olamaz.");
        }

        String cleanCode = costCenter.getCode().trim().toUpperCase();
        costCenterRepository.findByCode(cleanCode).ifPresent(existing -> {
            if (costCenter.getId() == null || !existing.getId().equals(costCenter.getId())) {
                throw new IllegalArgumentException("Bu masraf merkezi kodu zaten kullanımda: " + cleanCode);
            }
        });

        costCenter.setCode(cleanCode);
        return costCenterRepository.save(costCenter);
    }

    @Transactional
    public void deleteCostCenter(Long id) {
        CostCenter costCenter = getCostCenterById(id);
        if (costTransactionRepository.countByCenterId(id) > 0) {
            throw new IllegalStateException("Bu masraf merkezine bağlı maliyet fişleri/hareketleri bulunmaktadır.");
        }
        costCenterRepository.delete(costCenter);
        log.info("Cost center deleted successfully. ID: {}, Code: {}", id, costCenter.getCode());
    }

    // =========================================================================
    // SUMMARY COUNTS FOR HUB
    // =========================================================================

    public DefinitionCounts getSummaryCounts() {
        return new DefinitionCounts(
                machineRepository.count(),
                machineRepository.countByActiveTrue(),
                stockLocationRepository.count(),
                stockLocationRepository.countByActiveTrue(),
                quarryRepository.count(),
                customerRepository.count(),
                supplierRepository.count(),
                costCenterRepository.count()
        );
    }

    public record DefinitionCounts(
            long totalMachines,
            long activeMachines,
            long totalStockLocations,
            long activeStockLocations,
            long totalQuarries,
            long totalCustomers,
            long totalSuppliers,
            long totalCostCenters
    ) {}
}
