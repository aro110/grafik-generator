package pl.grafik.grafik_generator.infrastructure.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.grafik.grafik_generator.domain.entity.ScheduleAiEditEntity;

import java.util.Optional;

public interface ScheduleAiEditRepository extends JpaRepository<ScheduleAiEditEntity, Long> {

    @EntityGraph(attributePaths = {
            "sourceSchedule",
            "sourceSchedule.run",
            "sourceSchedule.run.config",
            "sourceSchedule.run.section",
            "sourceSchedule.run.section.employees",
            "acceptedSchedule"
    })
    Optional<ScheduleAiEditEntity> findDetailedById(Long id);
}
