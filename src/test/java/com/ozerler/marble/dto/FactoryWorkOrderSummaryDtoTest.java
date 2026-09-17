package com.ozerler.marble.dto;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.FactoryWorkOrder;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.enums.FactoryWorkOrderStatus;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(dto.getId()).isEqualTo(4L);
        assertThat(dto.getOrderNo()).isEqualTo("FWO-4");
        assertThat(dto.getBlockCode()).isEqualTo("BLK-009");
        assertThat(dto.getStoneType()).isEqualTo("Afyon Beyaz");
        assertThat(dto.getMachineName()).isEqualTo("ST-1");
        assertThat(dto.getAcceptedAt()).isEqualTo("2026-09-17");
        assertThat(dto.getResponsibleName()).isEqualTo("Ali");
        assertThat(dto.getStatus()).isEqualTo("ACCEPTED");
        assertThat(dto.getStatusLabel()).isNotBlank();
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

        assertThat(dto.getBlockCode()).isEmpty();
        assertThat(dto.getStoneType()).isEmpty();
        assertThat(dto.getMachineName()).isEmpty();
        assertThat(dto.getAcceptedAt()).isEmpty();
        assertThat(dto.getResponsibleName()).isEmpty();
        assertThat(dto.getStatus()).isEmpty();
        assertThat(dto.getStatusLabel()).isEmpty();
    }
}
