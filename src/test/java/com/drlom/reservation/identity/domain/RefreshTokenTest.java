package com.drlom.reservation.identity.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// RefreshToken Aggregate Root 테스트
@DisplayName("RefreshToken Aggregate Root")
class RefreshTokenTest {

  private static final String RAW_TOKEN = "test-refresh-token-12345";
  private static final Long USER_ID = 1L;

  @Nested
  @DisplayName("토큰 폐기 테스트 (revoke)")
  class RevokeTest {

    @Test
    @DisplayName("정상 토큰을 폐기하면 revokedAt이 설정된다")
    void revoke_validToken_setsRevokedAt() {
      // given
      RefreshToken token =
          RefreshToken.create(USER_ID, RAW_TOKEN, LocalDateTime.now().plusDays(7));

      // when
      token.revoke();

      // then
      assertThat(token.getRevokedAt()).isNotNull();
      assertThat(token.getRevokedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("폐기된 토큰의 isRevoked()는 true를 반환한다")
    void isRevoked_afterRevoke_returnsTrue() {
      // given
      RefreshToken token =
          RefreshToken.create(USER_ID, RAW_TOKEN, LocalDateTime.now().plusDays(7));

      // when
      token.revoke();

      // then
      assertThat(token.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("이미 폐기된 토큰을 다시 폐기해도 revokedAt은 유지된다 (멱등성)")
    void revoke_alreadyRevoked_keepsOriginalRevokedAt() {
      // given
      RefreshToken token =
          RefreshToken.create(USER_ID, RAW_TOKEN, LocalDateTime.now().plusDays(7));
      token.revoke();
      LocalDateTime firstRevokedAt = token.getRevokedAt();

      // when: 다시 폐기 시도
      token.revoke();

      // then: 첫 번째 revokedAt 유지
      assertThat(token.getRevokedAt()).isEqualTo(firstRevokedAt);
    }

    @Test
    @DisplayName("여러 번 revoke 호출해도 첫 번째 타임스탬프가 유지된다")
    void revoke_multipleCalls_keepsFirstTimestamp() {
      // given
      RefreshToken token =
          RefreshToken.create(USER_ID, RAW_TOKEN, LocalDateTime.now().plusDays(7));
      token.revoke();
      LocalDateTime firstRevokedAt = token.getRevokedAt();

      // when: 다시 호출
      token.revoke();
      token.revoke();

      // then
      assertThat(token.getRevokedAt()).isEqualTo(firstRevokedAt);
    }
  }

  @Nested
  @DisplayName("토큰 유효성 검증 테스트 (isValid)")
  class IsValidTest {

    @Test
    @DisplayName("폐기된 토큰은 유효하지 않다")
    void isValid_revokedToken_returnsFalse() {
      // given
      RefreshToken token =
          RefreshToken.create(USER_ID, RAW_TOKEN, LocalDateTime.now().plusDays(7));
      token.revoke();

      // when & then
      assertThat(token.isValid()).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 유효하지 않다")
    void isValid_expiredToken_returnsFalse() {
      // given: 이미 만료된 토큰
      RefreshToken token =
          RefreshToken.reconstituteBuilder()
              .id(1L)
              .userId(USER_ID)
              .tokenHash(RefreshToken.hash(RAW_TOKEN))
              .issuedAt(LocalDateTime.now().minusDays(8))
              .expiresAt(LocalDateTime.now().minusDays(1))
              .revokedAt(null)
              .build();

      // when & then
      assertThat(token.isValid()).isFalse();
      assertThat(token.isExpired()).isTrue();
    }

    @Test
    @DisplayName("유효한 토큰은 isValid()가 true를 반환한다")
    void isValid_validToken_returnsTrue() {
      // given
      RefreshToken token =
          RefreshToken.create(USER_ID, RAW_TOKEN, LocalDateTime.now().plusDays(7));

      // when & then
      assertThat(token.isValid()).isTrue();
      assertThat(token.isRevoked()).isFalse();
      assertThat(token.isExpired()).isFalse();
    }
  }

  @Nested
  @DisplayName("토큰 매칭 테스트 (matches)")
  class MatchesTest {

    @Test
    @DisplayName("null 토큰과 비교 시 false를 반환한다")
    void matches_nullToken_returnsFalse() {
      // given
      RefreshToken token =
          RefreshToken.create(USER_ID, RAW_TOKEN, LocalDateTime.now().plusDays(7));

      // when & then
      assertThat(token.matches(null)).isFalse();
    }

    @Test
    @DisplayName("동일한 원본 토큰과 비교 시 true를 반환한다")
    void matches_sameToken_returnsTrue() {
      // given
      RefreshToken token =
          RefreshToken.create(USER_ID, RAW_TOKEN, LocalDateTime.now().plusDays(7));

      // when & then
      assertThat(token.matches(RAW_TOKEN)).isTrue();
    }

    @Test
    @DisplayName("다른 토큰과 비교 시 false를 반환한다")
    void matches_differentToken_returnsFalse() {
      // given
      RefreshToken token =
          RefreshToken.create(USER_ID, RAW_TOKEN, LocalDateTime.now().plusDays(7));

      // when & then
      assertThat(token.matches("different-token")).isFalse();
    }
  }

  @Nested
  @DisplayName("해시 테스트 (hash)")
  class HashTest {

    @Test
    @DisplayName("동일한 토큰은 동일한 해시를 생성한다")
    void hash_sameToken_sameHash() {
      // given
      String token = "test-token-12345";

      // when
      byte[] hash1 = RefreshToken.hash(token);
      byte[] hash2 = RefreshToken.hash(token);

      // then
      assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("다른 토큰은 다른 해시를 생성한다")
    void hash_differentToken_differentHash() {
      // given
      String token1 = "test-token-12345";
      String token2 = "test-token-67890";

      // when
      byte[] hash1 = RefreshToken.hash(token1);
      byte[] hash2 = RefreshToken.hash(token2);

      // then
      assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("해시 결과는 32바이트(SHA-256)이다")
    void hash_resultIs32Bytes() {
      // given
      String token = "test-token";

      // when
      byte[] hash = RefreshToken.hash(token);

      // then
      assertThat(hash).hasSize(32);
    }
  }

  @Nested
  @DisplayName("토큰 생성 테스트 (create)")
  class CreateTest {

    @Test
    @DisplayName("유효한 파라미터로 토큰을 생성할 수 있다")
    void create_validParams_success() {
      // given
      LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

      // when
      RefreshToken token = RefreshToken.create(USER_ID, RAW_TOKEN, expiresAt);

      // then
      assertThat(token.getId()).isNull();
      assertThat(token.getUserId()).isEqualTo(USER_ID);
      assertThat(token.getTokenHash()).isNotNull();
      assertThat(token.getIssuedAt()).isNotNull();
      assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
      assertThat(token.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("userId가 null이면 예외가 발생한다")
    void create_nullUserId_throwsException() {
      // given
      LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

      // when & then
      assertThatThrownBy(() -> RefreshToken.create(null, RAW_TOKEN, expiresAt))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("userId는 필수입니다");
    }

    @Test
    @DisplayName("rawToken이 null이면 예외가 발생한다")
    void create_nullRawToken_throwsException() {
      // given
      LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

      // when & then
      assertThatThrownBy(() -> RefreshToken.create(USER_ID, null, expiresAt))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("rawToken은 필수입니다");
    }

    @Test
    @DisplayName("expiresAt이 null이면 예외가 발생한다")
    void create_nullExpiresAt_throwsException() {
      // when & then
      assertThatThrownBy(() -> RefreshToken.create(USER_ID, RAW_TOKEN, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("expiresAt은 필수입니다");
    }
  }

  @Nested
  @DisplayName("재구성 테스트 (Reconstitute)")
  class ReconstituteTest {

    @Test
    @DisplayName("DB에서 조회한 RefreshToken을 재구성할 수 있다")
    void reconstituteFromDatabase() {
      // given
      Long id = 1L;
      byte[] tokenHash = RefreshToken.hash(RAW_TOKEN);
      LocalDateTime issuedAt = LocalDateTime.now().minusHours(1);
      LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
      LocalDateTime revokedAt = LocalDateTime.now();

      // when
      RefreshToken token =
          RefreshToken.reconstituteBuilder()
              .id(id)
              .userId(USER_ID)
              .tokenHash(tokenHash)
              .issuedAt(issuedAt)
              .expiresAt(expiresAt)
              .revokedAt(revokedAt)
              .build();

      // then
      assertThat(token.getId()).isEqualTo(id);
      assertThat(token.getUserId()).isEqualTo(USER_ID);
      assertThat(token.getTokenHash()).isEqualTo(tokenHash);
      assertThat(token.getIssuedAt()).isEqualTo(issuedAt);
      assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
      assertThat(token.getRevokedAt()).isEqualTo(revokedAt);
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 ID를 가진 RefreshToken은 동등하다")
    void equalityWithSameId() {
      // given
      RefreshToken token1 =
          RefreshToken.reconstituteBuilder()
              .id(1L)
              .userId(USER_ID)
              .tokenHash(RefreshToken.hash("token1"))
              .issuedAt(LocalDateTime.now())
              .expiresAt(LocalDateTime.now().plusDays(7))
              .build();

      RefreshToken token2 =
          RefreshToken.reconstituteBuilder()
              .id(1L)
              .userId(2L)
              .tokenHash(RefreshToken.hash("token2"))
              .issuedAt(LocalDateTime.now())
              .expiresAt(LocalDateTime.now().plusDays(7))
              .build();

      // when & then
      assertThat(token1).isEqualTo(token2).hasSameHashCodeAs(token2);
    }

    @Test
    @DisplayName("다른 ID를 가진 RefreshToken은 동등하지 않다")
    void inequalityWithDifferentId() {
      // given
      RefreshToken token1 =
          RefreshToken.reconstituteBuilder()
              .id(1L)
              .userId(USER_ID)
              .tokenHash(RefreshToken.hash(RAW_TOKEN))
              .issuedAt(LocalDateTime.now())
              .expiresAt(LocalDateTime.now().plusDays(7))
              .build();

      RefreshToken token2 =
          RefreshToken.reconstituteBuilder()
              .id(2L)
              .userId(USER_ID)
              .tokenHash(RefreshToken.hash(RAW_TOKEN))
              .issuedAt(LocalDateTime.now())
              .expiresAt(LocalDateTime.now().plusDays(7))
              .build();

      // when & then
      assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("ID가 null인 RefreshToken끼리는 동등하지 않다")
    void inequalityWithNullIds() {
      // given
      RefreshToken token1 =
          RefreshToken.create(USER_ID, RAW_TOKEN, LocalDateTime.now().plusDays(7));
      RefreshToken token2 =
          RefreshToken.create(USER_ID, RAW_TOKEN, LocalDateTime.now().plusDays(7));

      // when & then
      assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("자기 자신과 비교 시 동등하다")
    void equalityWithSelf() {
      // given
      RefreshToken token =
          RefreshToken.create(USER_ID, RAW_TOKEN, LocalDateTime.now().plusDays(7));

      // when & then
      assertThat(token).isEqualTo(token);
    }

    @Test
    @DisplayName("null과 비교 시 동등하지 않다")
    void inequalityWithNull() {
      // given
      RefreshToken token =
          RefreshToken.reconstituteBuilder()
              .id(1L)
              .userId(USER_ID)
              .tokenHash(RefreshToken.hash(RAW_TOKEN))
              .issuedAt(LocalDateTime.now())
              .expiresAt(LocalDateTime.now().plusDays(7))
              .build();

      // when & then
      assertThat(token).isNotEqualTo(null);
    }
  }
}
