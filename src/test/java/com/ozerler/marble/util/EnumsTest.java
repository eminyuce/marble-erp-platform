package com.ozerler.marble.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.model.enums.OperationWorkOrderStatus;

class EnumsTest {

    @Test
    @DisplayName("parseOrNull accepts real work-order statuses, including mixed case")
    void parseOrNullAcceptsValidWorkOrderStatuses() {
        Assertions.assertThat(Enums.parseOrNull(OperationWorkOrderStatus.class, "NEW"))
                .isEqualTo(OperationWorkOrderStatus.NEW);
        Assertions.assertThat(Enums.parseOrNull(OperationWorkOrderStatus.class, "in_production"))
                .isEqualTo(OperationWorkOrderStatus.IN_PRODUCTION);
        Assertions.assertThat(Enums.parseOrNull(OperationWorkOrderStatus.class, " DISPATCHED "))
                .isEqualTo(OperationWorkOrderStatus.DISPATCHED);
    }

    @Test
    @DisplayName("parseOrNull ignores DRAFT and READY_FOR_SHIPMENT instead of throwing")
    void parseOrNullIgnoresUnknownWorkOrderStatuses() {
        Assertions.assertThat(Enums.parseOrNull(OperationWorkOrderStatus.class, "DRAFT")).isNull();
        Assertions.assertThat(Enums.parseOrNull(OperationWorkOrderStatus.class, "READY_FOR_SHIPMENT")).isNull();
        Assertions.assertThat(Enums.parseOrNull(OperationWorkOrderStatus.class, "")).isNull();
        Assertions.assertThat(Enums.parseOrNull(OperationWorkOrderStatus.class, "  ")).isNull();
        Assertions.assertThat(Enums.parseOrNull(OperationWorkOrderStatus.class, null)).isNull();
    }
}
