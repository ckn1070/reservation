package com.drlom.reservation.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI(Swagger) 설정
 *
 * <p>API 문서화를 위한 OpenAPI 3.0 설정
 */
@Configuration
public class OpenApiConfig {

  private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(apiInfo())
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
        .components(
            new Components()
                .addSecuritySchemes(
                    SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Access Token을 입력하세요 (Bearer 접두사 제외)")));
  }

  private Info apiInfo() {
    return new Info()
        .title("Reservation System API")
        .description("공연/이벤트 좌석 예약 시스템 API 문서")
        .version("v1.0.0")
        .contact(new Contact().name("Reservation System").email("contact@example.com"));
  }
}
