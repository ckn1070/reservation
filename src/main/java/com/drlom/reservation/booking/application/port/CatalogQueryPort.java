package com.drlom.reservation.booking.application.port;

import com.drlom.reservation.catalog.domain.Resource;
import java.util.Optional;

/**
 * Catalog BC 조회 Port
 *
 * <p>Booking BC에서 Catalog BC의 리소스 정보를 조회하기 위한 인터페이스
 *
 * <p>구현체는 Catalog BC의 infrastructure 계층에 위치
 */
public interface CatalogQueryPort {

  /**
   * ID로 Resource 조회
   *
   * @param id Resource ID
   * @return Resource (존재하지 않으면 Optional.empty())
   */
  Optional<Resource> findResourceById(Long id);
}
