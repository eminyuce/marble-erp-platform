package com.ozerler.marble.domain;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class StoneTypeCatalogTest {

    @Test
    void getAllContainsStandardMarbleTypes() {
        var all = StoneTypeCatalog.getAll();
        Assertions.assertThat(all).isNotEmpty();
        Assertions.assertThat(all).anyMatch(s -> s.code().equals("BEJ") && s.specificGravity().compareTo(new BigDecimal("2.71")) == 0);
        Assertions.assertThat(all).anyMatch(s -> s.code().equals("TRAVERTEN") && s.specificGravity().compareTo(new BigDecimal("2.45")) == 0);
        Assertions.assertThat(all).anyMatch(s -> s.code().equals("BEYAZ") && s.specificGravity().compareTo(new BigDecimal("2.70")) == 0);
        Assertions.assertThat(all).anyMatch(s -> s.code().equals("GRI") && s.specificGravity().compareTo(new BigDecimal("2.75")) == 0);
    }

    @Test
    void findDensityOrDefaultWhenBejReturns271() {
        BigDecimal density = StoneTypeCatalog.findDensityOrDefault("Burdur Bej", new BigDecimal("2.70"));
        Assertions.assertThat(density).isEqualByComparingTo("2.71");
    }

    @Test
    void findDensityOrDefaultWhenTravertineReturns245() {
        BigDecimal density = StoneTypeCatalog.findDensityOrDefault("Denizli Traverten Noce", new BigDecimal("2.70"));
        Assertions.assertThat(density).isEqualByComparingTo("2.45");
    }

    @Test
    void findDensityOrDefaultWhenUnknownReturnsFallback() {
        BigDecimal fallback = new BigDecimal("2.65");
        BigDecimal density = StoneTypeCatalog.findDensityOrDefault("Bilinmeyen Taş", fallback);
        Assertions.assertThat(density).isEqualTo(fallback);
    }
}
