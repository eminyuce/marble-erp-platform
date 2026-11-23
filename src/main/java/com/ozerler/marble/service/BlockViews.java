package com.ozerler.marble.service;

import com.ozerler.marble.dto.view.BlockView;
import com.ozerler.marble.mapper.BlockMapper;
import com.ozerler.marble.model.Block;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds block views: MapStruct copies the stored columns, then the derived values the entity already
 * calculates (dimensions, tonnage, canonical status) are set here, outside the mapper.
 */
@Component
@RequiredArgsConstructor
public class BlockViews {

    private final BlockMapper blockMapper;

    /**
     * Copies a block and its derived values into a view. Call inside a transaction.
     *
     * @param block the entity
     * @return the view
     */
    public BlockView toView(Block block) {
        BlockView view = blockMapper.toDto(block);
        view.setDimensions(block.getDimensions());
        view.setWeightTons(block.getWeightTons());
        view.setCanonicalStatus(block.getCanonicalStatus());
        view.setApproximateTonnage(block.getApproximateTonnage());
        return view;
    }
}
