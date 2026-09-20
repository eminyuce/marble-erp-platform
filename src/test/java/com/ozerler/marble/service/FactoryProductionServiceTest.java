package com.ozerler.marble.service;

import com.ozerler.marble.model.FactoryOperation;
import com.ozerler.marble.model.FactoryWorkOrder;
import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.model.enums.OperationStatus;
import com.ozerler.marble.model.enums.QuantityUnit;
import com.ozerler.marble.repository.FactoryOperationRepository;
import com.ozerler.marble.repository.FactoryWorkOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FactoryProductionServiceTest {

    @Test
    @DisplayName("ST and Katrak cutting keep ton input and square-meter output without unit conversion")
    void cuttingUsesDifferentPhysicalUnits() {
        assertThat(FactoryProcessType.ST_CUTTING.usesSamePhysicalUnit()).isFalse();
        assertThat(FactoryProcessType.GANGSAW_CUTTING.usesSamePhysicalUnit()).isFalse();
        assertThat(FactoryProcessType.ST_CUTTING.getDefaultInputUnit()).isEqualTo(QuantityUnit.TON);
        assertThat(FactoryProcessType.GANGSAW_CUTTING.getDefaultOutputUnit()).isEqualTo(QuantityUnit.SQUARE_METER);
        assertThat(FactoryProcessType.SLAB_POLISHING.usesSamePhysicalUnit()).isTrue();
        assertThat(FactoryProcessType.ST_CUTTING.toLegacyProcessType().getLabel()).isEqualTo("ST");
        assertThat(FactoryProcessType.GANGSAW_CUTTING.toLegacyProcessType().getLabel()).isEqualTo("Katrak");
    }

    @Test
    @DisplayName("recordCutting rejects non-cutting process types before touching a block")
    void recordCuttingRejectsSurfaceProcess() {
        FactoryProductionService service = new FactoryProductionService(
                null, null, null, null, null, null, null, null, null, null, null, null);
        FactoryProductionService.CuttingRequest request = new FactoryProductionService.CuttingRequest(
                1L, "PO-1", null, "Katrak-01", FactoryProcessType.SLAB_POLISHING,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "Op", null,
                0, 1, 0, 0, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, QuantityUnit.KG, null, null, null);

        assertThatThrownBy(() -> service.recordCutting(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

@ExtendWith(MockitoExtension.class)
class FactoryProductionServiceRoutingTest {

    @Mock
    private FactoryWorkOrderRepository workOrderRepository;
    @Mock
    private FactoryOperationRepository operationRepository;

    private FactoryProductionService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new FactoryProductionService(
                null, workOrderRepository, operationRepository, null, null, null, null,
                null, null, null, null, null);
    }

    @Test
    @DisplayName("ST cutting rejects slab polishing and accepts strip polishing")
    void stCuttingRouting() {
        FactoryWorkOrder workOrder = FactoryWorkOrder.builder().id(3L).build();
        when(workOrderRepository.findById(3L)).thenReturn(Optional.of(workOrder));
        when(operationRepository.findTopByWorkOrderIdAndStatusOrderByIdDesc(3L, OperationStatus.COMPLETED))
                .thenReturn(Optional.of(FactoryOperation.builder().processType(FactoryProcessType.ST_CUTTING).build()));
        when(operationRepository.findTopByWorkOrderIdAndProcessTypeInAndStatusOrderByIdDesc(
                eq(3L), any(), eq(OperationStatus.COMPLETED)))
                .thenReturn(Optional.of(FactoryOperation.builder().processType(FactoryProcessType.ST_CUTTING).build()));

        assertThatThrownBy(() -> service.recordSurfaceOperation(3L, FactoryProcessType.SLAB_POLISHING, null, "Op",
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        when(operationRepository.save(any(FactoryOperation.class))).thenAnswer(inv -> inv.getArgument(0));
        FactoryOperation saved = service.recordSurfaceOperation(3L, FactoryProcessType.STRIP_POLISHING, null, "Op",
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, null, null);
        assertThat(saved.getProcessType()).isEqualTo(FactoryProcessType.STRIP_POLISHING);
        verify(operationRepository).save(any(FactoryOperation.class));
    }

    @Test
    @DisplayName("Katrak cutting rejects strip polishing and accepts slab polishing")
    void gangsawnRouting() {
        FactoryWorkOrder workOrder = FactoryWorkOrder.builder().id(4L).build();
        when(workOrderRepository.findById(4L)).thenReturn(Optional.of(workOrder));
        when(operationRepository.findTopByWorkOrderIdAndProcessTypeInAndStatusOrderByIdDesc(
                eq(4L), any(), eq(OperationStatus.COMPLETED)))
                .thenReturn(Optional.of(FactoryOperation.builder().processType(FactoryProcessType.GANGSAW_CUTTING).build()));

        assertThatThrownBy(() -> service.recordSurfaceOperation(4L, FactoryProcessType.STRIP_POLISHING, null, "Op",
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        when(operationRepository.findTopByWorkOrderIdAndStatusOrderByIdDesc(4L, OperationStatus.COMPLETED))
                .thenReturn(Optional.of(FactoryOperation.builder().processType(FactoryProcessType.GANGSAW_CUTTING).build()));
        when(operationRepository.save(any(FactoryOperation.class))).thenAnswer(inv -> inv.getArgument(0));
        FactoryOperation saved = service.recordSurfaceOperation(4L, FactoryProcessType.SLAB_POLISHING, null, "Op",
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, null, null);
        assertThat(saved.getProcessType()).isEqualTo(FactoryProcessType.SLAB_POLISHING);
    }
}
