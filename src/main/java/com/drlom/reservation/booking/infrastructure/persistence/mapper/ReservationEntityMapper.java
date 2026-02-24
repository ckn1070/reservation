package com.drlom.reservation.booking.infrastructure.persistence.mapper;

import com.drlom.reservation.booking.domain.Reservation;
import com.drlom.reservation.booking.domain.ReservationItem;
import com.drlom.reservation.booking.domain.ReservationStatus;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ReservationItemJpaEntity;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ReservationJpaEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Reservation EntityMapper
 *
 * <p>Infrastructure 계층 (Domain ↔ JPA Entity 변환)
 */
@Component
public class ReservationEntityMapper {

  /**
   * JPA Entity → Domain
   *
   * @param jpaEntity ReservationJpaEntity (items 포함)
   * @return Domain Reservation
   */
  public Reservation toDomain(ReservationJpaEntity jpaEntity) {
    if (jpaEntity == null) {
      return null;
    }

    List<ReservationItem> items =
        jpaEntity.getItems().stream()
            .map(this::toItemDomain)
            .toList();

    return Reservation.reconstitute(
        jpaEntity.getId(),
        jpaEntity.getUserId(),
        jpaEntity.getShowInstanceId(),
        ReservationStatus.valueOf(jpaEntity.getStatus()),
        items,
        jpaEntity.getCancelReason(),
        jpaEntity.getConfirmedAt(),
        jpaEntity.getCancelledAt());
  }

  /**
   * Domain → JPA Entity
   *
   * @param domain Domain Reservation
   * @return ReservationJpaEntity (items 포함)
   */
  public ReservationJpaEntity toJpaEntity(Reservation domain) {
    if (domain == null) {
      return null;
    }

    ReservationJpaEntity jpaEntity;
    if (domain.getId() == null) {
      jpaEntity =
          ReservationJpaEntity.create(
              domain.getUserId(),
              domain.getShowInstanceId(),
              domain.getStatus().name());
    } else {
      jpaEntity =
          ReservationJpaEntity.reconstitute(
              domain.getId(),
              domain.getUserId(),
              domain.getShowInstanceId(),
              domain.getStatus().name(),
              domain.getCancelReason(),
              domain.getConfirmedAt(),
              domain.getCancelledAt());
    }

    // items 변환
    for (ReservationItem item : domain.getItems()) {
      ReservationItemJpaEntity itemEntity = toItemJpaEntity(item);
      jpaEntity.addItem(itemEntity);
    }

    return jpaEntity;
  }

  private ReservationItem toItemDomain(ReservationItemJpaEntity jpaEntity) {
    return ReservationItem.reconstitute(
        jpaEntity.getId(),
        jpaEntity.getSlotId(),
        jpaEntity.getPriceAmount(),
        jpaEntity.getCurrency());
  }

  private ReservationItemJpaEntity toItemJpaEntity(ReservationItem domain) {
    if (domain.getId() == null) {
      return ReservationItemJpaEntity.create(
          domain.getSlotId(), domain.getPriceAmount(), domain.getCurrency());
    } else {
      return ReservationItemJpaEntity.reconstitute(
          domain.getId(), domain.getSlotId(), domain.getPriceAmount(), domain.getCurrency());
    }
  }
}
