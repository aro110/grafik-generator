package pl.grafik.grafik_generator.application.mapper;

import org.springframework.stereotype.Component;
import pl.grafik.grafik_generator.application.dto.SectionDto;
import pl.grafik.grafik_generator.domain.entity.SectionEntity;

import java.util.List;

@Component
public class SectionMapper {

    private final EmployeeMapper employeeMapper;

    public SectionMapper(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    public SectionDto toDto(SectionEntity entity) {
        return new SectionDto(
                entity.getId(),
                entity.getName(),
                entity.getEmployees() == null ? List.of()
                        : entity.getEmployees().stream().map(employeeMapper::toDto).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
