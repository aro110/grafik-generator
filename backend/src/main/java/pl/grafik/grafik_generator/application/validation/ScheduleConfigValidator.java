package pl.grafik.grafik_generator.application.validation;

import pl.grafik.grafik_generator.domain.context.CalendarConfig;
import pl.grafik.grafik_generator.domain.context.StaffingTargets;
import pl.grafik.grafik_generator.domain.context.StoreHours;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class ScheduleConfigValidator {

    private ScheduleConfigValidator() {
    }

    public static void validate(StoreHours storeHours, StaffingTargets staffingTargets, CalendarConfig calendar) {
        if (storeHours == null || staffingTargets == null || calendar == null) {
            throw new IllegalArgumentException("Konfiguracja grafiku jest niekompletna.");
        }

        boolean hasTradingSundays = !calendar.tradingSundays().isEmpty();

        for (LocalDate tradingSunday : calendar.tradingSundays()) {
            if (!tradingSunday.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
                throw new IllegalArgumentException(
                        "Data " + tradingSunday + " nie jest niedzielą handlową (musi przypadać w niedzielę).");
            }
            if (!tradingSunday.getMonth().equals(calendar.yearMonth().getMonth())
                    || tradingSunday.getYear() != calendar.yearMonth().getYear()) {
                throw new IllegalArgumentException(
                        "Niedziela handlowa " + tradingSunday + " nie należy do wybranego miesiąca.");
            }
        }

        for (LocalDate holiday : calendar.holidays()) {
            if (!holiday.getMonth().equals(calendar.yearMonth().getMonth())
                    || holiday.getYear() != calendar.yearMonth().getYear()) {
                throw new IllegalArgumentException("Święto " + holiday + " nie należy do wybranego miesiąca.");
            }
        }

        if (hasTradingSundays) {
            if (!storeHours.hours().containsKey(DayOfWeek.SUNDAY)) {
                throw new IllegalArgumentException(
                        "Dla niedziel handlowych musisz włączyć godziny otwarcia w niedzielę.");
            }

            validateStaffingDay(staffingTargets, DayOfWeek.SUNDAY, "Niedziela");
        }

        for (DayOfWeek day : DayOfWeek.values()) {
            if (storeHours.hours().containsKey(day)) {
                validateStaffingDay(staffingTargets, day, day.name());
            }
        }
    }

    private static void validateStaffingDay(StaffingTargets staffingTargets, DayOfWeek day, String label) {
        if (staffingTargets.usesCount(day)) {
            Integer count = staffingTargets.countFor(day);
            if (count == null || count < 0) {
                throw new IllegalArgumentException(
                        "Cel obsady (liczba osób) dla " + label + " musi być liczbą większą lub równą 0.");
            }
        } else {
            int percent = staffingTargets.percentFor(day);
            if (percent < 0 || percent > 100) {
                throw new IllegalArgumentException(
                        "Cel obsady (%) dla " + label + " musi być w zakresie 0–100.");
            }
        }

    }
}
