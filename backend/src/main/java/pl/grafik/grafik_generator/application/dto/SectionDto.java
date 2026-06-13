package pl.grafik.grafik_generator.application.dto;

import java.time.Instant;
import java.util.List;

public record SectionDto(
        Long id,
        String name,
        List<EmployeeDto> employees,
        Instant createdAt,
        Instant updatedAt) {
}
