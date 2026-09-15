package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.repository.StockLocationRepository;

/**
 * Resolves seeded stock yards and creates them on first use (H2 tests have no Flyway).
 */
final class StockLocations {

    private StockLocations() {
    }

    static StockLocation require(StockLocationRepository repository, StockLocationType type) {
        return repository.findByLocationTypeAndActiveTrue(type)
                .or(() -> repository.findByCode(codeFor(type)))
                .map(location -> {
                    if (!location.isActive()) {
                        location.setActive(true);
                    }
                    return location;
                })
                .orElseGet(() -> repository.save(StockLocation.builder()
                        .code(codeFor(type))
                        .name(type.getLabel())
                        .businessUnit(type.getBusinessUnit())
                        .locationType(type)
                        .active(true)
                        .build()));
    }

    static String codeFor(StockLocationType type) {
        return switch (type) {
            case PRODUCTION_YARD -> Constants.STOCK_LOCATION_PRODUCTION_YARD;
            case DISPATCH_YARD -> Constants.STOCK_LOCATION_DISPATCH_YARD;
            case FACTORY_BLOCK_YARD -> Constants.STOCK_LOCATION_FACTORY_BLOCK_YARD;
            case SLAB_STOCK_YARD -> Constants.STOCK_LOCATION_SLAB_STOCK_YARD;
            case PALLET_STOCK_YARD -> Constants.STOCK_LOCATION_PALLET_STOCK_YARD;
            case WORKSHOP_STOCK -> Constants.STOCK_LOCATION_WORKSHOP_STOCK;
        };
    }
}
