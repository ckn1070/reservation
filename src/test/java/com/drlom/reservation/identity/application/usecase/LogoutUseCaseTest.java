package com.drlom.reservation.identity.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.LogoutCommand;
import com.drlom.reservation.identity.domain.RefreshToken;
import com.drlom.reservation.identity.domain.RefreshTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// LogoutUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("LogoutUseCase")
class LogoutUseCaseTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks private LogoutUseCase logoutUseCase;

  private static final String RAW_REFRESH_TOKEN = "valid-refresh-token-12345";
  private static final Long USER_ID = 1L;
  private static final Long TOKEN_ID = 100L;

  private RefreshToken validToken;
  private LogoutCommand validCommand;

  @BeforeEach
  void setUp() {
    validCommand = LogoutCommand.builder().refreshToken(RAW_REFRESH_TOKEN).build();

    validToken =
        RefreshToken.reconstituteBuilder()
            .id(TOKEN_ID)
            .userId(USER_ID)
            .tokenHash(RefreshToken.hash(RAW_REFRESH_TOKEN))
            .issuedAt(LocalDateTime.now().minusHours(1))
            .expiresAt(LocalDateTime.now().plusDays(7))
            .revokedAt(null)
            .build();
  }

  @Nested
  @DisplayName("로그아웃 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("유효한 토큰으로 로그아웃 시 토큰이 폐기된다")
    void logout_validToken_revokesToken() {
      // given
      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(validToken));
      when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(validToken);

      // when
      logoutUseCase.execute(validCommand);

      // then
      ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
      verify(refreshTokenRepository).save(tokenCaptor.capture());

      RefreshToken savedToken = tokenCaptor.getValue();
      assertThat(savedToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("유효한 토큰으로 로그아웃 시 save 메서드가 호출된다")
    void logout_validToken_saveIsCalled() {
      // given
      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(validToken));
      when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(validToken);

      // when
      logoutUseCase.execute(validCommand);

      // then
      verify(refreshTokenRepository).findByTokenHash(any(byte[].class));
      verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
  }

  @Nested
  @DisplayName("로그아웃 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 토큰으로 로그아웃 시 REFRESH_TOKEN_NOT_FOUND 예외 발생")
    void logout_nonExistentToken_throwsException() {
      // given
      when(refreshTokenRepository.findByTokenHash(any(byte[].class))).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> logoutUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.REFRESH_TOKEN_NOT_FOUND.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND);

      verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 폐기된 토큰으로 로그아웃 시 INVALID_TOKEN 예외 발생")
    void logout_alreadyRevokedToken_throwsException() {
      // given: 이미 폐기된 토큰
      RefreshToken revokedToken =
          RefreshToken.reconstituteBuilder()
              .id(TOKEN_ID)
              .userId(USER_ID)
              .tokenHash(RefreshToken.hash(RAW_REFRESH_TOKEN))
              .issuedAt(LocalDateTime.now().minusHours(1))
              .expiresAt(LocalDateTime.now().plusDays(7))
              .revokedAt(LocalDateTime.now().minusMinutes(30))
              .build();

      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(revokedToken));

      // when & then
      assertThatThrownBy(() -> logoutUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.INVALID_TOKEN.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_TOKEN);

      verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("만료된 토큰으로 로그아웃 시 INVALID_TOKEN 예외 발생")
    void logout_expiredToken_throwsException() {
      // given: 만료된 토큰
      RefreshToken expiredToken =
          RefreshToken.reconstituteBuilder()
              .id(TOKEN_ID)
              .userId(USER_ID)
              .tokenHash(RefreshToken.hash(RAW_REFRESH_TOKEN))
              .issuedAt(LocalDateTime.now().minusDays(8))
              .expiresAt(LocalDateTime.now().minusDays(1))
              .revokedAt(null)
              .build();

      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(expiredToken));

      // when & then
      assertThatThrownBy(() -> logoutUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.INVALID_TOKEN.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_TOKEN);

      verify(refreshTokenRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Command 검증 테스트")
  class CommandValidationTest {

    @Test
    @DisplayName("빈 문자열 토큰으로 로그아웃 시 예외 발생")
    void logout_emptyToken_throwsException() {
      // given
      LogoutCommand invalidCommand = LogoutCommand.builder().refreshToken("").build();

      // when & then
      assertThatThrownBy(() -> logoutUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리프레시 토큰은 필수입니다");

      verify(refreshTokenRepository, never()).findByTokenHash(any());
    }

    @Test
    @DisplayName("null 토큰으로 로그아웃 시 예외 발생")
    void logout_nullToken_throwsException() {
      // given
      LogoutCommand invalidCommand = LogoutCommand.builder().refreshToken(null).build();

      // when & then
      assertThatThrownBy(() -> logoutUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리프레시 토큰은 필수입니다");

      verify(refreshTokenRepository, never()).findByTokenHash(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("빈 값 또는 공백만 있는 토큰으로 로그아웃 시 예외 발생")
    void logout_blankToken_throwsException(String invalidToken) {
      // given
      LogoutCommand invalidCommand = LogoutCommand.builder().refreshToken(invalidToken).build();

      // when & then
      assertThatThrownBy(() -> logoutUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리프레시 토큰은 필수입니다");

      verify(refreshTokenRepository, never()).findByTokenHash(any());
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("동일한 토큰으로 두 번 로그아웃 시 두 번째는 실패한다")
    void logout_sameTwice_secondFails() {
      // given: 첫 번째 로그아웃
      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(validToken));
      when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(validToken);

      logoutUseCase.execute(validCommand);

      // given: 두 번째 로그아웃 - 이미 폐기된 토큰 반환
      RefreshToken revokedToken =
          RefreshToken.reconstituteBuilder()
              .id(TOKEN_ID)
              .userId(USER_ID)
              .tokenHash(RefreshToken.hash(RAW_REFRESH_TOKEN))
              .issuedAt(LocalDateTime.now().minusHours(1))
              .expiresAt(LocalDateTime.now().plusDays(7))
              .revokedAt(LocalDateTime.now())
              .build();

      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(revokedToken));

      // when & then
      assertThatThrownBy(() -> logoutUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("토큰 해시 생성이 올바르게 호출된다")
    void logout_correctTokenHashUsed() {
      // given
      when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
          .thenReturn(Optional.of(validToken));
      when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(validToken);

      // when
      logoutUseCase.execute(validCommand);

      // then
      ArgumentCaptor<byte[]> hashCaptor = ArgumentCaptor.forClass(byte[].class);
      verify(refreshTokenRepository).findByTokenHash(hashCaptor.capture());

      byte[] expectedHash = RefreshToken.hash(RAW_REFRESH_TOKEN);
      assertThat(hashCaptor.getValue()).isEqualTo(expectedHash);
    }
  }
}
