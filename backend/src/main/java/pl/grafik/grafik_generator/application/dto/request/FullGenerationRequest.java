package pl.grafik.grafik_generator.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FullGenerationRequest(
        @NotNull @Valid CreateScheduleConfigRequest config,
        @NotEmpty @Valid List<FullSectionRequest> sections,
        Long seed) {

    public record FullSectionRequest(
            @NotBlank @Size(max = 100) String name,
            @NotEmpty @Valid List<FullEmployeeRequest> employees) {
    }

    public record FullEmployeeRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 100) String surname,
            @NotNull @Min(0) Integer totalHours,
            @NotNull @Min(0) Integer totalDays,
            List<Integer> daysOff,
            List<Integer> vacations) {
    }
}
