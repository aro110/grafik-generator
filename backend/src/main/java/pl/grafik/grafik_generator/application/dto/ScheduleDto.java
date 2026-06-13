package pl.grafik.grafik_generator.application.dto;

import java.time.Instant;
import java.util.List;

public record ScheduleDto(
        Long id,
        Long runId,
        Double fitness,
        List<List<Integer>> genes,
        List<List<Integer>> shiftStarts,
        Boolean published,
        Instant publishedAt,
        Instant createdAt) {
}
