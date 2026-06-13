package pl.grafik.grafik_generator.application.dto;

import pl.grafik.grafik_generator.domain.entity.GenerationRunStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DashboardDto(
        List<RecentGenerationDto> recentGenerations,
        List<PublishedScheduleDto> publishedSchedules) {

    public record RecentGenerationDto(
            Long groupId,
            Long configId,
            String configName,
            GenerationRunStatus status,
            Integer progress,
            Instant createdAt,
            Instant finishedAt,
            Integer totalSections,
            Integer successSections,
            Integer failedSections) {
    }

    public record PublishedScheduleDto(
            Long groupId,
            Long configId,
            String configName,
            LocalDate yearMonth,
            Double averageFitness,
            Instant publishedAt) {
    }
}
