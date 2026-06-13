package pl.grafik.grafik_generator.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSectionRequest(
        @NotBlank @Size(max = 100) String name) {
}
