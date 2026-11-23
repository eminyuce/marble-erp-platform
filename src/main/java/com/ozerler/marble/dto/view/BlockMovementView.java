package com.ozerler.marble.dto.view;

import lombok.Getter;
import lombok.Setter;

/**
 * One location change of a block.
 */
@Getter
@Setter
public class BlockMovementView {
    private Long id;
    private String description;
    private BlockView.LocationView fromLocation;
    private BlockView.LocationView toLocation;
}
