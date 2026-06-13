package pl.grafik.grafik_generator.domain.context;

import java.time.DayOfWeek;
import java.util.Set;

public record VacationConfig(
        Set<DayOfWeek> workingDays,
        int hoursPerDay,
        boolean subtractHolidays) {
}
