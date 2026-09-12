package com.ozerler.marble.dto;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CutItem;
import com.ozerler.marble.model.Pallet;
import com.ozerler.marble.model.Slab;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PassportResult {
    private final String type;
    private final String title;
    private final Pallet pallet;
    private final Slab slab;
    private final CutItem item;
    private final Block block;
    private final String errorMessage;
    private final boolean found;
}
