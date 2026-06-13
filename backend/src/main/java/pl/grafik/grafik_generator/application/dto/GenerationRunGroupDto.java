package pl.grafik.grafik_generator.application.dto;

import pl.grafik.grafik_generator.domain.entity.GenerationRunGroupStatus;

import java.time.Instant;
import java.util.List;

public record GenerationRunGroupDto(
        Long id,
        Long configId,
        GenerationRunGroupStatus status,
        Long seed,
        Instant createdAt,
        Instant finishedAt,
        List<GenerationRunDto> runs) {
}
