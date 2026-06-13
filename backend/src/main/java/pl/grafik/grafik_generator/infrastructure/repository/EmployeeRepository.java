package pl.grafik.grafik_generator.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.grafik.grafik_generator.domain.entity.EmployeeEntity;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {

    List<EmployeeEntity> findBySectionId(Long sectionId);
}
