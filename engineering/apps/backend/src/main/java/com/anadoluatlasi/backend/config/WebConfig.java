package com.anadoluatlasi.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC yapılandırması.
 *
 * <p>CORS ayarları SecurityConfig'e taşındı (SecurityFilterChain üzerinden).
 * Spring Security aktifken, CORS yapılandırması SecurityFilterChain tarafından yönetilmelidir.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // CORS ayarları artık SecurityConfig.corsConfigurationSource() içinde.
    // WebMvcConfigurer CORS çakışmasını önlemek için burada tanımlanmaz.
}
