package pl.grafik.grafik_generator.application.dto;

public record ScheduleExportDto(
        String fileName,
        String contentType,
        byte[] content) {
}
