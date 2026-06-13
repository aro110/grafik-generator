package pl.grafik.grafik_generator.application.dto;

import pl.grafik.grafik_generator.domain.context.CalendarConfig;
import pl.grafik.grafik_generator.domain.context.GaParameters;
import pl.grafik.grafik_generator.domain.context.ShiftRules;
import pl.grafik.grafik_generator.domain.context.StaffingTargets;
import pl.grafik.grafik_generator.domain.context.StoreHours;
import pl.grafik.grafik_generator.domain.context.VacationConfig;
import pl.grafik.grafik_generator.domain.entity.ScheduleConfigStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ScheduleConfigDto(
        Long id,
        String name,
        ScheduleConfigStatus status,
        Integer version,
        LocalDate yearMonth,
        StoreHours storeHours,
        StaffingTargets staffingTargets,
        CalendarConfig calendar,
        ShiftRules shiftRules,
        VacationConfig vacationConfig,
        GaParameters gaParameters,
        Instant createdAt,
        Instant updatedAt) {
}
