package pl.grafik.grafik_generator.domain.model;

import pl.grafik.grafik_generator.domain.context.CalendarConfig;
import pl.grafik.grafik_generator.domain.context.ShiftRules;
import pl.grafik.grafik_generator.domain.shiftPoolGenerator.ShiftCombination;
import pl.grafik.grafik_generator.domain.shiftPoolGenerator.ShiftPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class Employee {
    private static final Map<String, Map<Integer, Integer>> sectionSaturdayUsage = new HashMap<>();

    private final String name;
    private final String surname;
    private final String section;
    private final int totalHours;
    private final int totalDays;
    private final List<ShiftCombination> shiftPool;
    private final List<Integer> daysOff;

    private final List<Integer> vacations;

    public Employee(String name, String surname, String section, int totalHours, int totalDays,
            List<Integer> daysOff, List<Integer> vacations, ShiftPool shiftPool, ShiftRules rules,
            CalendarConfig calendar, pl.grafik.grafik_generator.domain.context.VacationConfig vacationConfig)
            throws IllegalArgumentException {
        this.name = name;
        this.surname = surname;
        this.section = section;
        this.vacations = vacations == null ? new ArrayList<>() : new ArrayList<>(vacations);

        int adjustedTotalHours = totalHours;
        int adjustedTotalDays = totalDays;

        if (vacationConfig != null && this.vacations != null) {
            for (Integer vacDay : this.vacations) {
                java.time.LocalDate date = calendar.dateAt(vacDay);
                if (vacationConfig.workingDays().contains(date.getDayOfWeek())) {
                    if (vacationConfig.subtractHolidays() && calendar.holidays().contains(date)) {
                        continue;
                    }
                    adjustedTotalHours -= vacationConfig.hoursPerDay();
                    adjustedTotalDays -= 1;
                }
            }
        }

        this.totalHours = adjustedTotalHours;
        this.totalDays = adjustedTotalDays;

        this.shiftPool = shiftPool.generateAll(this.totalHours, this.totalDays);
        validateShiftPool(rules.shiftLengths());

        List<Integer> combinedDaysOff = daysOff == null ? new ArrayList<>() : new ArrayList<>(daysOff);
        combinedDaysOff.addAll(this.vacations);

        this.daysOff = grantFreeWeekend(combinedDaysOff, rules, calendar);
        validateTotalHours(rules.shiftLengths());
        validateTotalDays(this.totalDays, calendar);
    }

    private List<Integer> grantFreeWeekend(List<Integer> daysOff, ShiftRules rules, CalendarConfig calendar) {
        List<Integer> result = new ArrayList<>(daysOff);
        if (rules.grantFreeWeekend()) {
            int firstDayIndex = calendar.firstDayOfWeek().getValue() % 7;
            Map<Integer, Integer> usage = sectionSaturdayUsage.computeIfAbsent(section, k -> new HashMap<>());
            List<Integer> saturdays = new ArrayList<>();
            for (int i = 0; i < calendar.daysInMonth(); i++) {
                int dayOfWeek = (firstDayIndex + i) % 7;
                if (dayOfWeek == 6) {
                    saturdays.add(i);
                    usage.putIfAbsent(i, 0);
                }
            }
            if (!saturdays.isEmpty()) {
                int minUsage = saturdays.stream().mapToInt(usage::get).min().orElse(0);
                List<Integer> leastUsed = saturdays.stream()
                        .filter(s -> usage.get(s) == minUsage && !result.contains(s))
                        .collect(Collectors.toList());
                if (leastUsed.isEmpty()) {
                    leastUsed = saturdays.stream()
                            .filter(s -> usage.get(s) == minUsage)
                            .collect(Collectors.toList());
                }
                int picked = leastUsed.get(new Random().nextInt(leastUsed.size()));
                if (!result.contains(picked)) {
                    result.add(picked);
                }
                usage.merge(picked, 1, Integer::sum);
            }
        }
        return result;
    }

    private void validateShiftPool(List<Integer> validShifts) throws IllegalArgumentException {
        if (shiftPool.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Nie można przydzielić %d godzin w %d dniach przy dostępnych długościach zmian: %s.",
                    totalHours, totalDays, validShifts));
        }
    }

    private void validateTotalHours(List<Integer> validShifts) throws IllegalArgumentException {
        int minHours = validShifts.getFirst() * totalDays;
        int maxHours = validShifts.getLast() * totalDays;

        if (totalHours < minHours || totalHours > maxHours) {
            throw new IllegalArgumentException(String.format(
                    "Nie można przydzielić %d godzin w %d dniach. Minimalna liczba godzin: %d, maksymalna liczba godzin: %d.",
                    totalHours, totalDays, minHours, maxHours));
        }
    }

    private void validateTotalDays(int totalDays, CalendarConfig calendar) throws IllegalArgumentException {
        if (totalDays <= 0) {
            throw new IllegalArgumentException("Niepoprawna wartość ilości dni pracy.");
        }

        Set<Integer> unavailableDays = new HashSet<>(calendar.closedDayIndices());
        unavailableDays.addAll(daysOff);
        int availableWorkingDays = calendar.daysInMonth() - unavailableDays.size();

        if (totalDays > availableWorkingDays) {
            throw new IllegalArgumentException(String.format(
                    "Nie można przydzielić %d dni pracy. Dostępnych dni po uwzględnieniu wolnego i zamknięć: %d.",
                    totalDays, availableWorkingDays));
        }
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getSection() {
        return section;
    }

    public int getTotalHours() {
        return totalHours;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public List<ShiftCombination> getShiftPool() {
        return shiftPool;
    }

    public List<Integer> getDaysOff() {
        return daysOff;
    }
}
