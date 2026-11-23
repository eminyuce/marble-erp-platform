package com.ozerler.marble.service;

import java.util.Optional;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.dto.PassportResult;
import com.ozerler.marble.mapper.CutOrderMapper;
import com.ozerler.marble.mapper.ProductionOrderMapper;
import com.ozerler.marble.mapper.SlabMapper;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CutItem;
import com.ozerler.marble.model.Pallet;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CutItemRepository;
import com.ozerler.marble.repository.PalletRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.util.MessageUtils;

import lombok.RequiredArgsConstructor;

/**
 * Service managing Digital Stone Passport resolution (BRD Section 7.2)
 */
@Service
@RequiredArgsConstructor
public class PassportService {

    private final PalletRepository palletRepository;
    private final SlabRepository slabRepository;
    private final BlockRepository blockRepository;
    private final CutItemRepository cutItemRepository;
    private final ProductionOrderMapper productionOrderMapper;
    private final SlabMapper slabMapper;
    private final CutOrderMapper cutOrderMapper;
    private final BlockViews blockViews;
    private final MessageSource messageSource;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
                // best effort: fall back to the default below
            }
        }
        return MessageUtils.getMessage(code, args);
    }

    @Transactional(readOnly = true)
    public PassportResult getPassportByCode(String code) {
        if (code == null || code.isBlank()) {
            return PassportResult.builder()
                    .found(false)
                    .type("NONE")
                    .errorMessage(getMessage("passport.error.invalid_code"))
                    .build();
        }

        String cleanCode = code.trim().toUpperCase();

        Optional<Pallet> pallet = palletRepository.findByPalletCode(cleanCode);
        if (pallet.isPresent()) {
            return PassportResult.builder()
                    .found(true)
                    .type("PALLET")
                    .pallet(productionOrderMapper.toPallet(pallet.get()))
                    .title(getMessage("passport.title.pallet", pallet.get().getPalletCode()))
                    .build();
        }

        Optional<Slab> slab = slabRepository.findBySlabCodeWithBlock(cleanCode);
        if (slab.isPresent()) {
            return PassportResult.builder()
                    .found(true)
                    .type("SLAB")
                    .slab(slabMapper.toDto(slab.get()))
                    .title(getMessage("passport.title.slab", slab.get().getSlabCode()))
                    .build();
        }

        Optional<CutItem> item = cutItemRepository.findByItemCode(cleanCode);
        if (item.isPresent()) {
            return PassportResult.builder()
                    .found(true)
                    .type("ITEM")
                    .item(cutOrderMapper.toItem(item.get()))
                    .title(getMessage("passport.title.item", item.get().getItemCode()))
                    .build();
        }

        Optional<Block> block = blockRepository.findByBlockCodeWithQuarry(cleanCode);
        if (block.isPresent()) {
            return PassportResult.builder()
                    .found(true)
                    .type("BLOCK")
                    .block(blockViews.toView(block.get()))
                    .title(getMessage("passport.title.block", block.get().getBlockCode()))
                    .build();
        }

        return PassportResult.builder()
                .found(false)
                .type("NONE")
                .errorMessage(getMessage("passport.error.not_found", code))
                .build();
    }
}
