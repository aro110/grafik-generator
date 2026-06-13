package pl.grafik.grafik_generator.application.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import pl.grafik.grafik_generator.domain.context.VacationConfig;

public record StoreLevelSummaryDto(
        Long groupId,
        Long configId,
        LocalDate yearMonth,
        List<StoreDaySummaryDto> days,
        List<Long> scheduleIds) {

    public record StoreDaySummaryDto(
            Integer day,
            LocalDate date,
            Boolean closedDay,
            Integer totalRequired,
            Integer totalActual,
            Integer percentage,
            List<SectionDaySummaryDto> sections,
            List<HourlyCoverageDto> hourlyBreakdown) {
    }

    public record SectionDaySummaryDto(
            Long sectionId,
            String sectionName,
            Integer working) {
    }

    public record HourlyCoverageDto(
            Integer hour,
            Map<Long, Integer> employeesBySection) {
    }
}
