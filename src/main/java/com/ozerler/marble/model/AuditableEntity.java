package com.ozerler.marble.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Standardized abstract mapped superclass providing audit metadata across all database entities.
 * <p>
 * Includes:
 * <ul>
 *     <li>{@code createdDate} (created_date): Timestamp when the record was initially created.</li>
 *     <li>{@code updatedDate} (updated_date): Timestamp when the record was last modified.</li>
 *     <li>{@code addUserId} (add_user_id): Email of the authenticated user who added the record.</li>
 *     <li>{@code updateUserId} (update_user_id): Email of the authenticated user who last updated the record.</li>
 * </ul>
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    @CreatedBy
    @Column(name = "add_user_id", length = 128)
    private String addUserId;

    @LastModifiedBy
    @Column(name = "update_user_id", length = 128)
    private String updateUserId;

    // Backward-compatibility accessors for existing DTO mappers and controllers
    public LocalDateTime getCreatedAt() {
        return createdDate;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdDate = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedDate;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedDate = updatedAt;
    }
}
