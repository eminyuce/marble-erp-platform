package com.ozerler.marble.service;

import com.ozerler.marble.repository.CutItemRepository;
import com.ozerler.marble.repository.CutOrderRepository;
import com.ozerler.marble.repository.ProjectLocationRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.ScrapLogRepository;
import com.ozerler.marble.repository.SlabRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class WorkshopCutServiceTest {

    @Mock
    private CutOrderRepository cutOrderRepository;
    @Mock
    private CutItemRepository cutItemRepository;
    @Mock
    private SlabRepository slabRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectLocationRepository projectLocationRepository;
    @Mock
    private ScrapLogRepository scrapLogRepository;

    private WorkshopCutService workshopCutService;

    @BeforeEach
    void setUp() {
        workshopCutService = new WorkshopCutService(
                cutOrderRepository, cutItemRepository, slabRepository,
                projectRepository, projectLocationRepository, scrapLogRepository
        );
    }

    @Test
    @DisplayName("createCutOrder throws IllegalArgumentException when piecesCount <= 0")
    void createCutOrder_ZeroPieces_ThrowsException() {
        assertThatThrownBy(() -> workshopCutService.createCutOrder(
                1L, 1L, 1L, "BridgeSaw-01", "Ahmet",
                0, new BigDecimal("60"), new BigDecimal("120"),
                "PAHLI", "Banyo", "Notlar"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Parça adedi");
    }

    @Test
    @DisplayName("createCutOrder throws IllegalArgumentException when target dimensions are invalid")
    void createCutOrder_InvalidDimensions_ThrowsException() {
        assertThatThrownBy(() -> workshopCutService.createCutOrder(
                1L, 1L, 1L, "BridgeSaw-01", "Ahmet",
                5, BigDecimal.ZERO, new BigDecimal("120"),
                "PAHLI", "Banyo", "Notlar"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Hedef genişlik");
    }

    @Test
    @DisplayName("getCutOrderById throws NullPointerException when ID is null")
    void getCutOrderById_NullId_ThrowsException() {
        assertThatThrownBy(() -> workshopCutService.getCutOrderById(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ID");
    }
}
