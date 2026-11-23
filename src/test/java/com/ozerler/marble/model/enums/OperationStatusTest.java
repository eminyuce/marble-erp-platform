package com.ozerler.marble.model.enums;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.model.CutOrder;
import com.ozerler.marble.model.ProductionOrder;

class OperationStatusTest {

    @Test
    @DisplayName("labelOf returns Turkish labels for production and cut-order codes")
    void labelOfKnownCodesReturnsTurkishLabels() {
        Assertions.assertThat(OperationStatus.labelOf("PLANNED")).isEqualTo("Planlandı");
        Assertions.assertThat(OperationStatus.labelOf("IN_PROGRESS")).isEqualTo("Devam ediyor");
        Assertions.assertThat(OperationStatus.labelOf("COMPLETED")).isEqualTo("Tamamlandı");
        Assertions.assertThat(OperationStatus.labelOf("CANCELLED")).isEqualTo("İptal");
        Assertions.assertThat(OperationStatus.labelOf("completed")).isEqualTo("Tamamlandı");
    }

    @Test
    @DisplayName("labelOf falls back for blank and unknown codes")
    void labelOfUnknownAndBlank() {
        Assertions.assertThat(OperationStatus.labelOf(null)).isEmpty();
        Assertions.assertThat(OperationStatus.labelOf("")).isEmpty();
        Assertions.assertThat(OperationStatus.labelOf("CUSTOM")).isEqualTo("CUSTOM");
    }

    @Test
    @DisplayName("Entities expose the same Turkish status label used on detail pages")
    void entityStatusLabelMatchesEnum() {
        ProductionOrder production = ProductionOrder.builder().status("IN_PROGRESS").build();
        CutOrder cutOrder = CutOrder.builder().status("COMPLETED").build();

        Assertions.assertThat(production.getStatusLabel()).isEqualTo("Devam ediyor");
        Assertions.assertThat(cutOrder.getStatusLabel()).isEqualTo("Tamamlandı");
    }
}
