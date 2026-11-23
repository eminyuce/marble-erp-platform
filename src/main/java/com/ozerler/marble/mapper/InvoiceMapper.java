package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.view.InvoiceRef;
import com.ozerler.marble.dto.view.InvoiceView;
import com.ozerler.marble.model.CollectionRecord;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.InvoiceItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {CustomerMapper.class, SupplierMapper.class, ProjectMapper.class, BlockMapper.class, SlabMapper.class,
                StockItemMapper.class})
public interface InvoiceMapper {

    @Mapping(target = "invoiceNumber", source = "invoiceNo")
    @Mapping(target = "internalTransfer", ignore = true)
    InvoiceView toDto(Invoice invoice);

    InvoiceRef toRef(Invoice invoice);

    @Mapping(target = "unitLabel", ignore = true)
    InvoiceView.LineView toLine(InvoiceItem item);

    InvoiceView.CollectionView toCollection(CollectionRecord record);
}
