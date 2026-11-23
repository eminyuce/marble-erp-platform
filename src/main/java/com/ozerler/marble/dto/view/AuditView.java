package com.ozerler.marble.dto.view;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * Audit columns shown by the {@code fragments/audit :: auditInfo} fragment.
 */
@Getter
@Setter
public abstract class AuditView {
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String addUserId;
    private String updateUserId;
}
