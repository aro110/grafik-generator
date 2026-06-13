package pl.grafik.grafik_generator.application.mapper;

import org.springframework.stereotype.Component;
import pl.grafik.grafik_generator.application.dto.GenerationRunDto;
import pl.grafik.grafik_generator.domain.entity.GenerationRunEntity;

@Component
public class GenerationRunMapper {

    public GenerationRunDto toDto(GenerationRunEntity entity) {
        return new GenerationRunDto(
                entity.getId(),
                entity.getConfig() == null ? null : entity.getConfig().getId(),
                entity.getSection() == null ? null : entity.getSection().getId(),
                entity.getSection() == null ? null : entity.getSection().getName(),
                entity.getGroup() == null ? null : entity.getGroup().getId(),
                entity.getStatus(),
                entity.getSeed(),
                entity.getProgress(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getErrorMessage(),
                entity.getCreatedAt());
    }
}
