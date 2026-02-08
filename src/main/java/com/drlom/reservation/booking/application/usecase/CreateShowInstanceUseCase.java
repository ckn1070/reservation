package com.drlom.reservation.booking.application.usecase;

import com.drlom.reservation.booking.application.dto.command.CreateShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.port.CatalogQueryPort;
import com.drlom.reservation.booking.domain.ShowInstance;
import com.drlom.reservation.booking.domain.ShowInstanceRepository;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceType;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공연 회차 생성 UseCase
 *
 * <p>공연장(VENUE)에 대한 공연 회차 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateShowInstanceUseCase {

  private final ShowInstanceRepository showInstanceRepository;
  private final CatalogQueryPort catalogQueryPort;

  /**
   * 공연 회차 생성 실행
   *
   * @param command 공연 회차 생성 Command
   * @return ShowInstanceResult
   */
  @Transactional
  public ShowInstanceResult execute(CreateShowInstanceCommand command) {
    log.info("공연 회차 생성 시작: venueId={}, title={}", command.getVenueId(), command.getTitle());

    // 1. Command 검증
    command.validate();

    // 2. 공연장 조회 및 VENUE 타입 검증
    Resource venue = findAndValidateVenue(command.getVenueId());

    // 3. 동일 공연장/시간대 중복 체크
    validateNoOverlappingShows(
        command.getVenueId(), command.getStartAt(), command.getEndAt());

    // 4. Domain 객체 생성 (비즈니스 검증은 Domain에서)
    ShowInstance showInstance =
        ShowInstance.create(
            venue,
            command.getTitle(),
            command.getStartAt(),
            command.getEndAt(),
            command.getSalesOpenAt(),
            command.getSalesCloseAt());

    // 5. 저장
    ShowInstance savedShow = showInstanceRepository.save(showInstance);
    log.info("공연 회차 생성 완료: id={}", savedShow.getId());

    return ShowInstanceResult.from(savedShow);
  }

  private Resource findAndValidateVenue(Long venueId) {
    Resource resource =
        catalogQueryPort
            .findResourceById(venueId)
            .orElseThrow(
                () -> {
                  log.warn("존재하지 않는 공연장: venueId={}", venueId);
                  return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
                });

    if (resource.getType() != ResourceType.VENUE) {
      log.warn("VENUE 타입이 아닌 리소스: venueId={}, type={}", venueId, resource.getType());
      throw new BusinessException(ErrorCode.INVALID_VENUE_TYPE);
    }

    return resource;
  }

  private void validateNoOverlappingShows(
      Long venueId, java.time.LocalDateTime startAt, java.time.LocalDateTime endAt) {
    List<ShowInstance> overlappingShows =
        showInstanceRepository.findOverlappingShows(venueId, startAt, endAt);

    if (!overlappingShows.isEmpty()) {
      log.warn("동일 시간대에 겹치는 공연 존재: venueId={}, count={}", venueId, overlappingShows.size());
      throw new BusinessException(ErrorCode.SHOW_INSTANCE_ALREADY_EXISTS);
    }
  }
}
