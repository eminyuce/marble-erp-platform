package com.ozerler.marble.dto.option;

import java.math.BigDecimal;

public record QuarryOption(Long id, String name, String location, BigDecimal specificGravity) {
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public BigDecimal getSpecificGravity() {
        return specificGravity;
    }
}
