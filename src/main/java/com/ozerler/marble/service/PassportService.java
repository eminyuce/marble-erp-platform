package com.ozerler.marble.service;

import com.ozerler.marble.dto.PassportResult;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CutItem;
import com.ozerler.marble.model.Pallet;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CutItemRepository;
import com.ozerler.marble.repository.PalletRepository;
import com.ozerler.marble.repository.SlabRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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

    @Transactional(readOnly = true)
    public PassportResult getPassportByCode(String code) {
        if (code == null || code.isBlank()) {
            return PassportResult.builder()
                    .found(false)
                    .type("NONE")
                    .errorMessage("Geçersiz veya boş QR pasaport kodu.")
                    .build();
        }

        String cleanCode = code.trim().toUpperCase();

        Optional<Pallet> pallet = palletRepository.findByPalletCode(cleanCode);
        if (pallet.isPresent()) {
            return PassportResult.builder()
                    .found(true)
                    .type("PALLET")
                    .pallet(pallet.get())
                    .title("Palet Pasaportu: " + pallet.get().getPalletCode())
                    .build();
        }

        Optional<Slab> slab = slabRepository.findBySlabCode(cleanCode);
        if (slab.isPresent()) {
            return PassportResult.builder()
                    .found(true)
                    .type("SLAB")
                    .slab(slab.get())
                    .title("Plaka Pasaportu: " + slab.get().getSlabCode())
                    .build();
        }

        Optional<CutItem> item = cutItemRepository.findByItemCode(cleanCode);
        if (item.isPresent()) {
            return PassportResult.builder()
                    .found(true)
                    .type("ITEM")
                    .item(item.get())
                    .title("Ebatlı Mamul Pasaportu: " + item.get().getItemCode())
                    .build();
        }

        Optional<Block> block = blockRepository.findByBlockCode(cleanCode);
        if (block.isPresent()) {
            return PassportResult.builder()
                    .found(true)
                    .type("BLOCK")
                    .block(block.get())
                    .title("Blok Kimlik Kartı: " + block.get().getBlockCode())
                    .build();
        }

        return PassportResult.builder()
                .found(false)
                .type("NONE")
                .errorMessage("Aradığınız QR koduna ait doğal taş kaydı bulunamadı: " + code)
                .build();
    }
}
