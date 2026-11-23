package com.ozerler.marble.model.enums;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.model.CutItem;

class CutItemStatusTest {

    @Test
    @DisplayName("labelOf returns Turkish labels for cut-item codes")
    void labelOfKnownCodesReturnsTurkishLabels() {
        Assertions.assertThat(CutItemStatus.labelOf("READY")).isEqualTo("Hazır");
        Assertions.assertThat(CutItemStatus.labelOf("PACKED")).isEqualTo("Paketlendi");
        Assertions.assertThat(CutItemStatus.labelOf("DELIVERED")).isEqualTo("Teslim edildi");
        Assertions.assertThat(CutItemStatus.labelOf("INSTALLED")).isEqualTo("Monte edildi");
    }

    @Test
    @DisplayName("Cut items expose the Turkish status label used on workshop detail")
    void entityStatusLabelMatchesEnum() {
        CutItem item = CutItem.builder().status("READY").build();
        Assertions.assertThat(item.getStatusLabel()).isEqualTo("Hazır");
        Assertions.assertThat(CutItemStatus.labelOf(null)).isEmpty();
        Assertions.assertThat(CutItemStatus.labelOf("OTHER")).isEqualTo("OTHER");
    }
}
