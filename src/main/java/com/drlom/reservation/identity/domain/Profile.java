package com.drlom.reservation.identity.domain;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Profile Value Object
 *
 * <p>- 사용자 개인정보 (이름, 전화번호)
 *
 * <p>- 불변성: 생성 후 변경 불가
 *
 * <p>- 자가 검증: 생성 시점에 유효성 검증
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Profile {

  private static final int MAX_NAME_LENGTH = 50;
  private static final int MAX_PHONE_LENGTH = 30;
  private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{2,3}-\\d{3,4}-\\d{4}$");

  private final String name;
  private final String phone;

  /**
   * 정적 팩토리 메서드
   *
   * @param name 이름
   * @param phone 전화번호
   * @return Profile Value Object
   * @throws BusinessException 유효성 검증 실패 시
   */
  public static Profile of(String name, String phone) {
    validateName(name);
    validatePhone(phone);
    return new Profile(name.trim(), phone.trim());
  }

  /**
   * DB에서 조회한 Profile 재구성 (검증 없이)
   *
   * @param name 이름
   * @param phone 전화번호
   * @return Profile Value Object
   */
  public static Profile reconstitute(String name, String phone) {
    return new Profile(name, phone);
  }

  private static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이름은 필수입니다");
    }
    if (name.trim().length() > MAX_NAME_LENGTH) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, "이름은 " + MAX_NAME_LENGTH + "자 이하여야 합니다");
    }
  }

  private static void validatePhone(String phone) {
    if (phone == null || phone.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "전화번호는 필수입니다");
    }

    String trimmedPhone = phone.trim();

    if (trimmedPhone.length() > MAX_PHONE_LENGTH) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, "전화번호는 " + MAX_PHONE_LENGTH + "자 이하여야 합니다");
    }

    if (!PHONE_PATTERN.matcher(trimmedPhone).matches()) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)");
    }
  }

  // 값 기반 동등성 비교
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Profile profile = (Profile) o;
    return Objects.equals(name, profile.name) && Objects.equals(phone, profile.phone);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, phone);
  }

  @Override
  public String toString() {
    return "Profile{" + "name='" + name + '\'' + ", phone='" + phone + '\'' + '}';
  }
}
