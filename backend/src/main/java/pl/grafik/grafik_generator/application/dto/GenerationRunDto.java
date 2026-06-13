package pl.grafik.grafik_generator.application.dto;

import pl.grafik.grafik_generator.domain.entity.GenerationRunStatus;

import java.time.Instant;

public record GenerationRunDto(
        Long id,
        Long configId,
        Long sectionId,
        String sectionName,
        Long groupId,
        GenerationRunStatus status,
        Long seed,
        Integer progress,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage,
        Instant createdAt) {
}
