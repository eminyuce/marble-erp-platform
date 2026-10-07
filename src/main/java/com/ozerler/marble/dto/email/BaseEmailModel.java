package com.ozerler.marble.dto.email;

import java.util.Map;

/**
 * Base contract for strongly-typed ERP email template models.
 * Guarantees standard template resolution and placeholder extraction.
 */
public interface BaseEmailModel {

    /**
     * Unique database template key (e.g. ORDER_CONFIRMATION, SHIPMENT_DISPATCH).
     */
    String getTemplateKey();

    /**
     * Converts model properties into email template variable map.
     */
    Map<String, String> toVariables();

}
