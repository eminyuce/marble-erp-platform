package com.ozerler.marble.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.ozerler.marble.dto.view.InvoiceRef;
import com.ozerler.marble.dto.view.InvoiceView;
import com.ozerler.marble.model.CollectionRecord;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.InvoiceItem;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {CustomerMapper.class, SupplierMapper.class, ProjectMapper.class, BlockMapper.class, SlabMapper.class,
                StockItemMapper.class})
public interface InvoiceMapper {

    @Mapping(target = "invoiceNumber", source = "invoiceNo")
    @Mapping(target = "internalTransfer", ignore = true)
    InvoiceView toDto(Invoice invoice);

    /**
     * Copies the header fields only, for list rows; lines and collections are left empty.
     *
     * @param invoice the entity
     * @return the header view
     */
    @Named("row")
    @Mapping(target = "invoiceNumber", source = "invoiceNo")
    @Mapping(target = "internalTransfer", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "collections", ignore = true)
    InvoiceView toRow(Invoice invoice);

    InvoiceRef toRef(Invoice invoice);

    @Mapping(target = "unitLabel", ignore = true)
    InvoiceView.LineView toLine(InvoiceItem item);

    InvoiceView.CollectionView toCollection(CollectionRecord record);
}
