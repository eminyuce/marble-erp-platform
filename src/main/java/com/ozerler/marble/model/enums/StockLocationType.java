package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum StockLocationType {
    PRODUCTION_YARD("enum.stock_location_type.production_yard", BusinessUnit.QUARRY),
    DISPATCH_YARD("enum.stock_location_type.dispatch_yard", BusinessUnit.QUARRY),
    FACTORY_BLOCK_YARD("enum.stock_location_type.factory_block_yard", BusinessUnit.FACTORY),
    SLAB_STOCK_YARD("enum.stock_location_type.slab_stock_yard", BusinessUnit.FACTORY),
    PALLET_STOCK_YARD("enum.stock_location_type.pallet_stock_yard", BusinessUnit.FACTORY),
    WORKSHOP_STOCK("enum.stock_location_type.workshop_stock", BusinessUnit.WORKSHOP);

    private final String messageKey;
    private final BusinessUnit businessUnit;

    StockLocationType(String messageKey, BusinessUnit businessUnit) {
        this.messageKey = messageKey;
        this.businessUnit = businessUnit;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }

    public BusinessUnit getBusinessUnit() {
        return businessUnit;
    }
}
