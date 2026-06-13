package pl.grafik.grafik_generator.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ScheduleDetailsDto(
        Long id,
        Long runId,
        Long configId,
        Long sectionId,
        String configName,
        String sectionName,
        LocalDate yearMonth,
        Double fitness,
        Boolean published,
        Instant publishedAt,
        Instant createdAt,
        List<EmployeeScheduleRowDto> employees,
        List<DailyCoverageDto> coverage) {

    public record EmployeeScheduleRowDto(
            Long employeeId,
            String name,
            String surname,
            Integer totalHours,
            Integer totalDays,
            List<String> shifts) {
    }

    public record DailyCoverageDto(
            Integer day,
            LocalDate date,
            Integer required,
            Integer actual,
            Integer percentage,
            Boolean closedDay) {
    }
}
