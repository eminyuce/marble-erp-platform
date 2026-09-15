package com.ozerler.marble.service;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.QuarryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuarryBlockServiceTest {

    @Mock
    private BlockRepository blockRepository;
    @Mock
    private QuarryRepository quarryRepository;

    private QuarryBlockService quarryBlockService;

    @BeforeEach
    void setUp() {
        quarryBlockService = new QuarryBlockService(blockRepository, quarryRepository, null, null, null, null);
    }

    @Test
    @DisplayName("getBlockWithDetails returns the block loaded with its quarry")
    void getBlockWithDetails_ReturnsBlock() {
        Block block = Block.builder()
                .id(5L)
                .blockCode("BLK-001")
                .quarry(Quarry.builder().id(1L).name("Ana Ocak").build())
                .build();
        when(blockRepository.findByIdWithQuarry(5L)).thenReturn(Optional.of(block));

        Block result = quarryBlockService.getBlockWithDetails(5L);

        assertThat(result.getBlockCode()).isEqualTo("BLK-001");
        assertThat(result.getQuarry().getName()).isEqualTo("Ana Ocak");
        verify(blockRepository).findByIdWithQuarry(5L);
    }

    @Test
    @DisplayName("getBlockWithDetails throws when the block is missing")
    void getBlockWithDetails_MissingBlock_Throws() {
        when(blockRepository.findByIdWithQuarry(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quarryBlockService.getBlockWithDetails(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getBlockWithDetails rejects a null id")
    void getBlockWithDetails_NullId_Throws() {
        assertThatThrownBy(() -> quarryBlockService.getBlockWithDetails(null))
                .isInstanceOf(NullPointerException.class);
    }
}
