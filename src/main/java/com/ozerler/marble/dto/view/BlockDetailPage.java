package com.ozerler.marble.dto.view;

import java.math.BigDecimal;

/**
 * Everything the block detail page shows about the block itself, calculated in the service.
 *
 * @param block                    the block view
 * @param calculatedExtractionCost extraction cost from the quarry expense rate
 * @param calculatedTotalCost      extraction plus transport cost
 * @param blockMarketValue         market value from the unit price per ton
 * @param weightWarning            whether the weight deviation is above the warning limit
 * @param canDelete                whether the block has no dependent records
 */
public record BlockDetailPage(
        BlockView block,
        BigDecimal calculatedExtractionCost,
        BigDecimal calculatedTotalCost,
        BigDecimal blockMarketValue,
        boolean weightWarning,
        boolean canDelete) {
}
