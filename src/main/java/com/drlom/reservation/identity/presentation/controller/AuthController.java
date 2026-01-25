package com.drlom.reservation.identity.presentation.controller;

import com.drlom.reservation.identity.application.dto.result.LoginResult;
import com.drlom.reservation.identity.application.dto.result.TokenResult;
import com.drlom.reservation.identity.application.dto.result.UserResult;
import com.drlom.reservation.identity.application.usecase.LoginUseCase;
import com.drlom.reservation.identity.application.usecase.LogoutUseCase;
import com.drlom.reservation.identity.application.usecase.RefreshTokenUseCase;
import com.drlom.reservation.identity.application.usecase.SignUpUseCase;
import com.drlom.reservation.identity.presentation.dto.LoginWebRequest;
import com.drlom.reservation.identity.presentation.dto.LoginWebResponse;
import com.drlom.reservation.identity.presentation.dto.LogoutWebRequest;
import com.drlom.reservation.identity.presentation.dto.RefreshTokenWebRequest;
import com.drlom.reservation.identity.presentation.dto.SignUpWebRequest;
import com.drlom.reservation.identity.presentation.dto.SignUpWebResponse;
import com.drlom.reservation.identity.presentation.dto.TokenWebResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 Controller
 *
 * <p>Presentation 계층 (REST API):
 *
 * <p>- POST /api/auth/signup: 회원가입
 *
 * <p>- POST /api/auth/login: 로그인
 *
 * <p>- POST /api/auth/logout: 로그아웃
 *
 * <p>- POST /api/auth/refresh: 토큰 재발급
 *
 * <p>- Web DTO 사용 - Spring Validation - HTTP Status Code 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final SignUpUseCase signUpUseCase;
  private final LoginUseCase loginUseCase;
  private final LogoutUseCase logoutUseCase;
  private final RefreshTokenUseCase refreshTokenUseCase;

  /**
   * 회원가입 API
   *
   * @param request 회원가입 요청
   * @return 회원가입 응답 (사용자 정보)
   */
  @PostMapping("/signup")
  @ResponseStatus(HttpStatus.CREATED)
  public SignUpWebResponse signUp(@RequestBody @Valid SignUpWebRequest request) {
    log.info("회원가입 요청: email={}", request.getEmail());

    // UseCase 실행
    UserResult userResult = signUpUseCase.execute(request.toCommand());

    // Result → WebResponse 변환
    SignUpWebResponse response = SignUpWebResponse.from(userResult);

    log.info("회원가입 성공: userId={}, email={}", response.getId(), response.getEmail());

    return response;
  }

  /**
   * 로그인 API
   *
   * @param request 로그인 요청
   * @return 로그인 응답 (토큰 + 사용자 정보)
   */
  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  public LoginWebResponse login(@RequestBody @Valid LoginWebRequest request) {
    log.info("로그인 요청: email={}", request.getEmail());

    // UseCase 실행
    LoginResult loginResult = loginUseCase.execute(request.toCommand());

    // Result → WebResponse 변환
    LoginWebResponse response = LoginWebResponse.from(loginResult);

    log.info("로그인 성공: userId={}, email={}", response.getUserId(), response.getEmail());

    return response;
  }

  /**
   * 로그아웃 API
   *
   * @param request 로그아웃 요청 (Refresh Token 포함)
   */
  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@RequestBody @Valid LogoutWebRequest request) {
    log.info("로그아웃 요청");

    // UseCase 실행
    logoutUseCase.execute(request.toCommand());

    log.info("로그아웃 완료");
  }

  /**
   * 토큰 재발급 API
   *
   * @param request 토큰 재발급 요청 (Refresh Token 포함)
   * @return 토큰 응답 (새 Access Token + Refresh Token)
   */
  @PostMapping("/refresh")
  @ResponseStatus(HttpStatus.OK)
  public TokenWebResponse refresh(@RequestBody @Valid RefreshTokenWebRequest request) {
    log.info("토큰 재발급 요청");

    // UseCase 실행
    TokenResult tokenResult = refreshTokenUseCase.execute(request.toCommand());

    // Result → WebResponse 변환
    TokenWebResponse response = TokenWebResponse.from(tokenResult);

    log.info("토큰 재발급 성공");

    return response;
  }
}
