package pl.grafik.grafik_generator.domain.context;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;

public record StaffingTargets(
        Map<DayOfWeek, Integer> percentByDay,
        Map<DayOfWeek, Integer> countByDay,
        Map<DayOfWeek, PeakWindow> peakByDay) {

    public record PeakWindow(LocalTime start, LocalTime end) {
        public PeakWindow {
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("Peak end musi być po start");
            }
        }
    }

    public StaffingTargets {
        percentByDay = percentByDay != null ? Map.copyOf(percentByDay) : Map.of();
        countByDay = countByDay != null ? Map.copyOf(countByDay) : Map.of();
        peakByDay = peakByDay != null ? Map.copyOf(peakByDay) : Map.of();
    }

    public int percentFor(DayOfWeek day) {
        return percentByDay.getOrDefault(day, 50);
    }

    public boolean usesCount(DayOfWeek day) {
        return countByDay.containsKey(day) && countByDay.get(day) != null;
    }

    public Integer countFor(DayOfWeek day) {
        return countByDay.get(day);
    }

    public int requiredEmployees(DayOfWeek day, int sectionEmployeeCount) {
        if (sectionEmployeeCount <= 0) {
            return 0;
        }
        if (usesCount(day)) {
            int count = countByDay.get(day);
            if (count < 0) {
                return 0;
            }
            return Math.min(count, sectionEmployeeCount);
        }
        return (int) Math.round(sectionEmployeeCount * (percentFor(day) / 100.0));
    }

    public PeakWindow peakFor(DayOfWeek day) {
        return peakByDay.get(day);
    }

    public int[] peakHoursArray(DayOfWeek day, int openHour, int closeHour) {
        int length = closeHour - openHour;
        int[] peak = new int[length];

        PeakWindow pw = peakByDay.get(day);
        if (pw == null) {
            return peak;
        }

        int peakStart = pw.start().getHour();
        int peakEnd = pw.end().getHour();

        for (int i = 0; i < length; i++) {
            int hour = openHour + i;
            if (hour >= peakStart && hour < peakEnd) {
                peak[i] = 1;
            }
        }
        return peak;
    }
}
