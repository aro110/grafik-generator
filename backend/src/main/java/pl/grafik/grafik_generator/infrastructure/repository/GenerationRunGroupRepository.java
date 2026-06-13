package pl.grafik.grafik_generator.infrastructure.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.grafik.grafik_generator.domain.entity.GenerationRunGroupEntity;

import java.util.List;
import java.util.Optional;

public interface GenerationRunGroupRepository extends JpaRepository<GenerationRunGroupEntity, Long> {

    List<GenerationRunGroupEntity> findByConfigId(Long configId);

    @EntityGraph(attributePaths = { "config", "runs", "runs.section" })
    Optional<GenerationRunGroupEntity> findDetailedById(Long id);

    @EntityGraph(attributePaths = { "config", "runs", "runs.section" })
    List<GenerationRunGroupEntity> findTop10ByOrderByCreatedAtDesc();
}
