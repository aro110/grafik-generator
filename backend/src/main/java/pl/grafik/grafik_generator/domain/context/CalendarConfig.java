package pl.grafik.grafik_generator.domain.context;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

public record CalendarConfig(YearMonth yearMonth, Set<LocalDate> holidays, Set<LocalDate> tradingSundays) {
    public CalendarConfig {
        holidays = Set.copyOf(holidays);
        tradingSundays = Set.copyOf(tradingSundays);
    }

    public int daysInMonth() {
        return yearMonth.lengthOfMonth();
    }

    public DayOfWeek firstDayOfWeek() {
        return yearMonth.atDay(1).getDayOfWeek();
    }

    public LocalDate dateAt(int dayIndex) {
        return yearMonth.atDay(dayIndex + 1);
    }

    public DayOfWeek dayOfWeekAt(int dayIndex) {
        return firstDayOfWeek().plus(dayIndex);
    }

    public boolean isClosedDay(LocalDate date) {
        if (holidays.contains(date))
            return true;
        return date.getDayOfWeek() == DayOfWeek.SUNDAY
                && !tradingSundays.contains(date);
    }

    public boolean isClosedDay(int dayIndex) {
        return isClosedDay(dateAt(dayIndex));
    }

    public List<Integer> closedDayIndices() {
        List<Integer> closed = new java.util.ArrayList<>();
        for (int i = 0; i < daysInMonth(); i++) {
            if (isClosedDay(i)) {
                closed.add(i);
            }
        }
        return closed;
    }
}
