package pl.grafik.grafik_generator.infrastructure.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.grafik.grafik_generator.domain.entity.ScheduleEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {

    @EntityGraph(attributePaths = { "run", "run.config", "run.section" })
    List<ScheduleEntity> findByRunId(Long runId);

    @EntityGraph(attributePaths = { "run", "run.config", "run.section" })
    List<ScheduleEntity> findByRunIdIn(Collection<Long> runIds);

    @EntityGraph(attributePaths = { "run", "run.config", "run.section" })
    List<ScheduleEntity> findByPublishedTrueOrderByPublishedAtDesc();

    @EntityGraph(attributePaths = { "run", "run.config", "run.section", "run.section.employees" })
    List<ScheduleEntity> findByRunGroupId(Long groupId);

    @EntityGraph(attributePaths = { "run", "run.config", "run.section", "run.section.employees" })
    Optional<ScheduleEntity> findDetailedById(Long id);
}
