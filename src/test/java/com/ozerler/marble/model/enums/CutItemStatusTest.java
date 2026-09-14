package com.ozerler.marble.model.enums;

import com.ozerler.marble.model.CutItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CutItemStatusTest {

    @Test
    @DisplayName("labelOf returns Turkish labels for cut-item codes")
    void labelOf_knownCodes_returnsTurkishLabels() {
        assertThat(CutItemStatus.labelOf("READY")).isEqualTo("Hazır");
        assertThat(CutItemStatus.labelOf("PACKED")).isEqualTo("Paketlendi");
        assertThat(CutItemStatus.labelOf("DELIVERED")).isEqualTo("Teslim edildi");
        assertThat(CutItemStatus.labelOf("INSTALLED")).isEqualTo("Monte edildi");
    }

    @Test
    @DisplayName("Cut items expose the Turkish status label used on workshop detail")
    void entityStatusLabel_matchesEnum() {
        CutItem item = CutItem.builder().status("READY").build();
        assertThat(item.getStatusLabel()).isEqualTo("Hazır");
        assertThat(CutItemStatus.labelOf(null)).isEmpty();
        assertThat(CutItemStatus.labelOf("OTHER")).isEqualTo("OTHER");
    }
}
