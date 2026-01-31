package com.drlom.reservation.identity.domain;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.security.SecureRandom;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Password Value Object
 *
 * <p>- 비밀번호 해싱 로직 캡슐화
 *
 * <p>- 불변성: 생성 후 변경 불가
 *
 * <p>- 자가 검증: 생성 시점에 유효성 검증
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Password {

  private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
  private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String DIGITS = "0123456789";
  private static final String SPECIAL = "!@#$%^&*";
  private static final String ALL_CHARACTERS = LOWERCASE + UPPERCASE + DIGITS + SPECIAL;
  private static final int TEMPORARY_PASSWORD_LENGTH = 20;

  private final String hash;

  /**
   * 평문 비밀번호로부터 Password 생성 (해싱 수행)
   *
   * @param rawPassword 평문 비밀번호
   * @return Password Value Object
   * @throws BusinessException 비밀번호가 비어있는 경우
   */
  public static Password fromRawPassword(String rawPassword) {
    validate(rawPassword);
    String hash = PASSWORD_ENCODER.encode(rawPassword);
    return new Password(hash);
  }

  /**
   * DB에서 조회한 해시값으로 Password 재구성 (해싱 없이)
   *
   * @param hash 해싱된 비밀번호
   * @return Password Value Object
   */
  public static Password fromHash(String hash) {
    return new Password(hash);
  }

  /**
   * 비밀번호 검증
   *
   * @param rawPassword 평문 비밀번호
   * @return 일치 여부
   */
  public boolean matches(String rawPassword) {
    return PASSWORD_ENCODER.matches(rawPassword, this.hash);
  }

  private static void validate(String rawPassword) {
    if (rawPassword == null || rawPassword.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_PASSWORD, "비밀번호는 필수입니다");
    }
  }

  /**
   * 임시 비밀번호 생성 (20글자, 영문 대소문자 + 숫자 + 특수문자)
   *
   * @return 임시 비밀번호 평문
   */
  public static String generateTemporaryPassword() {
    StringBuilder password = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);

    // 각 문자 종류 최소 1개 보장
    password.append(LOWERCASE.charAt(SECURE_RANDOM.nextInt(LOWERCASE.length())));
    password.append(UPPERCASE.charAt(SECURE_RANDOM.nextInt(UPPERCASE.length())));
    password.append(DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length())));
    password.append(SPECIAL.charAt(SECURE_RANDOM.nextInt(SPECIAL.length())));

    // 나머지 글자 랜덤 채우기
    for (int i = 4; i < TEMPORARY_PASSWORD_LENGTH; i++) {
      password.append(ALL_CHARACTERS.charAt(SECURE_RANDOM.nextInt(ALL_CHARACTERS.length())));
    }

    // 셔플 (Fisher-Yates)
    char[] chars = password.toString().toCharArray();
    for (int i = chars.length - 1; i > 0; i--) {
      int j = SECURE_RANDOM.nextInt(i + 1);
      char temp = chars[i];
      chars[i] = chars[j];
      chars[j] = temp;
    }

    return new String(chars);
  }

  // 값 기반 동등성 비교
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Password password = (Password) o;
    return Objects.equals(hash, password.hash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hash);
  }

  @Override
  public String toString() {
    return "Password{hash='[PROTECTED]'}";
  }
}
