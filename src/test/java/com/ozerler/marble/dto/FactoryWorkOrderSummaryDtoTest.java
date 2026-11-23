package com.ozerler.marble.dto;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.FactoryWorkOrder;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.enums.FactoryWorkOrderStatus;

class FactoryWorkOrderSummaryDtoTest {

    @Test
    @DisplayName("summary DTO copies block code while the association is already loaded")
    void fromEntityCopiesBlockCode() {
        FactoryWorkOrder workOrder = FactoryWorkOrder.builder()
                .id(4L)
                .orderNo("FWO-4")
                .block(Block.builder().id(9L).blockCode("BLK-009").stoneType("Afyon Beyaz").build())
                .assignedMachine(Machine.builder().id(2L).name("ST-1").build())
                .acceptedAt(LocalDate.of(2026, 9, 17))
                .responsibleName("Ali")
                .status(FactoryWorkOrderStatus.ACCEPTED)
                .build();

        FactoryWorkOrderSummaryDto dto = FactoryWorkOrderSummaryDto.fromEntity(workOrder);

        Assertions.assertThat(dto.getId()).isEqualTo(4L);
        Assertions.assertThat(dto.getOrderNo()).isEqualTo("FWO-4");
        Assertions.assertThat(dto.getBlockCode()).isEqualTo("BLK-009");
        Assertions.assertThat(dto.getStoneType()).isEqualTo("Afyon Beyaz");
        Assertions.assertThat(dto.getMachineName()).isEqualTo("ST-1");
        Assertions.assertThat(dto.getAcceptedAt()).isEqualTo("2026-09-17");
        Assertions.assertThat(dto.getResponsibleName()).isEqualTo("Ali");
        Assertions.assertThat(dto.getStatus()).isEqualTo("ACCEPTED");
        Assertions.assertThat(dto.getStatusLabel()).isNotBlank();
    }

    @Test
    @DisplayName("summary DTO stays blank when the work order has no block")
    void fromEntityAllowsMissingBlock() {
        FactoryWorkOrder workOrder = FactoryWorkOrder.builder()
                .id(5L)
                .orderNo("FWO-5")
                .block(null)
                .status(null)
                .build();

        FactoryWorkOrderSummaryDto dto = FactoryWorkOrderSummaryDto.fromEntity(workOrder);

        Assertions.assertThat(dto.getBlockCode()).isEmpty();
        Assertions.assertThat(dto.getStoneType()).isEmpty();
        Assertions.assertThat(dto.getMachineName()).isEmpty();
        Assertions.assertThat(dto.getAcceptedAt()).isEmpty();
        Assertions.assertThat(dto.getResponsibleName()).isEmpty();
        Assertions.assertThat(dto.getStatus()).isEmpty();
        Assertions.assertThat(dto.getStatusLabel()).isEmpty();
    }
}
