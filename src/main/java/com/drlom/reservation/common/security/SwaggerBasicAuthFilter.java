package com.drlom.reservation.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Swagger UI Basic Auth 필터
 *
 * <p>Spring Security와 독립적으로 Swagger 경로에 Basic Auth 적용
 */
@NullMarked
@Component
public class SwaggerBasicAuthFilter extends OncePerRequestFilter {

  private static final String SWAGGER_PATH_PREFIX = "/swagger-ui";
  private static final String API_DOCS_PATH_PREFIX = "/v3/api-docs";
  private static final String REALM = "Swagger API Documentation";

  @Value("${swagger.auth.username:swagger-admin}")
  private String username;

  @Value("${swagger.auth.password:swagger-secret-123}")
  private String password;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();

    // Swagger 경로가 아니면 통과
    if (!isSwaggerPath(path)) {
      filterChain.doFilter(request, response);
      return;
    }

    // Basic Auth 검증
    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Basic ")) {
      sendUnauthorized(response);
      return;
    }

    String base64Credentials = authHeader.substring("Basic ".length());
    String credentials =
        new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
    String[] parts = credentials.split(":", 2);

    if (parts.length != 2 || !isValidCredentials(parts[0], parts[1])) {
      sendUnauthorized(response);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private boolean isSwaggerPath(String path) {
    return path.startsWith(SWAGGER_PATH_PREFIX)
        || path.startsWith(API_DOCS_PATH_PREFIX)
        || path.equals("/swagger-ui.html");
  }

  private boolean isValidCredentials(String inputUsername, String inputPassword) {
    return username.equals(inputUsername) && password.equals(inputPassword);
  }

  private void sendUnauthorized(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setHeader("WWW-Authenticate", "Basic realm=\"" + REALM + "\"");
    response.getWriter().flush();
  }
}
