package com.startup.turkuproje.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// JPA Auditing aktifleştirme - @CreatedDate ve @LastModifiedDate için gerekli
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
