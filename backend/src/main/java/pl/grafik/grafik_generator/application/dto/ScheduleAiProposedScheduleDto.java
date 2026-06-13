package pl.grafik.grafik_generator.application.dto;

import pl.grafik.grafik_generator.domain.context.ProtectedScheduleOverride;

import java.util.List;

public record ScheduleAiProposedScheduleDto(
        Long sourceScheduleId,
        List<List<Integer>> genes,
        List<List<Integer>> shiftStarts,
        List<ProtectedScheduleOverride> protectedOverrides) {
}
