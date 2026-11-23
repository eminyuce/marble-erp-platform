package com.ozerler.marble.dto.view;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ViewLabelsTest {

    @Test
    @DisplayName("customerName falls back to Genel Stok when the party is missing")
    void customerNameUsesGeneralStockFallback() {
        Assertions.assertThat(ViewLabels.customerName(null)).isEqualTo("Genel Stok");
        PartyView blank = new PartyView();
        blank.setCompanyName("  ");
        Assertions.assertThat(ViewLabels.customerName(blank)).isEqualTo("Genel Stok");
        PartyView named = new PartyView();
        named.setCompanyName("Ebat Müşteri");
        Assertions.assertThat(ViewLabels.customerName(named)).isEqualTo("Ebat Müşteri");
    }

    @Test
    @DisplayName("displayStoneType prefers the slab type, then the block, then the fallback")
    void displayStoneTypePrefersSlabThenBlock() {
        BlockView block = new BlockView();
        block.setStoneType("Burdur Bej");
        Assertions.assertThat(ViewLabels.displayStoneType("Afyon Beyaz", block, "Mermer"))
                .isEqualTo("Afyon Beyaz");
        Assertions.assertThat(ViewLabels.displayStoneType(null, block, "Mermer"))
                .isEqualTo("Burdur Bej");
        Assertions.assertThat(ViewLabels.displayStoneType(null, null, "Mermer Plaka"))
                .isEqualTo("Mermer Plaka");
    }

    @Test
    @DisplayName("SlabView labels work when customer and block are missing")
    void slabViewLabelsWithoutCustomerOrBlock() {
        SlabView slab = new SlabView();
        slab.setSlabCode("SLB-FREE");
        slab.setThicknessCm(new BigDecimal("2"));
        slab.setWidthCm(new BigDecimal("100"));
        slab.setLengthCm(new BigDecimal("200"));
        slab.setSurfaceAreaM2(new BigDecimal("2.00"));

        Assertions.assertThat(slab.getCustomerName()).isEqualTo("Genel Stok");
        Assertions.assertThat(slab.getDisplayStoneType()).isEqualTo("Mermer");
        Assertions.assertThat(slab.getPrefillStoneType()).isEqualTo("Mermer Plaka");
        Assertions.assertThat(slab.getFactorySizedOptionLabel())
                .contains("SLB-FREE")
                .contains("Mermer")
                .contains("Genel Stok");
        Assertions.assertThat(slab.getWorkshopSizedOptionLabel())
                .contains("Alan:")
                .contains("2.00");
    }
}
