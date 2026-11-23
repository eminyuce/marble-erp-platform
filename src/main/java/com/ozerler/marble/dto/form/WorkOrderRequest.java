package com.ozerler.marble.dto.form;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.ozerler.marble.model.enums.BusinessUnit;

/**
 * Raw work-order create request as posted by the form, including the optional dimension rows.
 *
 * @param orderNo           optional order number
 * @param customerId        optional customer
 * @param orderDate         order date
 * @param dueDate           due date
 * @param department        owning department
 * @param responsiblePerson responsible person
 * @param stoneType         stone type
 * @param colorQuality      colour/quality text
 * @param thicknessCm       header thickness
 * @param widthCm           header width
 * @param lengthCm          header length
 * @param quantity          header quantity
 * @param quantityUnit      header quantity unit
 * @param surfaceOperation  header surface operation
 * @param edgeOperation     header edge operation
 * @param sourceStockItemId optional source stock item
 * @param sourceSlabId      optional source slab
 * @param sourceBlockId     optional source block
 * @param usedQuantity      used source quantity
 * @param notes             free notes
 * @param dimensions        posted dimension rows
 */
public record WorkOrderRequest(
        String orderNo,
        Long customerId,
        LocalDate orderDate,
        LocalDate dueDate,
        BusinessUnit department,
        String responsiblePerson,
        String stoneType,
        String colorQuality,
        BigDecimal thicknessCm,
        BigDecimal widthCm,
        BigDecimal lengthCm,
        BigDecimal quantity,
        String quantityUnit,
        String surfaceOperation,
        String edgeOperation,
        Long sourceStockItemId,
        Long sourceSlabId,
        Long sourceBlockId,
        BigDecimal usedQuantity,
        String notes,
        Dimensions dimensions) {

    /**
     * Parallel lists of posted dimension rows; any list may be null.
     *
     * @param widths        widths in cm
     * @param lengths       lengths in cm
     * @param thicknesses   thicknesses in cm
     * @param pieceCounts   piece counts
     * @param surfaceOps    surface operations
     * @param surfaceUnits  surface units
     * @param edgeOps       edge operations
     * @param edgeUnits     edge units
     * @param notes         row notes
     */
    public record Dimensions(
            List<BigDecimal> widths,
            List<BigDecimal> lengths,
            List<BigDecimal> thicknesses,
            List<Integer> pieceCounts,
            List<String> surfaceOps,
            List<String> surfaceUnits,
            List<String> edgeOps,
            List<String> edgeUnits,
            List<String> notes) {
    }
}
