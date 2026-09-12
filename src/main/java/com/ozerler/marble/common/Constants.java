package com.ozerler.marble.common;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Global application constants for system defaults, configuration keys,
 * header names, error codes, view paths, and business metrics across
 * the Marble ERP platform.
 */
public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    // =========================================================================
    // 1. System & Application Defaults
    // =========================================================================
    /** Header name for tracing requests across distributed layers. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    /** Default application name used in logs and health reports. */
    public static final String DEFAULT_APP_NAME = "marble-erp-platform";

    /** Default local hostname. */
    public static final String DEFAULT_HOST_LOCALHOST = "localhost";

    /** Default stone type for marble products. */
    public static final String DEFAULT_STONE_TYPE = "Dogal Mermer";

    /** Default product identifier for standard marble output. */
    public static final String DEFAULT_IDENTIFIER = "MAMUL-STANDARD";

    /** Default location name for general factory processing. */
    public static final String DEFAULT_LOCATION_NAME = "Genel";

    /** Default target location for workshop cuts. */
    public static final String DEFAULT_TARGET_LOCATION = "Genel";

    /** Default edge finish specification for fabricated marble items. */
    public static final String DEFAULT_EDGE_FINISH = "PAHLI_CILALI";


    // =========================================================================
    // 2. Security & Auth Roles
    // =========================================================================
    /** Role identifier for system administrators. */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /** Role identifier for standard authenticated users. */
    public static final String ROLE_USER = "ROLE_USER";

    /** Role identifier for executive/managerial view access. */
    public static final String ROLE_EXECUTIVE = "ROLE_EXECUTIVE";

    /** Dynamic setting key to enable or disable global rate limiting. */
    public static final String SETTING_KEY_RATE_LIMITING = "security.rate_limiting.enabled";


    // =========================================================================
    // 3. HTTP Headers & Request Attributes
    // =========================================================================
    /** Reverse-proxy client IP header (e.g. from AWS ALB, Nginx, Cloudflare). */
    public static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    /** Standard Nginx real IP header. */
    public static final String REAL_IP_HEADER = "X-Real-IP";

    /** AJAX request indicator header sent by browsers and fetch/XHR libraries. */
    public static final String REQUESTED_WITH_HEADER = "X-Requested-With";

    /** Value of XMLHttpRequest for AJAX detection. */
    public static final String XML_HTTP_REQUEST = "XMLHttpRequest";

    /** HTTP Accept header name. */
    public static final String HEADER_ACCEPT = "Accept";

    /** HTMX request indicator header. */
    public static final String HEADER_HX_REQUEST = "HX-Request";


    // =========================================================================
    // 4. MDC Logging Context Keys
    // =========================================================================
    /** MDC key storing correlation id. */
    public static final String MDC_CORRELATION_ID = "correlationId";

    /** MDC key storing authenticated username or anonymous indicator. */
    public static final String MDC_USER_ID = "userId";

    /** MDC key storing resolved client IP. */
    public static final String MDC_CLIENT_IP = "clientIp";

    /** MDC key storing incoming HTTP method (GET, POST, etc.). */
    public static final String MDC_HTTP_METHOD = "httpMethod";

    /** MDC key storing requested URI path. */
    public static final String MDC_REQUEST_URI = "requestUri";


    // =========================================================================
    // 5. Cache Names
    // =========================================================================
    /** Cache name for dynamic system settings. */
    public static final String CACHE_SETTINGS = "settings";

    /** Cache name for rendered and active email templates. */
    public static final String CACHE_EMAIL_TEMPLATES = "emailTemplates";

    /** Cache name for quarry reference data. */
    public static final String CACHE_QUARRIES = "quarries";

    /** Cache name for cost center lookup records. */
    public static final String CACHE_COST_CENTERS = "costCenters";


    // =========================================================================
    // 6. Async Task Executor Configuration
    // =========================================================================
    /** Bean name for the global search asynchronous task executor. */
    public static final String SEARCH_EXECUTOR = "searchTaskExecutor";

    /** Core pool size for parallel search executor threads. */
    public static final int SEARCH_CORE_POOL_SIZE = 4;

    /** Maximum pool size for parallel search executor threads. */
    public static final int SEARCH_MAX_POOL_SIZE = 13;

    /** Queue capacity for queued search tasks. */
    public static final int SEARCH_QUEUE_CAPACITY = 32;

    /** Graceful shutdown timeout in seconds for background search executor. */
    public static final int SEARCH_SHUTDOWN_SECONDS = 10;

    /** Thread name prefix for asynchronous global search executor threads. */
    public static final String THREAD_PREFIX_GLOBAL_SEARCH = "global-search-";


    // =========================================================================
    // 7. Pagination Defaults
    // =========================================================================
    /** Default page size for remote Tabulator grids and service queries. */
    public static final int DEFAULT_PAGE_SIZE = 10;


    // =========================================================================
    // 8. Production, Workshop & Barcode Metrics
    // =========================================================================
    /** Default width and height (250x250 px) for generated QR codes. */
    public static final int DEFAULT_QR_CODE_SIZE = 250;

    /** Conversion divisor from square centimeters (cm2) to square meters (m2). */
    public static final BigDecimal SQUARE_CENTIMETERS_PER_SQUARE_METER = new BigDecimal("10000");

    /** Default standard slab thickness in centimeters. */
    public static final BigDecimal DEFAULT_THICKNESS_CM = new BigDecimal("2.0");

    /** Default duration in hours for standard gangsaw cutting operation. */
    public static final BigDecimal DEFAULT_DURATION_HOURS = new BigDecimal("8.0");

    /** Surcharge coefficient applied to scrap cost burdens. */
    public static final BigDecimal SCRAP_COST_IMPACT_RATE = new BigDecimal("0.10");

    /** Overhead multiplier for custom workshop fabrication processes (15%). */
    public static final BigDecimal WORKSHOP_OVERHEAD_FACTOR = new BigDecimal("1.15");

    /** Standard decimal scale for area computations (square meters). */
    public static final int AREA_SCALE = 4;

    /** Standard decimal scale for currency and monetary costs. */
    public static final int COST_SCALE = 2;


    // =========================================================================
    // 9. Cost Accounting & Pricing Defaults
    // =========================================================================
    /** Standard benchmark total cost per square meter (TL/m2). */
    public static final BigDecimal DEFAULT_STANDARD_COST_PER_M2 = new BigDecimal("1365.00");

    /** Default raw block extraction cost allocation per m2. */
    public static final BigDecimal DEFAULT_RAW_BLOCK_COST_M2 = new BigDecimal("820.00");

    /** Default factory gangsaw primary production cost per m2. */
    public static final BigDecimal DEFAULT_FACTORY_PRODUCTION_M2 = new BigDecimal("210.00");

    /** Default secondary workshop cutting and fabrication cost per m2. */
    public static final BigDecimal DEFAULT_WORKSHOP_FABRICATION_M2 = new BigDecimal("165.00");

    /** Default waste and scrap allocation burden per m2. */
    public static final BigDecimal DEFAULT_SCRAP_BURDEN_M2 = new BigDecimal("95.00");

    /** Default internal logistics and transport cost per m2. */
    public static final BigDecimal DEFAULT_LOGISTICS_M2 = new BigDecimal("45.00");

    /** Default general factory overhead allocation per m2. */
    public static final BigDecimal DEFAULT_GENERAL_OVERHEAD_M2 = new BigDecimal("30.00");

    /** Default targeted profit margin percentage (30.00%). */
    public static final BigDecimal DEFAULT_TARGET_MARGIN_PCT = new BigDecimal("30.00");

    /** Minimum permissible profit margin percentage threshold (22.0%). */
    public static final BigDecimal MINIMUM_ACCEPTABLE_MARGIN_PCT = new BigDecimal("22.0");

    /** Divisor constant for calculating percentage values (100). */
    public static final BigDecimal PERCENT_DIVISOR = new BigDecimal("100");

    /** Decimal scale for display and storage of currency amounts. */
    public static final int CURRENCY_SCALE = 2;

    /** Decimal scale for percentage ratios. */
    public static final int PERCENT_SCALE = 1;

    /** Internal calculation scale for intermediate financial math. */
    public static final int CALCULATION_SCALE = 4;

    /** Decimal scale for final pricing simulation outputs. */
    public static final int RESULT_SCALE = 2;


    // =========================================================================
    // 10. Entity & Lifecycle Statuses
    // =========================================================================
    /** Active status for general entities. */
    public static final String STATUS_ACTIVE = "ACTIVE";

    /** Completed status for work orders, cut orders, and productions. */
    public static final String STATUS_COMPLETED = "COMPLETED";

    /** In-progress status for currently operating orders. */
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    /** Planned status for scheduled but not yet started activities. */
    public static final String STATUS_PLANNED = "PLANNED";

    /** Ready status for fabricated items ready for shipment or inspection. */
    public static final String STATUS_READY = "READY";


    // =========================================================================
    // 11. Genealogy & Quality Warning Thresholds
    // =========================================================================
    /** Threshold for crack level above which a critical risk warning is issued. */
    public static final int CRITICAL_CRACK_LEVEL_THRESHOLD = 1;

    /** Warning message displayed when a block or slab exceeds the crack threshold. */
    public static final String CRITICAL_CRACK_WARNING = "Kritik Seviye Ic Catlak Riski Tespit Edildi!";


    // =========================================================================
    // 12. Global Search Defaults
    // =========================================================================
    /** Maximum number of hits returned per searchable entity type. */
    public static final int SEARCH_PER_TYPE_LIMIT = 5;

    /** Minimum character length required to trigger autocomplete search. */
    public static final int SEARCH_MIN_QUERY_LENGTH = 2;


    // =========================================================================
    // 13. Admin & View Template Paths
    // =========================================================================
    /** Thymeleaf view path for user create/edit form. */
    public static final String VIEW_USER_FORM = "admin/users/form";

    /** Thymeleaf view path for user password reset page. */
    public static final String VIEW_USER_RESET_PASSWORD = "admin/users/reset-password";


    // =========================================================================
    // 14. Error & Validation Messages
    // =========================================================================
    public static final String ERROR_BLOCK_NOT_FOUND = "error.block.not_found";
    public static final String ERROR_CUT_ORDER_NOT_FOUND = "error.cut_order.not_found";
    public static final String ERROR_SLAB_NOT_FOUND = "error.slab.not_found";
    public static final String ERROR_PROJECT_NOT_FOUND = "error.project.not_found";
    public static final String ERROR_LOCATION_NOT_FOUND = "error.location.not_found";
    public static final String ERROR_COST_CENTER_NOT_FOUND = "error.cost_center.not_found";
    public static final String ERROR_USER_NOT_FOUND = "error.user.not_found";
    public static final String ERROR_EMPTY_FILE = "error.file.empty";
    public static final String ERROR_INVALID_PATH = "error.file.invalid_path";
    public static final String ERROR_GENERIC_INTERNAL = "An unexpected error occurred";
    public static final String ERROR_VALIDATION_FAILED = "Validation failed";
    public static final String ERROR_ACCESS_DENIED = "Access denied";
    public static final String ERROR_INVALID_VALUE = "Invalid value";


    // =========================================================================
    // 15. HTML Error Page Presentation Defaults & Tones
    // =========================================================================
    public static final String ERROR_NO_MESSAGE_AVAILABLE = "No message available";
    public static final String ERROR_UNKNOWN_PATH = "/";
    public static final String DEFAULT_HTTP_METHOD = "GET";
    public static final String ERROR_TONE_CLIENT = "client";
    public static final String ERROR_TONE_FORBIDDEN = "forbidden";
    public static final String ERROR_TONE_MISSING = "missing";
    public static final String ERROR_TONE_SERVER = "server";


    // =========================================================================
    // 16. Backend Response Status Codes
    // =========================================================================
    /** Success code indicating no errors occurred during service processing. */
    public static final String NO_ERR = "0";

    /** Fatal error code for uncaught or severe exceptions. */
    public static final String ERR_FATAL = "3399";

    /** Bad request error code for invalid arguments or validation failure. */
    public static final String ERR_BAD_REQUEST = "4000";

    /** Resource not found error code. */
    public static final String ERR_NOT_FOUND = "4004";


    // =========================================================================
    // 17. File Upload & Download Defaults
    // =========================================================================
    /** Default local filesystem directory name for stored user uploads. */
    public static final String DEFAULT_UPLOADS_DIRECTORY = "uploads";

    /** Public URL prefix serving uploaded media assets. */
    public static final String UPLOADS_URL_PREFIX = "/uploads/";

    /** Default fallback basename for exported CSV or Excel files. */
    public static final String FALLBACK_DOWNLOAD_FILENAME = "indirilen";


    // =========================================================================
    // 18. Locales & Text/Date Formatting
    // =========================================================================
    /** Turkish language Locale instance ("tr"). */
    public static final Locale LOCALE_TR = Locale.forLanguageTag("tr");

    /** Standard long Turkish date-time format pattern. */
    public static final String DATE_TIME_FORMAT_TURKISH_LONG = "EEEE, d MMMM yyyy HH:mm";

    /** Standard ISO-like date-time format pattern without seconds. */
    public static final String DATE_TIME_FORMAT_YMD_HM = "yyyy-MM-dd HH:mm";

    /** Default display string for zero or negative uptime. */
    public static final String DEFAULT_UPTIME_FORMAT = "0 sa 00 dk 00 sn";

    /** Standard template placeholder pattern matching {{key}} or ${key}. */
    public static final Pattern PATTERN_TEMPLATE_PLACEHOLDER = Pattern.compile(
            "\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*\\}\\}|\\$\\{\\s*([a-zA-Z0-9_.-]+)\\s*\\}"
    );

    /** Advanced placeholder pattern matching {{{raw}}}, {{escaped}}, or ${key}. */
    public static final Pattern PATTERN_ADVANCED_PLACEHOLDER = Pattern.compile(
            "(\\{\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*\\}\\}\\})|(\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*\\}\\})|(\\$\\{\\s*([a-zA-Z0-9_.-]+)\\s*\\})"
    );


    // =========================================================================
    // 19. Asynchronous Logging Subsystem Defaults
    // =========================================================================
    /** Default queue capacity for asynchronous log event buffer. */
    public static final int ASYNC_LOGGING_QUEUE_CAPACITY = 8192;

    /** High-water mark percentage at which low priority log events are dropped. */
    public static final int ASYNC_LOGGING_HIGH_WATER_MARK_PERCENT = 90;

    /** Default batch size for flushing log events to disk appenders. */
    public static final int ASYNC_LOGGING_BATCH_SIZE = 256;

    /** Drain timeout in milliseconds on JVM shutdown. */
    public static final long ASYNC_LOGGING_DRAIN_TIMEOUT_MILLIS = 5_000L;

    /** Queue poll timeout in milliseconds for background worker loop. */
    public static final long ASYNC_LOGGING_POLL_TIMEOUT_MILLIS = 100L;

    /** Flag indicating whether caller data (stack frame/line numbers) is extracted. */
    public static final boolean ASYNC_LOGGING_INCLUDE_CALLER_DATA = false;


    // =========================================================================
    // 20. CORS & Cross-Origin Defaults
    // =========================================================================
    /** Default standard HTTP methods permitted across cross-origin requests. */
    public static final String[] ALLOWED_CORS_METHODS = {
            "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    };
}
