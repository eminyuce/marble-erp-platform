package com.ozerler.marble.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class EnumLocalizationTest {

    private static final Class<?>[] ENUMS = {
            BlockStatus.class,
            BusinessUnit.class,
            ChamferStatus.class,
            ExpenseCategory.class,
            ExpenseType.class,
            FactoryProcessType.class,
            FactoryWorkOrderStatus.class,
            MachineType.class,
            MaterialLotStatus.class,
            PalletStatus.class,
            ProcessType.class,
            ProductForm.class,
            QualityGrade.class,
            QuantityUnit.class,
            ShipmentDeliveryStatus.class,
            StockLocationType.class,
            SupplyRoute.class,
            WorkshopProcessType.class,
            WorkshopReceiptSource.class,
            WorkshopWorkPurpose.class,
            ConsumptionType.class,
            OperationStatus.class,
            SlabStatus.class
    };

    @Test
    @DisplayName("every operational enum exposes a Turkish label instead of its message key")
    void labelsAreLocalized() throws Exception {
        for (Class<?> type : ENUMS) {
            Method getLabel = type.getMethod("getLabel");
            for (Object constant : type.getEnumConstants()) {
                String label = (String) getLabel.invoke(constant);
                String name = ((Enum<?>) constant).name();
                assertThat(label)
                        .as("%s.%s", type.getSimpleName(), name)
                        .isNotBlank()
                        .doesNotStartWith("enum.");
                if (name.contains("_")) {
                    assertThat(label)
                            .as("%s.%s should not show the raw code", type.getSimpleName(), name)
                            .isNotEqualTo(name);
                }
            }
        }
        assertThat(ProcessType.ST.getLabel()).isEqualTo("ST");
        assertThat(ProcessType.GANGSAW.getLabel()).isEqualTo("Katrak");
        assertThat(QualityGrade.MOLOZ.getLabel()).isEqualTo("Moloz");
        assertThat(StockLocationType.DISPATCH_YARD.getLabel()).isEqualTo("Stok Sahası");
        assertThat(FactoryProcessType.ST_CUTTING.getLabel()).contains("ST");
        assertThat(FactoryProcessType.GANGSAW_CUTTING.getLabel()).contains("Katrak");
    }
}
