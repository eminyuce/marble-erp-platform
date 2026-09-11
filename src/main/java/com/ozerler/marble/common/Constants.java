package com.ozerler.marble.common;

/**
 * Global application constants for system defaults, configuration keys,
 * header names, and common labels across the Marble ERP platform.
 */
public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    // System & Logging Headers
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String DEFAULT_APP_NAME = "marble-erp-platform";

    // Common Status Values
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_PLANNED = "PLANNED";
    public static final String STATUS_READY = "READY";

    // System Defaults & Configurations
    public static final String DEFAULT_HOST_LOCALHOST = "localhost";
    public static final String DEFAULT_EDGE_FINISH = "PAHLI_CILALI";
    public static final String DEFAULT_LOCATION_NAME = "Genel";
    public static final String DEFAULT_STONE_TYPE = "Doğal Mermer";
    public static final String DEFAULT_UPLOADS_DIRECTORY = "uploads";
    public static final String UPLOADS_URL_PREFIX = "/uploads/";

    // Security & Auth Roles
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_EXECUTIVE = "ROLE_EXECUTIVE";

    // Error & Validation Messages
    public static final String ERROR_BLOCK_NOT_FOUND = "Blok bulunamadı: ";
    public static final String ERROR_CUT_ORDER_NOT_FOUND = "Kesim iş emri bulunamadı: ";
    public static final String ERROR_SLAB_NOT_FOUND = "Kaynak plaka bulunamadı: ";
    public static final String ERROR_PROJECT_NOT_FOUND = "Proje bulunamadı: ";
    public static final String ERROR_LOCATION_NOT_FOUND = "Mahal bulunamadı: ";
    public static final String ERROR_COST_CENTER_NOT_FOUND = "Masraf merkezi bulunamadı: ";
    public static final String ERROR_USER_NOT_FOUND = "Kullanıcı bulunamadı: ";
    public static final String ERROR_EMPTY_FILE = "Boş dosya kaydedilemez";
    public static final String ERROR_INVALID_PATH = "Geçersiz dosya yolu tespit edildi: ";
}
