package com.ozerler.marble.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

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
    private String pageKey;

    @JsonProperty("title")
    private String title;

    @JsonProperty("body")
    private String body;

    @JsonProperty("format")
    private String format;

    @JsonProperty("lastUpdated")
    private LocalDate lastUpdated;
}
