package com.ozerler.marble.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StoneTypeCatalogTest {

    @Test
    void getAll_ContainsStandardMarbleTypes() {
        var all = StoneTypeCatalog.getAll();
        assertThat(all).isNotEmpty();
        assertThat(all).anyMatch(s -> s.code().equals("BEJ") && s.specificGravity().compareTo(new BigDecimal("2.71")) == 0);
        assertThat(all).anyMatch(s -> s.code().equals("TRAVERTEN") && s.specificGravity().compareTo(new BigDecimal("2.45")) == 0);
        assertThat(all).anyMatch(s -> s.code().equals("BEYAZ") && s.specificGravity().compareTo(new BigDecimal("2.70")) == 0);
        assertThat(all).anyMatch(s -> s.code().equals("GRI") && s.specificGravity().compareTo(new BigDecimal("2.75")) == 0);
    }

    @Test
    void findDensityOrDefault_WhenBej_Returns271() {
        BigDecimal density = StoneTypeCatalog.findDensityOrDefault("Burdur Bej", new BigDecimal("2.70"));
        assertThat(density).isEqualByComparingTo("2.71");
    }

    @Test
    void findDensityOrDefault_WhenTravertine_Returns245() {
        BigDecimal density = StoneTypeCatalog.findDensityOrDefault("Denizli Traverten Noce", new BigDecimal("2.70"));
        assertThat(density).isEqualByComparingTo("2.45");
    }

    @Test
    void findDensityOrDefault_WhenUnknown_ReturnsFallback() {
        BigDecimal fallback = new BigDecimal("2.65");
        BigDecimal density = StoneTypeCatalog.findDensityOrDefault("Bilinmeyen Taş", fallback);
        assertThat(density).isEqualTo(fallback);
    }
}
