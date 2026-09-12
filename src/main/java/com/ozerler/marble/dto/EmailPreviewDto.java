package com.ozerler.marble.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO transferring rendered email template preview data to the presentation layer.
 */
@Getter
@Builder
public class EmailPreviewDto {
    private final String templateKey;
    private final String templateName;
    private final String subject;
    private final String html;
    private final String rawSubject;
    private final String rawHtml;
}
