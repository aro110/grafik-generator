package pl.grafik.grafik_generator.application.mapper;

import org.springframework.stereotype.Component;
import pl.grafik.grafik_generator.application.dto.ScheduleDto;
import pl.grafik.grafik_generator.domain.entity.ScheduleEntity;

@Component
public class ScheduleMapper {

    public ScheduleDto toDto(ScheduleEntity entity) {
        return new ScheduleDto(
                entity.getId(),
                entity.getRun() == null ? null : entity.getRun().getId(),
                entity.getFitness(),
                entity.getGenes(),
                entity.getShiftStarts(),
                entity.getPublished(),
                entity.getPublishedAt(),
                entity.getCreatedAt());
    }
}
