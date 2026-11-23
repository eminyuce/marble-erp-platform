package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.form.CostCenterForm;
import com.ozerler.marble.dto.form.CustomerForm;
import com.ozerler.marble.dto.form.MachineForm;
import com.ozerler.marble.dto.form.QuarryForm;
import com.ozerler.marble.dto.form.StockLocationForm;
import com.ozerler.marble.dto.form.SupplierForm;
import com.ozerler.marble.dto.view.InvoiceRef;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * Maps JPA entities to the forms and view objects the web layer is allowed to hold.
 * Unmapped entity fields stay on the persistence object; they are not part of the form.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface ErpViewMapper {

    CustomerForm toForm(Customer customer);

    void update(CustomerForm form, @MappingTarget Customer customer);

    SupplierForm toForm(Supplier supplier);

    void update(SupplierForm form, @MappingTarget Supplier supplier);

    MachineForm toForm(Machine machine);

    void update(MachineForm form, @MappingTarget Machine machine);

    QuarryForm toForm(Quarry quarry);

    void update(QuarryForm form, @MappingTarget Quarry quarry);

    StockLocationForm toForm(StockLocation location);

    void update(StockLocationForm form, @MappingTarget StockLocation location);

    CostCenterForm toForm(CostCenter costCenter);

    void update(CostCenterForm form, @MappingTarget CostCenter costCenter);

    InvoiceRef toRef(Invoice invoice);
}
