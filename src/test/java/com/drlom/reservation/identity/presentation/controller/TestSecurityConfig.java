package com.drlom.reservation.identity.presentation.controller;

import static org.mockito.Mockito.*;

import com.drlom.reservation.identity.infrastructure.security.JwtAuthenticationFilter;
import com.drlom.reservation.identity.infrastructure.security.JwtTokenProvider;
import java.util.Collections;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// 테스트용 보안 설정. @WebMvcTest에서 @PreAuthorize가 동작하도록 Method Security 활성화.
@TestConfiguration
@EnableMethodSecurity
public class TestSecurityConfig {

  @Bean
  @Primary
  public JwtTokenProvider testJwtTokenProvider() {
    JwtTokenProvider mock = mock(JwtTokenProvider.class);
    when(mock.validateToken(anyString())).thenReturn(true);
    when(mock.getUserIdFromToken(anyString())).thenReturn(1L);
    when(mock.getRolesFromToken(anyString())).thenReturn(Collections.emptyList());
    return mock;
  }

  @Bean
  @Primary
  public JwtAuthenticationFilter testJwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
    return new JwtAuthenticationFilter(jwtTokenProvider);
  }

  @Bean
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) {
    try {
      http.csrf(CsrfConfigurer::disable)
          .sessionManagement(
              session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(
              auth ->
                  auth.requestMatchers("/api/auth/**")
                      .permitAll()
                      .anyRequest()
                      .authenticated());
      return http.build();
    } catch (Exception e) {
      throw new IllegalStateException("Security configuration failed", e);
    }
  }
}
