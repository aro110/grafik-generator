package pl.grafik.grafik_generator.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StartGenerationRequest(
        @NotNull Long configId,
        @NotEmpty List<Long> sectionIds,
        Long seed) {
}
