package pl.grafik.grafik_generator.application.dto;

import pl.grafik.grafik_generator.domain.entity.ScheduleAiEditStatus;

import java.time.Instant;
import java.util.List;

public record ScheduleAiEditDto(
        Long editId,
        Long sourceScheduleId,
        Long sourceGroupId,
        Long acceptedScheduleId,
        Long acceptedGroupId,
        List<Long> acceptedScheduleIds,
        ScheduleAiEditStatus status,
        String instruction,
        String model,
        Boolean allowProtectedDateChanges,
        List<ScheduleAiChangeDto> changes,
        List<ScheduleDiffCellDto> diff,
        List<String> warnings,
        List<String> errors,
        ScheduleDetailsDto proposedSchedule,
        List<ScheduleDetailsDto> proposedSchedules,
        ScheduleDetailsDto acceptedSchedule,
        List<ScheduleDetailsDto> acceptedSchedules,
        Instant createdAt,
        Instant updatedAt) {
}
