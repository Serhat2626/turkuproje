package com.anadoluatlasi.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger yapılandırması.
 * Swagger UI: /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI anadoluAtlasiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Anadolu Atlasi API")
                        .version("0.1.0")
                        .description("Anadolu Atlası - AI destekli çok kanallı içerik üretim platformu REST API")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
