package com.drlom.reservation.identity.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 인증 필터
 *
 * <p>모든 요청에서 Authorization 헤더의 JWT 토큰을 검증하고, 유효한 경우 SecurityContext에 Authentication을 설정합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // 1. Authorization 헤더에서 JWT 토큰 추출
    String token = extractToken(request);

    // 2. 토큰이 있고 유효한 경우 Authentication 설정
    if (token != null && authenticateWithToken(token)) {
      log.debug("JWT 인증 성공: uri={}", request.getRequestURI());
    }

    // 3. 다음 필터로 진행
    filterChain.doFilter(request, response);
  }

  /**
   * Authorization 헤더에서 Bearer 토큰 추출
   *
   * @param request HTTP 요청
   * @return JWT 토큰 (없으면 null)
   */
  private String extractToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }
    return null;
  }

  /**
   * JWT 토큰으로 인증 수행
   *
   * @param token JWT 토큰
   * @return 인증 성공 여부
   */
  private boolean authenticateWithToken(String token) {
    try {
      // 토큰 유효성 검증
      if (!jwtTokenProvider.validateToken(token)) {
        return false;
      }

      // 토큰에서 정보 추출
      Long userId = jwtTokenProvider.getUserIdFromToken(token);
      List<String> roles = jwtTokenProvider.getRolesFromToken(token);

      // 권한 목록 생성
      List<SimpleGrantedAuthority> authorities =
          roles.stream().map(SimpleGrantedAuthority::new).toList();

      // Authentication 객체 생성 (principal = userId)
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(userId, null, authorities);

      // SecurityContext에 설정
      SecurityContextHolder.getContext().setAuthentication(authentication);

      return true;
    } catch (Exception e) {
      log.debug("JWT 인증 실패: {}", e.getMessage());
      SecurityContextHolder.clearContext();
      return false;
    }
  }
}
