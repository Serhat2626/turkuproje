package com.anadoluatlasi.backend.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tüm entity'ler için ortak alanları barındıran soyut temel sınıf.
 *
 * <p>Sağladığı ortak alanlar:</p>
 * <ul>
 *   <li>{@code id} — UUID primary key, auto-generated</li>
 *   <li>{@code createdAt} — JPA auditing ile otomatik atanan oluşturma zamanı</li>
 *   <li>{@code updatedAt} — JPA auditing ile otomatik atanan güncelleme zamanı</li>
 * </ul>
 *
 * <p>Alt sınıflar {@code @SuperBuilder} kullanmalıdır (Lombok {@code @Builder} yerine).</p>
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onPrePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
