package pl.grafik.grafik_generator.application.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.grafik.grafik_generator.application.dto.GenerationRunGroupDto;
import pl.grafik.grafik_generator.domain.entity.GenerationRunGroupEntity;

@Component
@RequiredArgsConstructor
public class GenerationRunGroupMapper {

    private final GenerationRunMapper runMapper;

    public GenerationRunGroupDto toDto(GenerationRunGroupEntity entity) {
        return new GenerationRunGroupDto(
                entity.getId(),
                entity.getConfig() == null ? null : entity.getConfig().getId(),
                entity.getStatus(),
                entity.getSeed(),
                entity.getCreatedAt(),
                entity.getFinishedAt(),
                entity.getRuns() == null ? null : entity.getRuns().stream().map(runMapper::toDto).toList()
        );
    }
}
