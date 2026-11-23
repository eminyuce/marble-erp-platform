package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ozerler.marble.model.FactoryOperation;
import com.ozerler.marble.model.FactoryWorkOrder;
import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.model.enums.OperationStatus;
import com.ozerler.marble.repository.FactoryOperationRepository;
import com.ozerler.marble.repository.FactoryWorkOrderRepository;

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
                null, null, null, null, workOrderRepository, operationRepository, null, null, null, null,
                null, null, null, null, null);
    }

    @Test
    @DisplayName("ST cutting rejects slab polishing and accepts strip polishing")
    void stCuttingRouting() {
        FactoryWorkOrder workOrder = FactoryWorkOrder.builder().id(3L).build();
        Mockito.when(workOrderRepository.findById(3L)).thenReturn(Optional.of(workOrder));
        Mockito.when(operationRepository.findTopByWorkOrderIdAndStatusOrderByIdDesc(3L, OperationStatus.COMPLETED))
                .thenReturn(Optional.of(FactoryOperation.builder().processType(FactoryProcessType.ST_CUTTING).build()));
        Mockito.when(operationRepository.findTopByWorkOrderIdAndProcessTypeInAndStatusOrderByIdDesc(
                ArgumentMatchers.eq(3L), ArgumentMatchers.any(), ArgumentMatchers.eq(OperationStatus.COMPLETED)))
                .thenReturn(Optional.of(FactoryOperation.builder().processType(FactoryProcessType.ST_CUTTING).build()));

        Assertions.assertThatThrownBy(() -> service.recordSurfaceOperation(3L, FactoryProcessType.SLAB_POLISHING, null, "Op",
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        Mockito.when(operationRepository.save(ArgumentMatchers.any(FactoryOperation.class))).thenAnswer(inv -> inv.getArgument(0));
        FactoryOperation saved = service.recordSurfaceOperation(3L, FactoryProcessType.STRIP_POLISHING, null, "Op",
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, null, null);
        Assertions.assertThat(saved.getProcessType()).isEqualTo(FactoryProcessType.STRIP_POLISHING);
        Mockito.verify(operationRepository).save(ArgumentMatchers.any(FactoryOperation.class));
    }

    @Test
    @DisplayName("Katrak cutting rejects strip polishing and accepts slab polishing")
    void gangsawnRouting() {
        FactoryWorkOrder workOrder = FactoryWorkOrder.builder().id(4L).build();
        Mockito.when(workOrderRepository.findById(4L)).thenReturn(Optional.of(workOrder));
        Mockito.when(operationRepository.findTopByWorkOrderIdAndProcessTypeInAndStatusOrderByIdDesc(
                ArgumentMatchers.eq(4L), ArgumentMatchers.any(), ArgumentMatchers.eq(OperationStatus.COMPLETED)))
                .thenReturn(Optional.of(FactoryOperation.builder().processType(FactoryProcessType.GANGSAW_CUTTING).build()));

        Assertions.assertThatThrownBy(() -> service.recordSurfaceOperation(4L, FactoryProcessType.STRIP_POLISHING, null, "Op",
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        Mockito.when(operationRepository.findTopByWorkOrderIdAndStatusOrderByIdDesc(4L, OperationStatus.COMPLETED))
                .thenReturn(Optional.of(FactoryOperation.builder().processType(FactoryProcessType.GANGSAW_CUTTING).build()));
        Mockito.when(operationRepository.save(ArgumentMatchers.any(FactoryOperation.class))).thenAnswer(inv -> inv.getArgument(0));
        FactoryOperation saved = service.recordSurfaceOperation(4L, FactoryProcessType.SLAB_POLISHING, null, "Op",
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, null, null);
        Assertions.assertThat(saved.getProcessType()).isEqualTo(FactoryProcessType.SLAB_POLISHING);
    }

    @Test
    @DisplayName("updateSurfaceOperation updates fields and calculates total cost correctly")
    void updateSurfaceOperationSuccess() {
        FactoryOperation existing = FactoryOperation.builder()
                .id(10L)
                .processType(FactoryProcessType.SLAB_POLISHING)
                .operatorName("Old Op")
                .inputQuantity(BigDecimal.TEN)
                .outputQuantity(BigDecimal.valueOf(8))
                .wasteQuantity(BigDecimal.valueOf(2))
                .build();

        Mockito.when(operationRepository.findById(10L)).thenReturn(Optional.of(existing));
        Mockito.when(operationRepository.save(ArgumentMatchers.any(FactoryOperation.class))).thenAnswer(inv -> inv.getArgument(0));

        FactoryOperation updated = service.updateSurfaceOperation(10L, null, "New Op",
                new BigDecimal("20.00"), new BigDecimal("18.00"), new BigDecimal("2.00"),
                null, "Guncellendi", new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("25"));

        Assertions.assertThat(updated.getOperatorName()).isEqualTo("New Op");
        Assertions.assertThat(updated.getInputQuantity()).isEqualByComparingTo(new BigDecimal("20.00"));
        Assertions.assertThat(updated.getOutputQuantity()).isEqualByComparingTo(new BigDecimal("18.00"));
        Assertions.assertThat(updated.getWasteQuantity()).isEqualByComparingTo(new BigDecimal("2.00"));
        Assertions.assertThat(updated.getTotalOperationCost()).isEqualByComparingTo(new BigDecimal("175"));
        Assertions.assertThat(updated.getNotes()).isEqualTo("Guncellendi");
    }
}
