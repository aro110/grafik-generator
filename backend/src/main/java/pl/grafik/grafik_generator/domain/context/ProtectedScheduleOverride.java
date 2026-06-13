package pl.grafik.grafik_generator.domain.context;

public record ProtectedScheduleOverride(Long employeeId, int day, String type) {
    public static final String ALLOW_WORK = "ALLOW_WORK";
}
