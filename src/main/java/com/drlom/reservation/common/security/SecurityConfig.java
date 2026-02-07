package com.drlom.reservation.common.security;

import com.drlom.reservation.identity.infrastructure.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  // Swagger UI 경로
  private static final String[] SWAGGER_WHITELIST = {
    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**"
  };

  // 역할 계층: SUPER_ADMIN > ADMIN > USER
  @Bean
  public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy(
        "ROLE_SUPER_ADMIN > ROLE_ADMIN\nROLE_ADMIN > ROLE_USER");
  }

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

  // JwtAuthenticationFilter가 Spring Bean으로 자동 등록되는 것을 방지
  // (SecurityFilterChain에서 직접 추가하므로 중복 방지)
  @Bean
  public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
      JwtAuthenticationFilter filter) {
    FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(filter);
    registration.setEnabled(false); // 자동 등록 비활성화
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

        // 예외 처리 설정
        .exceptionHandling(
            exceptions ->
                exceptions
                    // 인증 실패 시 (401)
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          response.setStatus(HttpStatus.UNAUTHORIZED.value());
                          response.setContentType("application/json;charset=UTF-8");
                          response
                              .getWriter()
                              .write(
                                  "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\",\"timestamp\":\""
                                      + java.time.LocalDateTime.now()
                                      + "\"}");
                        })
                    // 권한 부족 시 (403) - @PreAuthorize 실패 등
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) -> {
                          response.setStatus(HttpStatus.FORBIDDEN.value());
                          response.setContentType("application/json;charset=UTF-8");
                          response
                              .getWriter()
                              .write(
                                  "{\"code\":\"FORBIDDEN\",\"message\":\"접근 권한이 없습니다\",\"timestamp\":\""
                                      + java.time.LocalDateTime.now()
                                      + "\"}");
                        }))

        // URL 별 권한 설정
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(SWAGGER_WHITELIST)
                    .permitAll() // Swagger는 커스텀 필터에서 처리
                    .requestMatchers("/api/auth/**")
                    .permitAll() // 회원가입, 로그인은 public
                    .anyRequest()
                    .authenticated() // 나머지는 인증 필요
            )

        // JWT 인증 필터 추가 (UsernamePasswordAuthenticationFilter 이전에 실행)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
