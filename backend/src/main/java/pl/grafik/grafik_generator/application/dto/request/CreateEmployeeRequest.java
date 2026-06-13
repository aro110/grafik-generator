package pl.grafik.grafik_generator.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateEmployeeRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String surname,
        @NotNull Long sectionId,
        @NotNull @Min(0) Integer totalHours,
        @NotNull @Min(0) Integer totalDays,
        List<Integer> daysOff,
        List<Integer> vacations) {
}
