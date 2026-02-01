package com.drlom.reservation.identity.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.RefreshTokenCommand;
import com.drlom.reservation.identity.application.dto.result.TokenResult;
import com.drlom.reservation.identity.domain.Email;
import com.drlom.reservation.identity.domain.Password;
import com.drlom.reservation.identity.domain.Profile;
import com.drlom.reservation.identity.domain.RefreshToken;
import com.drlom.reservation.identity.domain.RefreshTokenRepository;
import com.drlom.reservation.identity.domain.Role;
import com.drlom.reservation.identity.domain.User;
import com.drlom.reservation.identity.domain.UserRepository;
import com.drlom.reservation.identity.domain.UserStatus;
import com.drlom.reservation.identity.infrastructure.security.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// RefreshTokenUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenUseCase")
class RefreshTokenUseCaseTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @Mock private UserRepository userRepository;

  @Mock private JwtTokenProvider jwtTokenProvider;

  @InjectMocks private RefreshTokenUseCase refreshTokenUseCase;

  private static final String RAW_REFRESH_TOKEN = "valid-refresh-token";
  private static final Long USER_ID = 1L;

  private RefreshTokenCommand validCommand;
  private RefreshToken validRefreshToken;
  private User activeUser;
  private Role userRole;
  private TokenResult newTokenResult;

  @BeforeEach
  void setUp() {
    validCommand = RefreshTokenCommand.builder().refreshToken(RAW_REFRESH_TOKEN).build();

    userRole = Role.reconstitute(1L, "ROLE_USER");

    activeUser =
        User.reconstitute(
            USER_ID,
            Email.of("user@example.com"),
            Password.fromRawPassword("password123!"),
            Profile.reconstitute("홍길동", "010-1234-5678"),
            UserStatus.ACTIVE,
            null,
            Set.of(userRole),
            false);

    validRefreshToken =
        RefreshToken.reconstitute(
            1L,
            USER_ID,
            RefreshToken.hash(RAW_REFRESH_TOKEN),
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusDays(6),
            null);

    newTokenResult =
        TokenResult.builder()
            .accessToken("new-access-token")
            .refreshToken("new-refresh-token")
            .accessTokenExpiresAt(LocalDateTime.now().plusHours(1))
            .refreshTokenExpiresAt(LocalDateTime.now().plusDays(7))
            .build();
  }

  @Nested
  @DisplayName("토큰 재발급 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("유효한 Refresh Token으로 새 토큰 발급 성공")
    void refreshWithValidToken() {
      // given
      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(validRefreshToken));
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser));
      when(jwtTokenProvider.generateTokens(activeUser)).thenReturn(newTokenResult);

      // when
      TokenResult result = refreshTokenUseCase.execute(validCommand);

      // then: 새 토큰 정보 확인
      assertThat(result).isNotNull();
      assertThat(result.getAccessToken()).isEqualTo("new-access-token");
      assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");

      // then: 기존 토큰 폐기 확인
      assertThat(validRefreshToken.isRevoked()).isTrue();
      verify(refreshTokenRepository).save(validRefreshToken);

      // then: 새 RefreshToken 저장 확인
      verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("토큰 재발급 시 기존 토큰이 폐기된다 (Token Rotation)")
    void refreshRevokesOldToken() {
      // given
      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(validRefreshToken));
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser));
      when(jwtTokenProvider.generateTokens(activeUser)).thenReturn(newTokenResult);

      // when
      refreshTokenUseCase.execute(validCommand);

      // then: 기존 토큰이 폐기되었는지 확인
      assertThat(validRefreshToken.isRevoked()).isTrue();
      assertThat(validRefreshToken.getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("여러 역할을 가진 사용자의 토큰 재발급 성공")
    void refreshWithMultipleRolesUser() {
      // given
      Role adminRole = Role.reconstitute(2L, "ROLE_ADMIN");
      User multiRoleUser =
          User.reconstitute(
              USER_ID,
              Email.of("admin@example.com"),
              Password.fromRawPassword("password123!"),
              Profile.reconstitute("관리자", "010-1234-5678"),
              UserStatus.ACTIVE,
              null,
              Set.of(userRole, adminRole),
              false);

      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(validRefreshToken));
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(multiRoleUser));
      when(jwtTokenProvider.generateTokens(multiRoleUser)).thenReturn(newTokenResult);

      // when
      TokenResult result = refreshTokenUseCase.execute(validCommand);

      // then
      assertThat(result).isNotNull();
      verify(jwtTokenProvider).generateTokens(multiRoleUser);
    }
  }

  @Nested
  @DisplayName("토큰 재발급 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 Refresh Token으로 요청 시 예외 발생")
    void refreshWithNonExistentToken() {
      // given
      when(refreshTokenRepository.findByTokenHash(any(byte[].class))).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> refreshTokenUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.REFRESH_TOKEN_NOT_FOUND.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND);

      // 토큰 발급 시도하지 않아야 함
      verify(jwtTokenProvider, never()).generateTokens(any());
    }

    @Test
    @DisplayName("폐기된 Refresh Token으로 요청 시 예외 발생")
    void refreshWithRevokedToken() {
      // given: 이미 폐기된 토큰
      RefreshToken revokedToken =
          RefreshToken.reconstitute(
              2L,
              USER_ID,
              RefreshToken.hash(RAW_REFRESH_TOKEN),
              LocalDateTime.now().minusHours(2),
              LocalDateTime.now().plusDays(6),
              LocalDateTime.now().minusHours(1));

      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(revokedToken));

      // when & then
      assertThatThrownBy(() -> refreshTokenUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.INVALID_TOKEN.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_TOKEN);

      verify(jwtTokenProvider, never()).generateTokens(any());
    }

    @Test
    @DisplayName("만료된 Refresh Token으로 요청 시 예외 발생")
    void refreshWithExpiredToken() {
      // given: 만료된 토큰
      RefreshToken expiredToken =
          RefreshToken.reconstitute(
              3L,
              USER_ID,
              RefreshToken.hash(RAW_REFRESH_TOKEN),
              LocalDateTime.now().minusDays(8),
              LocalDateTime.now().minusDays(1),
              null);

      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(expiredToken));

      // when & then
      assertThatThrownBy(() -> refreshTokenUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.TOKEN_EXPIRED.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.TOKEN_EXPIRED);

      verify(jwtTokenProvider, never()).generateTokens(any());
    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 예외 발생")
    void refreshWithNonExistentUser() {
      // given
      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(validRefreshToken));
      when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> refreshTokenUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_NOT_FOUND);

      verify(jwtTokenProvider, never()).generateTokens(any());
    }

    @Test
    @DisplayName("정지된 사용자의 토큰 재발급 시 예외 발생")
    void refreshWithSuspendedUser() {
      // given
      User suspendedUser =
          User.reconstitute(
              USER_ID,
              Email.of("suspended@example.com"),
              Password.fromRawPassword("password123!"),
              Profile.reconstitute("정지된유저", "010-0000-0000"),
              UserStatus.SUSPENDED,
              null,
              Set.of(userRole),
              false);

      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(validRefreshToken));
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(suspendedUser));

      // when & then
      assertThatThrownBy(() -> refreshTokenUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.USER_SUSPENDED.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_SUSPENDED);

      verify(jwtTokenProvider, never()).generateTokens(any());
    }

    @Test
    @DisplayName("삭제된 사용자의 토큰 재발급 시 예외 발생")
    void refreshWithDeletedUser() {
      // given
      User deletedUser =
          User.reconstitute(
              USER_ID,
              Email.of("deleted@example.com"),
              Password.fromRawPassword("password123!"),
              Profile.reconstitute("삭제된유저", "010-0000-0000"),
              UserStatus.DELETED,
              null,
              Set.of(userRole),
              false);

      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(validRefreshToken));
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(deletedUser));

      // when & then
      assertThatThrownBy(() -> refreshTokenUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.USER_DELETED.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_DELETED);

      verify(jwtTokenProvider, never()).generateTokens(any());
    }
  }

  @Nested
  @DisplayName("Command 검증 테스트")
  class CommandValidationTest {

    static Stream<Arguments> invalidRefreshTokenProvider() {
      return Stream.of(
          Arguments.of(null, "null"),
          Arguments.of("", "빈 문자열"),
          Arguments.of("   ", "공백만"),
          Arguments.of("\t", "탭"),
          Arguments.of("\n", "개행"));
    }

    @ParameterizedTest(name = "refreshToken이 {1}이면 예외 발생")
    @MethodSource("invalidRefreshTokenProvider")
    @DisplayName("유효하지 않은 refreshToken으로 요청 시 예외 발생")
    void refreshWithInvalidToken(String invalidToken) {
      // given
      RefreshTokenCommand invalidCommand =
          RefreshTokenCommand.builder().refreshToken(invalidToken).build();

      // when & then
      assertThatThrownBy(() -> refreshTokenUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("refreshToken은 필수입니다");
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("만료 직전의 토큰으로 재발급 성공")
    void refreshWithAlmostExpiredToken() {
      // given: 1초 후 만료되는 토큰
      RefreshToken almostExpiredToken =
          RefreshToken.reconstitute(
              4L,
              USER_ID,
              RefreshToken.hash(RAW_REFRESH_TOKEN),
              LocalDateTime.now().minusDays(7),
              LocalDateTime.now().plusSeconds(1),
              null);

      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(almostExpiredToken));
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser));
      when(jwtTokenProvider.generateTokens(activeUser)).thenReturn(newTokenResult);

      // when
      TokenResult result = refreshTokenUseCase.execute(validCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getAccessToken()).isEqualTo("new-access-token");
    }

    @Test
    @DisplayName("이미 폐기된 토큰을 다시 폐기하려 해도 안전하게 처리")
    void revokeAlreadyRevokedToken() {
      // given: 폐기된 토큰
      RefreshToken alreadyRevokedToken =
          RefreshToken.reconstitute(
              5L,
              USER_ID,
              RefreshToken.hash(RAW_REFRESH_TOKEN),
              LocalDateTime.now().minusHours(2),
              LocalDateTime.now().plusDays(6),
              LocalDateTime.now().minusHours(1));

      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(alreadyRevokedToken));

      // when & then: 이미 폐기된 토큰이므로 INVALID_TOKEN 예외
      assertThatThrownBy(() -> refreshTokenUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("발급 직후의 토큰으로 재발급 성공")
    void refreshWithJustIssuedToken() {
      // given: 방금 발급된 토큰
      RefreshToken justIssuedToken =
          RefreshToken.reconstitute(
              6L,
              USER_ID,
              RefreshToken.hash(RAW_REFRESH_TOKEN),
              LocalDateTime.now(),
              LocalDateTime.now().plusDays(7),
              null);

      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(justIssuedToken));
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser));
      when(jwtTokenProvider.generateTokens(activeUser)).thenReturn(newTokenResult);

      // when
      TokenResult result = refreshTokenUseCase.execute(validCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getAccessToken()).isEqualTo("new-access-token");
    }
  }
}
