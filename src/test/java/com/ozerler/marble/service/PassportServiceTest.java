package com.ozerler.marble.service;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ozerler.marble.dto.PassportResult;
import com.ozerler.marble.mapper.CutOrderMapper;
import com.ozerler.marble.mapper.ProductionOrderMapper;
import com.ozerler.marble.mapper.SlabMapper;
import com.ozerler.marble.model.Pallet;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CutItemRepository;
import com.ozerler.marble.repository.PalletRepository;
import com.ozerler.marble.repository.SlabRepository;

@ExtendWith(MockitoExtension.class)
class PassportServiceTest {

    @Mock
    private PalletRepository palletRepository;
    @Mock
    private SlabRepository slabRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private CutItemRepository cutItemRepository;
    @Mock
    private ProductionOrderMapper productionOrderMapper;
    @Mock
    private SlabMapper slabMapper;
    @Mock
    private CutOrderMapper cutOrderMapper;
    @Mock
    private BlockViews blockViews;

    @InjectMocks
    private PassportService passportService;

    @Test
    @DisplayName("getPassportByCode resolves PALLET first when matching")
    void getPassportByCode_ResolvesPallet() {
        Pallet pallet = Pallet.builder().palletCode("PAL-2026-001").build();
        Mockito.when(palletRepository.findByPalletCode("PAL-2026-001")).thenReturn(Optional.of(pallet));
        com.ozerler.marble.dto.view.PalletRef palletView = new com.ozerler.marble.dto.view.PalletRef();
        palletView.setPalletCode("PAL-2026-001");
        Mockito.when(productionOrderMapper.toPallet(pallet)).thenReturn(palletView);

        PassportResult result = passportService.getPassportByCode("pal-2026-001");

        Assertions.assertThat(result.isFound()).isTrue();
        Assertions.assertThat(result.getType()).isEqualTo("PALLET");
        Assertions.assertThat(result.getPallet().getPalletCode()).isEqualTo("PAL-2026-001");
        Assertions.assertThat(result.getTitle()).contains("PAL-2026-001");
    }

    @Test
    @DisplayName("getPassportByCode resolves SLAB when pallet not found")
    void getPassportByCode_ResolvesSlab() {
        Slab slab = Slab.builder().slabCode("SLB-2026-002").build();
        Mockito.when(palletRepository.findByPalletCode("SLB-2026-002")).thenReturn(Optional.empty());
        Mockito.when(slabRepository.findBySlabCodeWithBlock("SLB-2026-002")).thenReturn(Optional.of(slab));
        com.ozerler.marble.dto.view.SlabView slabView = new com.ozerler.marble.dto.view.SlabView();
        slabView.setSlabCode("SLB-2026-002");
        Mockito.when(slabMapper.toDto(slab)).thenReturn(slabView);

        PassportResult result = passportService.getPassportByCode("SLB-2026-002");

        Assertions.assertThat(result.isFound()).isTrue();
        Assertions.assertThat(result.getType()).isEqualTo("SLAB");
        Assertions.assertThat(result.getSlab().getSlabCode()).isEqualTo("SLB-2026-002");
    }

    @Test
    @DisplayName("getPassportByCode returns not found when code does not match any entity")
    void getPassportByCode_NotFound() {
        Mockito.when(palletRepository.findByPalletCode("UNKNOWN")).thenReturn(Optional.empty());
        Mockito.when(slabRepository.findBySlabCodeWithBlock("UNKNOWN")).thenReturn(Optional.empty());
        Mockito.when(cutItemRepository.findByItemCode("UNKNOWN")).thenReturn(Optional.empty());
        Mockito.when(blockRepository.findByBlockCodeWithQuarry("UNKNOWN")).thenReturn(Optional.empty());

        PassportResult result = passportService.getPassportByCode("UNKNOWN");

        Assertions.assertThat(result.isFound()).isFalse();
        Assertions.assertThat(result.getType()).isEqualTo("NONE");
        Assertions.assertThat(result.getErrorMessage()).contains("UNKNOWN");
    }

    @Test
    @DisplayName("getPassportByCode handles null or blank input gracefully")
    void getPassportByCode_BlankInput() {
        PassportResult result = passportService.getPassportByCode("   ");

        Assertions.assertThat(result.isFound()).isFalse();
        Assertions.assertThat(result.getType()).isEqualTo("NONE");
    }
}
