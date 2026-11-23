package com.ozerler.marble.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Email template as the admin screens see it. Property names match the entity so templates keep their paths.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateDto {
    private Long id;
    private String templateKey;
    private String templateName;
    private String subject;
    private String bodyHtml;
    private String placeholders;
    private String category;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private LocalDateTime updatedAt;
    private String addUserId;
    private String updateUserId;
}
