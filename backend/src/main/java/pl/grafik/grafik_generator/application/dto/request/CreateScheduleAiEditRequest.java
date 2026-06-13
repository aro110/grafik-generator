package pl.grafik.grafik_generator.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateScheduleAiEditRequest(
        @NotBlank(message = "Polecenie dla AI jest wymagane.")
        @Size(max = 2000, message = "Polecenie dla AI może mieć maksymalnie 2000 znaków.")
        String instruction,
        Boolean allowProtectedDateChanges) {
}
