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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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

    @InjectMocks
    private PassportService passportService;

    @Test
    @DisplayName("getPassportByCode resolves PALLET first when matching")
    void getPassportByCode_ResolvesPallet() {
        Pallet pallet = Pallet.builder().palletCode("PAL-2026-001").build();
        when(palletRepository.findByPalletCode("PAL-2026-001")).thenReturn(Optional.of(pallet));

        PassportResult result = passportService.getPassportByCode("pal-2026-001");

        assertThat(result.isFound()).isTrue();
        assertThat(result.getType()).isEqualTo("PALLET");
        assertThat(result.getPallet()).isEqualTo(pallet);
        assertThat(result.getTitle()).contains("PAL-2026-001");
    }

    @Test
    @DisplayName("getPassportByCode resolves SLAB when pallet not found")
    void getPassportByCode_ResolvesSlab() {
        Slab slab = Slab.builder().slabCode("SLB-2026-002").build();
        when(palletRepository.findByPalletCode("SLB-2026-002")).thenReturn(Optional.empty());
        when(slabRepository.findBySlabCode("SLB-2026-002")).thenReturn(Optional.of(slab));

        PassportResult result = passportService.getPassportByCode("SLB-2026-002");

        assertThat(result.isFound()).isTrue();
        assertThat(result.getType()).isEqualTo("SLAB");
        assertThat(result.getSlab()).isEqualTo(slab);
    }

    @Test
    @DisplayName("getPassportByCode returns not found when code does not match any entity")
    void getPassportByCode_NotFound() {
        when(palletRepository.findByPalletCode("UNKNOWN")).thenReturn(Optional.empty());
        when(slabRepository.findBySlabCode("UNKNOWN")).thenReturn(Optional.empty());
        when(cutItemRepository.findByItemCode("UNKNOWN")).thenReturn(Optional.empty());
        when(blockRepository.findByBlockCode("UNKNOWN")).thenReturn(Optional.empty());

        PassportResult result = passportService.getPassportByCode("UNKNOWN");

        assertThat(result.isFound()).isFalse();
        assertThat(result.getType()).isEqualTo("NONE");
        assertThat(result.getErrorMessage()).contains("UNKNOWN");
    }

    @Test
    @DisplayName("getPassportByCode handles null or blank input gracefully")
    void getPassportByCode_BlankInput() {
        PassportResult result = passportService.getPassportByCode("   ");

        assertThat(result.isFound()).isFalse();
        assertThat(result.getType()).isEqualTo("NONE");
    }
}
