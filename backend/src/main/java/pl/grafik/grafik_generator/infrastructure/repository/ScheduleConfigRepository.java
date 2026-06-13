package pl.grafik.grafik_generator.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.grafik.grafik_generator.domain.entity.ScheduleConfigEntity;
import pl.grafik.grafik_generator.domain.entity.ScheduleConfigStatus;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleConfigRepository extends JpaRepository<ScheduleConfigEntity, Long> {

    List<ScheduleConfigEntity> findByStatus(ScheduleConfigStatus status);

    List<ScheduleConfigEntity> findByYearMonth(LocalDate yearMonth);
}
