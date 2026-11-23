package com.ozerler.marble.dto.option;

public record CostCenterOption(Long id, String code, String name) {
    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
