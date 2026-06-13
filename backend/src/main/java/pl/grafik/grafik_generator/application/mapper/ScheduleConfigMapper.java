package pl.grafik.grafik_generator.application.mapper;

import org.springframework.stereotype.Component;
import pl.grafik.grafik_generator.application.dto.ScheduleConfigDto;
import pl.grafik.grafik_generator.domain.context.GenerationContext;
import pl.grafik.grafik_generator.domain.context.VacationConfig;
import pl.grafik.grafik_generator.domain.entity.ScheduleConfigEntity;

import java.time.DayOfWeek;
import java.util.Set;

@Component
public class ScheduleConfigMapper {

    public ScheduleConfigDto toDto(ScheduleConfigEntity entity) {
        return new ScheduleConfigDto(
                entity.getId(),
                entity.getName(),
                entity.getStatus(),
                entity.getVersion(),
                entity.getYearMonth(),
                entity.getStoreHours(),
                entity.getStaffingTargets(),
                entity.getCalendar(),
                entity.getShiftRules(),
                entity.getVacationConfig(),
                entity.getGaParameters(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public GenerationContext toContext(ScheduleConfigEntity entity) {
        VacationConfig vac = entity.getVacationConfig();
        if (vac == null) {
            vac = new VacationConfig(
                    Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                            DayOfWeek.FRIDAY),
                    8,
                    true);
        }
        return new GenerationContext(
                entity.getStoreHours(),
                entity.getStaffingTargets(),
                entity.getCalendar(),
                entity.getShiftRules(),
                vac,
                entity.getGaParameters());
    }
}
