package com.ozerler.marble.dto.view;

/**
 * Shared display labels for view DTOs so templates do not walk lazy or nested associations.
 */
public final class ViewLabels {

    public static final String GENERAL_STOCK = "Genel Stok";
    public static final String DEFAULT_STONE_TYPE = "Mermer";
    public static final String DEFAULT_SLAB_DESCRIPTION = "Mermer Plaka";
    public static final String DEFAULT_BLOCK_STONE_TYPE = "Taş";

    private ViewLabels() {
    }

    public static String customerName(PartyView customer) {
        return companyName(customer, GENERAL_STOCK);
    }

    public static String companyName(PartyView customer, String fallback) {
        if (customer == null) {
            return fallback;
        }
        String name = customer.getCompanyName();
        if (name == null || name.isBlank()) {
            return fallback;
        }
        return name;
    }

    public static String displayStoneType(String stoneType, BlockView block, String fallback) {
        if (hasText(stoneType)) {
            return stoneType;
        }
        if (block != null && hasText(block.getStoneType())) {
            return block.getStoneType();
        }
        if (fallback == null) {
            return DEFAULT_STONE_TYPE;
        }
        return fallback;
    }

    public static String displayQuarryName(BlockView block) {
        if (block == null) {
            return "";
        }
        if (hasText(block.getQuarryName())) {
            return block.getQuarryName();
        }
        QuarryRef quarry = block.getQuarry();
        if (quarry != null && hasText(quarry.getName())) {
            return quarry.getName();
        }
        return "";
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
