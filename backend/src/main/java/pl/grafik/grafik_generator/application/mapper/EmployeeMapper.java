package pl.grafik.grafik_generator.application.mapper;

import org.springframework.stereotype.Component;
import pl.grafik.grafik_generator.application.dto.EmployeeDto;
import pl.grafik.grafik_generator.domain.entity.EmployeeEntity;

import java.util.ArrayList;
import java.util.List;

@Component
public class EmployeeMapper {

    public EmployeeDto toDto(EmployeeEntity entity) {
        return new EmployeeDto(
                entity.getId(),
                entity.getName(),
                entity.getSurname(),
                entity.getSection() == null ? null : entity.getSection().getId(),
                entity.getTotalHours(),
                entity.getTotalDays(),
                entity.getDaysOff() == null ? List.of() : new ArrayList<>(entity.getDaysOff()),
                entity.getVacations() == null ? List.of() : new ArrayList<>(entity.getVacations()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
