package com.ozerler.marble.dto.option;

public record ProjectOption(Long id, String name, String projectCode) {
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProjectCode() {
        return projectCode;
    }
}
