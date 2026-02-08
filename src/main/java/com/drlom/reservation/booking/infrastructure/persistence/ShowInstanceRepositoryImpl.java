package com.drlom.reservation.booking.infrastructure.persistence;

import com.drlom.reservation.booking.application.port.CatalogQueryPort;
import com.drlom.reservation.booking.domain.ShowInstance;
import com.drlom.reservation.booking.domain.ShowInstanceRepository;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ShowInstanceJpaEntity;
import com.drlom.reservation.booking.infrastructure.persistence.mapper.ShowInstanceEntityMapper;
import com.drlom.reservation.catalog.domain.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * ShowInstanceRepository 구현체
 *
 * <p>Infrastructure 계층: Domain ShowInstanceRepository 인터페이스 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ShowInstanceRepositoryImpl implements ShowInstanceRepository {

  private final ShowInstanceJpaRepository jpaRepository;
  private final ShowInstanceEntityMapper entityMapper;
  private final CatalogQueryPort catalogQueryPort;

  @Override
  public ShowInstance save(ShowInstance showInstance) {
    log.debug(
        "ShowInstance 저장: venueId={}, title={}",
        showInstance.getVenue().getId(),
        showInstance.getTitle());

    ShowInstanceJpaEntity jpaEntity = entityMapper.toJpaEntity(showInstance);
    ShowInstanceJpaEntity savedEntity = jpaRepository.save(jpaEntity);

    return entityMapper.toDomain(savedEntity, showInstance.getVenue());
  }

  @Override
  public Optional<ShowInstance> findById(Long id) {
    log.debug("ShowInstance 조회: id={}", id);

    return jpaRepository.findById(id).map(this::toDomainWithVenue);
  }

  @Override
  public List<ShowInstance> findOverlappingShows(
      Long venueId, LocalDateTime startAt, LocalDateTime endAt) {
    log.debug("겹치는 ShowInstance 조회: venueId={}, startAt={}, endAt={}", venueId, startAt, endAt);

    return jpaRepository.findOverlappingShows(venueId, startAt, endAt).stream()
        .map(this::toDomainWithVenue)
        .toList();
  }

  @Override
  public List<ShowInstance> findByVenueId(Long venueId) {
    log.debug("ShowInstance 조회 by venueId: venueId={}", venueId);

    return jpaRepository.findByResourceIdOrderByStartAtAsc(venueId).stream()
        .map(this::toDomainWithVenue)
        .toList();
  }

  @Override
  public List<ShowInstance> findByStatus(ShowStatus status) {
    log.debug("ShowInstance 조회 by status: status={}", status);

    return jpaRepository.findByStatusOrderByStartAtAsc(status.name()).stream()
        .map(this::toDomainWithVenue)
        .toList();
  }

  @Override
  public List<ShowInstance> findAll() {
    log.debug("ShowInstance 전체 조회");

    return jpaRepository.findAllByOrderByStartAtAsc().stream()
        .map(this::toDomainWithVenue)
        .toList();
  }

  @Override
  public List<ShowInstance> findByVenueIdAndStatus(Long venueId, ShowStatus status) {
    log.debug("ShowInstance 조회 by venueId and status: venueId={}, status={}", venueId, status);

    return jpaRepository
        .findByResourceIdAndStatusOrderByStartAtAsc(venueId, status.name())
        .stream()
        .map(this::toDomainWithVenue)
        .toList();
  }

  @Override
  public void delete(ShowInstance showInstance) {
    log.debug("ShowInstance 삭제: id={}", showInstance.getId());

    jpaRepository.deleteById(showInstance.getId());
  }

  private ShowInstance toDomainWithVenue(
      com.drlom.reservation.booking.infrastructure.persistence.entity.ShowInstanceJpaEntity
          jpaEntity) {
    Resource venue =
        catalogQueryPort
            .findResourceById(jpaEntity.getResourceId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "공연장을 찾을 수 없습니다: " + jpaEntity.getResourceId()));

    return entityMapper.toDomain(jpaEntity, venue);
  }
}
