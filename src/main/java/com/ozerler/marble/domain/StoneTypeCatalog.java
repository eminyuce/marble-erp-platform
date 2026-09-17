package com.ozerler.marble.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Mermer ve doğal taş cinsleri ve standart özgül ağırlıkları (ton/m³).
 */
public final class StoneTypeCatalog {

    public record StoneTypeItem(String code, String name, BigDecimal specificGravity, String description) {}

    private static final List<StoneTypeItem> CATALOG = List.of(
            new StoneTypeItem("BEJ", "Burdur / Klasik Bej", new BigDecimal("2.71"), "Standart bej mermer (2.71 t/m³)"),
            new StoneTypeItem("BEYAZ", "Muğla / Afyon Beyaz", new BigDecimal("2.70"), "Saf kristalin beyaz mermer (2.70 t/m³)"),
            new StoneTypeItem("TRAVERTEN", "Denizli Traverten", new BigDecimal("2.45"), "Gözenekli açık/koyu traverten (2.45 t/m³)"),
            new StoneTypeItem("GRI", "Afyon / Tundra Gri", new BigDecimal("2.75"), "Damarlı gri mermer (2.75 t/m³)"),
            new StoneTypeItem("SIYAH", "Toros Siyah", new BigDecimal("2.73"), "Koyu siyah kalsit damarlı (2.73 t/m³)"),
            new StoneTypeItem("ONIKS", "Oniks (Onyx)", new BigDecimal("2.72"), "Yarı saydam kalsit oniks (2.72 t/m³)"),
            new StoneTypeItem("GRANIT", "Doğal Granit", new BigDecimal("2.80"), "Sert magma kökenli granit (2.80 t/m³)"),
            new StoneTypeItem("LIMESTONE", "Limestone / Kireçtaşı", new BigDecimal("2.60"), "Yumuşak sedimanter kireçtaşı (2.60 t/m³)")
    );

    private StoneTypeCatalog() {
    }

    public static List<StoneTypeItem> getAll() {
        return CATALOG;
    }

    public static BigDecimal findDensityOrDefault(String stoneName, BigDecimal fallback) {
        if (stoneName == null || stoneName.isBlank()) {
            return fallback != null ? fallback : new BigDecimal("2.70");
        }
        String normalized = stoneName.trim().toUpperCase();
        for (StoneTypeItem item : CATALOG) {
            if (normalized.contains(item.code()) || normalized.contains(item.name().toUpperCase())) {
                return item.specificGravity();
            }
        }
        return fallback != null ? fallback : new BigDecimal("2.70");
    }

    public static Optional<StoneTypeItem> findByName(String stoneName) {
        if (stoneName == null || stoneName.isBlank()) {
            return Optional.empty();
        }
        String normalized = stoneName.trim().toUpperCase();
        return CATALOG.stream()
                .filter(item -> normalized.contains(item.code()) || normalized.contains(item.name().toUpperCase()))
                .findFirst();
    }
}
