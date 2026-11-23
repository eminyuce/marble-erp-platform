package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.view.CheckRef;
import com.ozerler.marble.dto.view.CollectionRef;
import com.ozerler.marble.model.CheckRecord;
import com.ozerler.marble.model.CollectionRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Copies collections and checks into references. No calculation happens here.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {CustomerMapper.class, InvoiceMapper.class})
public interface CollectionMapper {

    /**
     * Copies the id and number of a collection.
     *
     * @param collection the entity
     * @return the reference
     */
    CollectionRef toRef(CollectionRecord collection);

    /**
     * Copies the id and number of a check.
     *
     * @param check the entity
     * @return the reference
     */
    CheckRef toRef(CheckRecord check);
}
