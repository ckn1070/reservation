package com.drlom.reservation.common.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 *
 * <p>- JWT 기반 Stateless 인증 (API)
 *
 * <p>- Basic Auth (Swagger UI)
 *
 * <p>- CSRF 비활성화 (REST API)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  // Swagger UI 경로
  private static final String[] SWAGGER_WHITELIST = {
    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**"
  };

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // Swagger Basic Auth 필터를 Spring Security보다 먼저 실행
  @Bean
  public FilterRegistrationBean<SwaggerBasicAuthFilter> swaggerBasicAuthFilterRegistration(
      SwaggerBasicAuthFilter filter) {
    FilterRegistrationBean<SwaggerBasicAuthFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(filter);
    registration.addUrlPatterns("/swagger-ui.html", "/swagger-ui/*", "/v3/api-docs/*");
    registration.setOrder(-200); // Spring Security(-100)보다 먼저 실행
    return registration;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http
        // CSRF 비활성화 (REST API, JWT 사용)
        .csrf(CsrfConfigurer::disable)

        // Frame Options 비활성화 (H2 Console 사용 시)
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

        // 세션 사용 안 함 (JWT: Stateless)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // URL 별 권한 설정
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(SWAGGER_WHITELIST)
                    .permitAll() // Swagger는 커스텀 필터에서 처리
                    .requestMatchers("/api/auth/**")
                    .permitAll() // 회원가입, 로그인은 public
                    .anyRequest()
                    .authenticated() // 나머지는 인증 필요
            );

    return http.build();
  }
}
