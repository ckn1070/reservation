package com.drlom.reservation.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourcePolicy;
import com.drlom.reservation.catalog.domain.ResourcePolicyRepository;
import com.drlom.reservation.catalog.domain.ResourceRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.mapper.ResourceEntityMapper;
import com.drlom.reservation.catalog.infrastructure.persistence.mapper.ResourcePolicyEntityMapper;
import com.drlom.reservation.common.config.JpaAuditingConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

// ResourcePolicyRepositoryImpl 통합 테스트
@DataJpaTest
@Import({
  ResourceRepositoryImpl.class,
  ResourceEntityMapper.class,
  ResourcePolicyRepositoryImpl.class,
  ResourcePolicyEntityMapper.class,
  JpaAuditingConfig.class
})
@DisplayName("ResourcePolicyRepositoryImpl 통합 테스트")
class ResourcePolicyRepositoryImplTest {

  @Autowired private ResourceRepository resourceRepository;

  @Autowired private ResourcePolicyRepository policyRepository;

  private Resource savedVenue;
  private ResourcePolicy savedStringPolicy;

  @BeforeEach
  void setUp() {
    // VENUE 생성 및 저장
    Resource venue = Resource.createVenue("VN001", "소극장", 100);
    savedVenue = resourceRepository.save(venue);

    // String 정책 생성
    ResourcePolicy stringPolicy =
        ResourcePolicy.createStringPolicy(savedVenue, "RESTRICTION", "19세 이상 관람가");
    savedStringPolicy = policyRepository.save(stringPolicy);

    // Number 정책 생성
    ResourcePolicy numberPolicy =
        ResourcePolicy.createNumberPolicy(savedVenue, "AGE_LIMIT", BigDecimal.valueOf(19));
    policyRepository.save(numberPolicy);
  }

  @Nested
  @DisplayName("save 테스트")
  class SaveTest {

    @Test
    @DisplayName("String 정책 저장 성공")
    void saveStringPolicy() {
      // given
      ResourcePolicy policy =
          ResourcePolicy.createStringPolicy(savedVenue, "NEW_POLICY", "정책 값");

      // when
      ResourcePolicy saved = policyRepository.save(policy);

      // then
      assertThat(saved).isNotNull();
      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getPolicyType()).isEqualTo("NEW_POLICY");
      assertThat(saved.getValueString()).isEqualTo("정책 값");
    }

    @Test
    @DisplayName("Number 정책 저장 성공")
    void saveNumberPolicy() {
      // given
      ResourcePolicy policy =
          ResourcePolicy.createNumberPolicy(savedVenue, "MAX_TICKETS", BigDecimal.valueOf(10));

      // when
      ResourcePolicy saved = policyRepository.save(policy);

      // then
      assertThat(saved).isNotNull();
      assertThat(saved.getPolicyType()).isEqualTo("MAX_TICKETS");
      assertThat(saved.getValueNumber()).isEqualTo(BigDecimal.valueOf(10));
    }

    @Test
    @DisplayName("Bool 정책 저장 성공")
    void saveBoolPolicy() {
      // given
      ResourcePolicy policy = ResourcePolicy.createBoolPolicy(savedVenue, "NO_SMOKING", true);

      // when
      ResourcePolicy saved = policyRepository.save(policy);

      // then
      assertThat(saved).isNotNull();
      assertThat(saved.getPolicyType()).isEqualTo("NO_SMOKING");
      assertThat(saved.getValueBool()).isTrue();
    }
  }

  @Nested
  @DisplayName("findById 테스트")
  class FindByIdTest {

    @Test
    @DisplayName("존재하는 ID로 조회 성공")
    void findByExistingId() {
      // when
      Optional<ResourcePolicy> found = policyRepository.findById(savedStringPolicy.getId());

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getPolicyType()).isEqualTo("RESTRICTION");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 empty 반환")
    void findByNonExistingId() {
      // when
      Optional<ResourcePolicy> found = policyRepository.findById(999L);

      // then
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByResource 테스트")
  class FindByResourceTest {

    @Test
    @DisplayName("리소스의 모든 정책 조회 성공")
    void findByResource() {
      // when
      List<ResourcePolicy> policies = policyRepository.findByResource(savedVenue);

      // then
      assertThat(policies).hasSize(2);
      assertThat(policies).extracting("policyType").containsExactlyInAnyOrder("RESTRICTION", "AGE_LIMIT");
    }

    @Test
    @DisplayName("정책이 없는 리소스 조회 시 빈 목록 반환")
    void findByResourceWithNoPolicies() {
      // given
      Resource newVenue = Resource.createVenue("VN002", "대극장", 500);
      Resource savedNewVenue = resourceRepository.save(newVenue);

      // when
      List<ResourcePolicy> policies = policyRepository.findByResource(savedNewVenue);

      // then
      assertThat(policies).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByResourceAndPolicyType 테스트")
  class FindByResourceAndPolicyTypeTest {

    @Test
    @DisplayName("리소스와 정책 타입으로 조회 성공")
    void findByResourceAndPolicyType() {
      // when
      Optional<ResourcePolicy> found =
          policyRepository.findByResourceAndPolicyType(savedVenue, "RESTRICTION");

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getValueString()).isEqualTo("19세 이상 관람가");
    }

    @Test
    @DisplayName("존재하지 않는 정책 타입 조회 시 empty 반환")
    void findByNonExistingPolicyType() {
      // when
      Optional<ResourcePolicy> found =
          policyRepository.findByResourceAndPolicyType(savedVenue, "NON_EXISTENT");

      // then
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("delete 테스트")
  class DeleteTest {

    @Test
    @DisplayName("정책 삭제 성공")
    void deletePolicy() {
      // given
      Long id = savedStringPolicy.getId();

      // when
      policyRepository.delete(savedStringPolicy);

      // then
      Optional<ResourcePolicy> found = policyRepository.findById(id);
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("deleteByResource 테스트")
  class DeleteByResourceTest {

    @Test
    @DisplayName("리소스의 모든 정책 삭제 성공")
    void deleteByResource() {
      // given - 삭제 전 확인
      List<ResourcePolicy> beforeDelete = policyRepository.findByResource(savedVenue);
      assertThat(beforeDelete).hasSize(2);

      // when
      policyRepository.deleteByResource(savedVenue);

      // then
      List<ResourcePolicy> afterDelete = policyRepository.findByResource(savedVenue);
      assertThat(afterDelete).isEmpty();
    }
  }
}
