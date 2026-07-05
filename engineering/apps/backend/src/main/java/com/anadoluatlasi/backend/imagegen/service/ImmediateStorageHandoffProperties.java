package com.anadoluatlasi.backend.imagegen.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * FP-010 Task 04: Immediate storage handoff configuration.
 *
 * <p>Controls whether successful generation triggers immediate
 * retrieval → validation → private storage handoff.</p>
 *
 * <p>Disabled by default — generation creates assets without storage.
 * When enabled, transient provider URL flows into controlled handoff pipeline.</p>
 *
 * <p>No secrets in this config. Provider URL remains transient method parameter.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "imagegen.storage.immediate-handoff")
public class ImmediateStorageHandoffProperties {

    /** Whether immediate storage handoff after generation is enabled. Default: false. */
    private boolean enabled = false;
}
