package pl.grafik.grafik_generator.application.service.impl;

import org.springframework.stereotype.Component;
import pl.grafik.grafik_generator.application.dto.ScheduleDetailsDto;
import pl.grafik.grafik_generator.domain.context.ProtectedScheduleOverride;
import pl.grafik.grafik_generator.domain.entity.EmployeeEntity;
import pl.grafik.grafik_generator.domain.entity.GenerationRunEntity;
import pl.grafik.grafik_generator.domain.entity.ScheduleConfigEntity;
import pl.grafik.grafik_generator.domain.entity.ScheduleEntity;
import pl.grafik.grafik_generator.domain.entity.SectionEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ScheduleDetailsAssembler {

    public ScheduleDetailsDto build(ScheduleEntity schedule) {
        GenerationRunEntity run = schedule.getRun();
        if (run == null) {
            throw new IllegalStateException("Grafik nie ma powiązanego runa generacji.");
        }

        ScheduleConfigEntity config = run.getConfig();
        if (config == null) {
            throw new IllegalStateException("Run generacji nie ma konfiguracji grafiku.");
        }

        SectionEntity section = run.getSection();
        if (section == null) {
            throw new IllegalStateException("Run generacji nie ma przypisanej sekcji.");
        }

        List<EmployeeEntity> employees = section.getEmployees() == null
                ? List.of()
                : section.getEmployees().stream()
                        .sorted(Comparator.comparing(EmployeeEntity::getId))
                        .toList();

        if (employees.isEmpty()) {
            throw new IllegalStateException("Sekcja nie ma pracowników do zbudowania widoku grafiku.");
        }

        if (schedule.getGenes() == null || schedule.getShiftStarts() == null) {
            throw new IllegalStateException("Grafik nie zawiera kompletnych danych zmian.");
        }

        int daysInMonth = config.getCalendar().daysInMonth();
        if (schedule.getGenes().size() != employees.size() || schedule.getShiftStarts().size() != employees.size()) {
            throw new IllegalStateException("Dane grafiku nie zgadzają się z liczbą pracowników sekcji.");
        }

        List<ScheduleDetailsDto.EmployeeScheduleRowDto> employeeRows = new ArrayList<>();
        for (int employeeIndex = 0; employeeIndex < employees.size(); employeeIndex++) {
            EmployeeEntity employee = employees.get(employeeIndex);
            List<String> shifts = new ArrayList<>();

            for (int dayIndex = 0; dayIndex < daysInMonth; dayIndex++) {
                shifts.add(buildShiftLabel(schedule, config, employee, employeeIndex, dayIndex));
            }

            employeeRows.add(new ScheduleDetailsDto.EmployeeScheduleRowDto(
                    employee.getId(),
                    employee.getName(),
                    employee.getSurname(),
                    employee.getTotalHours(),
                    employee.getTotalDays(),
                    shifts));
        }

        List<ScheduleDetailsDto.DailyCoverageDto> coverage = new ArrayList<>();
        for (int dayIndex = 0; dayIndex < daysInMonth; dayIndex++) {
            LocalDate date = config.getCalendar().dateAt(dayIndex);
            boolean closed = config.getCalendar().isClosedDay(dayIndex);

            int required = 0;
            int actual = 0;

            if (!closed) {
                required = config.getStaffingTargets().requiredEmployees(date.getDayOfWeek(), employees.size());
                actual = countWorkingEmployees(schedule, dayIndex);
            }

            coverage.add(new ScheduleDetailsDto.DailyCoverageDto(
                    dayIndex + 1,
                    date,
                    required,
                    actual,
                    closed || required <= 0 ? null : (int) Math.round((actual * 100.0) / required),
                    closed));
        }

        return new ScheduleDetailsDto(
                schedule.getId(),
                run.getId(),
                config.getId(),
                section.getId(),
                config.getName(),
                section.getName(),
                config.getYearMonth(),
                schedule.getFitness(),
                schedule.getPublished(),
                schedule.getPublishedAt(),
                schedule.getCreatedAt() == null ? Instant.now() : schedule.getCreatedAt(),
                employeeRows,
                coverage);
    }

    private String buildShiftLabel(ScheduleEntity schedule, ScheduleConfigEntity config, EmployeeEntity employee,
            int employeeIndex, int dayIndex) {
        if (config.getCalendar().isClosedDay(dayIndex)) {
            return "W";
        }

        boolean protectedWorkOverride = hasProtectedWorkOverride(schedule, employee, dayIndex);
        if (!protectedWorkOverride
                && employee != null
                && employee.getVacations() != null
                && employee.getVacations().contains(dayIndex)) {
            return "U";
        }

        Integer shiftLength = schedule.getGenes().get(employeeIndex).get(dayIndex);
        if (shiftLength == null || shiftLength <= 0) {
            return "W";
        }

        Integer startHour = schedule.getShiftStarts().get(employeeIndex).get(dayIndex);
        if (startHour == null || startHour <= 0) {
            startHour = config.getStoreHours().openHour(config.getCalendar().dayOfWeekAt(dayIndex));
        }

        int endHour = startHour + shiftLength;
        return "%02d:00-%02d:00".formatted(startHour, endHour);
    }

    private boolean hasProtectedWorkOverride(ScheduleEntity schedule, EmployeeEntity employee, int dayIndex) {
        if (employee == null || schedule.getProtectedOverrides() == null) {
            return false;
        }
        Set<String> keys = schedule.getProtectedOverrides().stream()
                .filter(override -> ProtectedScheduleOverride.ALLOW_WORK.equals(override.type()))
                .map(override -> override.employeeId() + ":" + override.day())
                .collect(Collectors.toSet());
        return keys.contains(employee.getId() + ":" + (dayIndex + 1));
    }

    private int countWorkingEmployees(ScheduleEntity schedule, int dayIndex) {
        int count = 0;
        for (List<Integer> row : schedule.getGenes()) {
            Integer value = row.get(dayIndex);
            if (value != null && value > 0) {
                count++;
            }
        }
        return count;
    }
}
