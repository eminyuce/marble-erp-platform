package com.ozerler.marble.dto.option;

/**
 * Customer or supplier row for a select list. The view reads id and companyName only.
 */
public record PartyOption(Long id, String companyName) {
    public Long getId() {
        return id;
    }

    public String getCompanyName() {
        return companyName;
    }
}
