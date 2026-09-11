package com.ozerler.marble.controller;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CutItem;
import com.ozerler.marble.model.Pallet;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CutItemRepository;
import com.ozerler.marble.repository.PalletRepository;
import com.ozerler.marble.repository.SlabRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("/passport")
@RequiredArgsConstructor
public class PassportController {

    private final PalletRepository palletRepository;
    private final SlabRepository slabRepository;
    private final BlockRepository blockRepository;
    private final CutItemRepository cutItemRepository;

    /**
     * Public responsive Digital Stone Passport endpoint (BRD Section 7.2)
     */
    @GetMapping("/{code}")
    public String viewDigitalPassport(@PathVariable("code") String code, Model model) {
        String cleanCode = code.trim().toUpperCase();

        Optional<Pallet> pallet = palletRepository.findByPalletCode(cleanCode);
        if (pallet.isPresent()) {
            model.addAttribute("type", "PALLET");
            model.addAttribute("pallet", pallet.get());
            model.addAttribute("title", "Palet Pasaportu: " + pallet.get().getPalletCode());
            return "erp/passport/view";
        }

        Optional<Slab> slab = slabRepository.findBySlabCode(cleanCode);
        if (slab.isPresent()) {
            model.addAttribute("type", "SLAB");
            model.addAttribute("slab", slab.get());
            model.addAttribute("title", "Plaka Pasaportu: " + slab.get().getSlabCode());
            return "erp/passport/view";
        }

        Optional<CutItem> item = cutItemRepository.findByItemCode(cleanCode);
        if (item.isPresent()) {
            model.addAttribute("type", "ITEM");
            model.addAttribute("item", item.get());
            model.addAttribute("title", "Ebatlı Mamul Pasaportu: " + item.get().getItemCode());
            return "erp/passport/view";
        }

        Optional<Block> block = blockRepository.findByBlockCode(cleanCode);
        if (block.isPresent()) {
            model.addAttribute("type", "BLOCK");
            model.addAttribute("block", block.get());
            model.addAttribute("title", "Blok Kimlik Kartı: " + block.get().getBlockCode());
            return "erp/passport/view";
        }

        model.addAttribute("errorMessage", "Aradığınız QR koduna ait doğal taş kaydı bulunamadı: " + code);
        return "erp/passport/view";
    }
}
