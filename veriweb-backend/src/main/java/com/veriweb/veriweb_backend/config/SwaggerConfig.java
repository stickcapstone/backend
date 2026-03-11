package com.veriweb.veriweb_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VeriWeb API")
                        .description("웹 페이지 신뢰도 분석 플랫폼 API")
                        .version("v1.0.0"));
    }
}
