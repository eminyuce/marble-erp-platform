package com.ozerler.marble.service;

import com.ozerler.marble.dto.HelpPageDto;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads every help HTML file from {@code classpath:help/*.html} once at startup
 * and serves pages from that in-memory cache.
 */
@Slf4j
@Component
public class HelpContentCatalog {

    private static final String HELP_FILES_PATTERN = "classpath:help/*.html";
    private static final String HTML_SUFFIX = ".html";
    private static final LocalDate FALLBACK_UPDATED = LocalDate.of(2026, 9, 14);
    private static final Pattern PAGE_KEY_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");
    private static final Pattern TITLE_ATTR = Pattern.compile("data-title=\"([^\"]+)\"");
    private static final Pattern UPDATED_ATTR = Pattern.compile("data-updated=\"([^\"]+)\"");
    private static final Pattern INNER_HTML = Pattern.compile(
            "(?s)<section[^>]*>(.*)</section>", Pattern.CASE_INSENSITIVE);

    private static final List<ActionPathMapping> ACTION_PATH_MAPPINGS = List.of(
            new ActionPathMapping(Pattern.compile("^/blocks/[0-9]+/move$"), "block-move"),
            new ActionPathMapping(Pattern.compile("^/blocks/[0-9]+/transfer-to-factory$"), "block-transfer"),
            new ActionPathMapping(Pattern.compile("^/blocks/[0-9]+/photos(/[0-9]+)?$"), "block-photos"),
            new ActionPathMapping(Pattern.compile("^/admin/settings/templates/[0-9]+/preview$"), "email-preview")
    );

    private static final List<PathMapping> PATH_MAPPINGS = List.of(
                    new PathMapping("/account/change-password", "change-password"),
                    new PathMapping("/admin/dashboard/systemhealth", "system-health"),
                    new PathMapping("/production/tablet", "production"),
                    new PathMapping("/production/pallets", "production"),
                    new PathMapping("/production/polish", "production"),
                    new PathMapping("/production/slabs", "slabs"),
                    new PathMapping("/admin/definitions", "definitions"),
                    new PathMapping("/admin/deployment", "deployment"),
                    new PathMapping("/admin/settings", "settings"),
                    new PathMapping("/admin/users", "users"),
                    new PathMapping("/admin/dashboard", "dashboard"),
                    new PathMapping("/factory", "production"),
                    new PathMapping("/production", "production"),
                    new PathMapping("/workshop", "workshop"),
                    new PathMapping("/sites", "projects"),
                    new PathMapping("/projects", "projects"),
                    new PathMapping("/procurement", "procurement"),
                    new PathMapping("/sales", "sales"),
                    new PathMapping("/quarry", "blocks"),
                    new PathMapping("/blocks", "blocks"),
                    new PathMapping("/genealogy", "genealogy"),
                    new PathMapping("/reports", "reports"),
                    new PathMapping("/cost-analysis", "costs"),
                    new PathMapping("/costs", "costs"),
                    new PathMapping("/expenses", "expenses"),
                    new PathMapping("/machines/fuel", "machine-fuel"),
                    new PathMapping("/machines", "definitions")
            ).stream()
            .sorted(Comparator.comparingInt((PathMapping mapping) -> mapping.path().length()).reversed())
            .toList();

    private final ResourcePatternResolver resourcePatternResolver;
    private Map<String, HelpPageDto> pagesByKey = Map.of();

    public HelpContentCatalog(ResourceLoader resourceLoader) {
        this.resourcePatternResolver = resourceLoader instanceof ResourcePatternResolver resolver
                ? resolver
                : new PathMatchingResourcePatternResolver(resourceLoader);
    }

    @PostConstruct
    void loadHelpPages() {
        Map<String, HelpPageDto> loaded = new LinkedHashMap<>();
        for (Resource resource : listHelpResources()) {
            String filename = resource.getFilename();
            if (filename == null || !filename.endsWith(HTML_SUFFIX)) {
                continue;
            }
            String pageKey = filename.substring(0, filename.length() - HTML_SUFFIX.length());
            if (!PAGE_KEY_PATTERN.matcher(pageKey).matches()) {
                log.warn("Skipping help file with unsafe name: {}", filename);
                continue;
            }
            readHelpResource(resource, filename).ifPresent(html -> loaded.put(pageKey, toDto(pageKey, html)));
        }
        if (loaded.isEmpty()) {
            throw new IllegalStateException("No help HTML files found on " + HELP_FILES_PATTERN);
        }
        this.pagesByKey = Map.copyOf(loaded);
        log.info("Cached {} help pages from classpath", loaded.size());
    }

    public Optional<HelpPageDto> findByPageKey(String pageKey) {
        if (pageKey == null || pageKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(pagesByKey.get(pageKey));
    }

    public Optional<String> resolvePageKey(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return Optional.empty();
        }
        String path = stripQuery(requestUri);
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        for (ActionPathMapping mapping : ACTION_PATH_MAPPINGS) {
            if (mapping.pattern().matcher(path).matches()) {
                return Optional.of(mapping.pageKey());
            }
        }
        for (PathMapping mapping : PATH_MAPPINGS) {
            if (path.equals(mapping.path()) || path.startsWith(mapping.path() + "/")) {
                return Optional.of(mapping.pageKey());
            }
        }
        return Optional.empty();
    }

    private Resource[] listHelpResources() {
        try {
            return resourcePatternResolver.getResources(HELP_FILES_PATTERN);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to scan help HTML files on " + HELP_FILES_PATTERN, ex);
        }
    }

    private Optional<String> readHelpResource(Resource resource, String filename) {
        try (InputStream input = resource.getInputStream()) {
            return Optional.of(StreamUtils.copyToString(input, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            log.error("Failed to read help file {}", filename, ex);
            return Optional.empty();
        }
    }

    private static HelpPageDto toDto(String pageKey, String html) {
        String title = firstGroup(TITLE_ATTR, html).orElse(pageKey);
        LocalDate lastUpdated = firstGroup(UPDATED_ATTR, html)
                .map(HelpContentCatalog::parseDate)
                .orElse(FALLBACK_UPDATED);
        String body = firstGroup(INNER_HTML, html).orElse(html).trim();
        return HelpPageDto.builder()
                .pageKey(pageKey)
                .title(title)
                .body(body)
                .format("html")
                .lastUpdated(lastUpdated)
                .build();
    }

    private static Optional<String> firstGroup(Pattern pattern, String html) {
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return Optional.of(matcher.group(1).trim());
        }
        return Optional.empty();
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
            return FALLBACK_UPDATED;
        }
    }

    private static String stripQuery(String requestUri) {
        int queryIndex = requestUri.indexOf('?');
        return queryIndex >= 0 ? requestUri.substring(0, queryIndex) : requestUri;
    }

    private record PathMapping(String path, String pageKey) {
    }

    private record ActionPathMapping(Pattern pattern, String pageKey) {
    }
}
