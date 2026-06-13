package pl.grafik.grafik_generator.application.dto;

public record ScheduleAiChangeDto(
        Long employeeId,
        Integer day,
        Integer shiftLength,
        Integer startHour,
        String reason,
        Long scheduleId,
        Long sectionId) {
}
