package pl.grafik.grafik_generator.infrastructure.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.grafik.grafik_generator.domain.entity.GenerationRunEntity;
import pl.grafik.grafik_generator.domain.entity.GenerationRunStatus;

import java.util.List;

public interface GenerationRunRepository extends JpaRepository<GenerationRunEntity, Long> {

    List<GenerationRunEntity> findByStatus(GenerationRunStatus status);

    List<GenerationRunEntity> findByConfigId(Long configId);

    List<GenerationRunEntity> findByGroupId(Long groupId);

    @EntityGraph(attributePaths = { "config", "section" })
    List<GenerationRunEntity> findTop10ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = { "config", "section" })
    List<GenerationRunEntity> findTop10ByStatusOrderByCreatedAtDesc(GenerationRunStatus status);
}
