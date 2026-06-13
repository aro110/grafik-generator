package pl.grafik.grafik_generator.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSectionRequest(
        @NotBlank @Size(max = 100) String name) {
}
