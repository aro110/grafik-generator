package pl.grafik.grafik_generator.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.grafik.grafik_generator.domain.context.CalendarConfig;
import pl.grafik.grafik_generator.domain.context.GaParameters;
import pl.grafik.grafik_generator.domain.context.ShiftRules;
import pl.grafik.grafik_generator.domain.context.StaffingTargets;
import pl.grafik.grafik_generator.domain.context.StoreHours;
import pl.grafik.grafik_generator.domain.context.VacationConfig;

import java.time.LocalDate;

public record UpdateScheduleConfigRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull LocalDate yearMonth,
        @NotNull StoreHours storeHours,
        @NotNull StaffingTargets staffingTargets,
        @NotNull CalendarConfig calendar,
        @NotNull ShiftRules shiftRules,
        @NotNull VacationConfig vacationConfig,
        @NotNull GaParameters gaParameters) {
}
