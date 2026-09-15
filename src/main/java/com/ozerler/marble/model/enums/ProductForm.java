package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum ProductForm {
    SLAB("enum.product_form.slab"),
    STRIP("enum.product_form.strip"),
    SIZED_PRODUCT("enum.product_form.sized_product");

    private final String messageKey;

    ProductForm(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
