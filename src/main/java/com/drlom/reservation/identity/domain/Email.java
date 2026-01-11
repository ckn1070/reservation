package com.drlom.reservation.identity.domain;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Email Value Object
 *
 * <p>- 불변성: 생성 후 변경 불가
 *
 * <p>- 자가 검증: 생성 시점에 유효성 검증
 *
 * <p>- 동등성: 값 기반 비교
 *
 * <p>- 이메일 형식 검증 로직 포함
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Email {

  private static final int MAX_LENGTH = 200;
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

  private final String value;

  /**
   * 정적 팩토리 메서드
   *
   * @param email 이메일 문자열
   * @return Email Value Object
   * @throws BusinessException 이메일 형식이 올바르지 않은 경우
   */
  public static Email of(String email) {
    validate(email);
    return new Email(email.toLowerCase().trim());
  }

  /**
   * 이메일 유효성 검증
   *
   * <p>검증 규칙: - null 또는 빈 문자열 불가 - 200자 이하 - RFC 5322 간소화 버전 정규식 준수
   */
  private static void validate(String email) {
    if (email == null || email.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
    }

    String trimmedEmail = email.trim();

    if (trimmedEmail.length() > MAX_LENGTH) {
      throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
    }

    if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
      throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
    }
  }

  // 값 기반 동등성 비교 (이메일은 소문자로 정규화 후 비교)
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Email email = (Email) o;
    return Objects.equals(value, email.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
