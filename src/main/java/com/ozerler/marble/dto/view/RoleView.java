package com.ozerler.marble.dto.view;

import lombok.Getter;
import lombok.Setter;

/**
 * A security role as listed on the user form.
 */
@Getter
@Setter
public class RoleView {
    private Long id;
    private String name;
    private String description;
}
