package com.ozerler.marble.model.enums;

import com.ozerler.marble.model.CutOrder;
import com.ozerler.marble.model.ProductionOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperationStatusTest {

    @Test
    @DisplayName("labelOf returns Turkish labels for production and cut-order codes")
    void labelOf_knownCodes_returnsTurkishLabels() {
        assertThat(OperationStatus.labelOf("PLANNED")).isEqualTo("Planlandı");
        assertThat(OperationStatus.labelOf("IN_PROGRESS")).isEqualTo("Devam ediyor");
        assertThat(OperationStatus.labelOf("COMPLETED")).isEqualTo("Tamamlandı");
        assertThat(OperationStatus.labelOf("CANCELLED")).isEqualTo("İptal");
        assertThat(OperationStatus.labelOf("completed")).isEqualTo("Tamamlandı");
    }

    @Test
    @DisplayName("labelOf falls back for blank and unknown codes")
    void labelOf_unknownAndBlank() {
        assertThat(OperationStatus.labelOf(null)).isEmpty();
        assertThat(OperationStatus.labelOf("")).isEmpty();
        assertThat(OperationStatus.labelOf("CUSTOM")).isEqualTo("CUSTOM");
    }

    @Test
    @DisplayName("Entities expose the same Turkish status label used on detail pages")
    void entityStatusLabel_matchesEnum() {
        ProductionOrder production = ProductionOrder.builder().status("IN_PROGRESS").build();
        CutOrder cutOrder = CutOrder.builder().status("COMPLETED").build();

        assertThat(production.getStatusLabel()).isEqualTo("Devam ediyor");
        assertThat(cutOrder.getStatusLabel()).isEqualTo("Tamamlandı");
    }
}
