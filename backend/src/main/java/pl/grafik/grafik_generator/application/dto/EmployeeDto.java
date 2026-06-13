package pl.grafik.grafik_generator.application.dto;

import java.time.Instant;
import java.util.List;

public record EmployeeDto(
        Long id,
        String name,
        String surname,
        Long sectionId,
        Integer totalHours,
        Integer totalDays,
        List<Integer> daysOff,
        List<Integer> vacations,
        Instant createdAt,
        Instant updatedAt) {
}
