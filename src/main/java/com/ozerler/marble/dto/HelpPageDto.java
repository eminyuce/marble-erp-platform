package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

/**
 * Page-specific help payload served to the slide-over panel.
 *
 * <pre>
 * {
 *   "pageKey": "change-password",
 *   "title": "Nasıl kullanılır?",
 *   "body": "&lt;ul&gt;&lt;li&gt;...&lt;/li&gt;&lt;/ul&gt;",
 *   "format": "html",
 *   "lastUpdated": "2026-09-14"
 * }
 * </pre>
 */
@Value
@Builder
public class HelpPageDto {

    @JsonProperty("pageKey")
    String pageKey;

    @JsonProperty("title")
    String title;

    @JsonProperty("body")
    String body;

    @JsonProperty("format")
    String format;

    @JsonProperty("lastUpdated")
    LocalDate lastUpdated;
}
