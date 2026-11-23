package com.ozerler.marble.dto;

import com.ozerler.marble.dto.view.BlockView;
import com.ozerler.marble.dto.view.CutOrderView;
import com.ozerler.marble.dto.view.PalletRef;
import com.ozerler.marble.dto.view.SlabView;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PassportResult {
    private final String type;
    private final String title;
    private final PalletRef pallet;
    private final SlabView slab;
    private final CutOrderView.ItemView item;
    private final BlockView block;
    private final String errorMessage;
    private final boolean found;
}
