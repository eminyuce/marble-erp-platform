package com.ozerler.marble.dto.form;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockLocationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockLocationForm {
    private Long id;
    private String code;
    private String name;
    private BusinessUnit businessUnit = BusinessUnit.FACTORY;
    private StockLocationType locationType = StockLocationType.FACTORY_BLOCK_YARD;
    private boolean active = true;
}
