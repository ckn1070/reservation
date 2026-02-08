package com.drlom.reservation.booking.presentation.controller;

import static org.mockito.Mockito.*;

import com.drlom.reservation.identity.infrastructure.security.JwtAuthenticationFilter;
import com.drlom.reservation.identity.infrastructure.security.JwtTokenProvider;
import java.util.Collections;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/** 테스트용 보안 설정. @WebMvcTest에서 @PreAuthorize가 동작하도록 Method Security 활성화. */
@TestConfiguration
@EnableMethodSecurity
public class TestSecurityConfig {

  @Bean
  public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy(
        "ROLE_SUPER_ADMIN > ROLE_ADMIN\nROLE_ADMIN > ROLE_USER");
  }

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
}
