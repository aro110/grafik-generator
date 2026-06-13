package pl.grafik.grafik_generator.application.dto;

public record ScheduleDiffCellDto(
        Long employeeId,
        Integer day,
        String employeeName,
        String before,
        String after,
        String reason,
        Boolean protectedDate) {
}
