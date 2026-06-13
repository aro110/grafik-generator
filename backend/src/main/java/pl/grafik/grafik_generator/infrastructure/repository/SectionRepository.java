package pl.grafik.grafik_generator.infrastructure.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.grafik.grafik_generator.domain.entity.SectionEntity;

import java.util.List;
public interface SectionRepository extends JpaRepository<SectionEntity, Long> {

    @EntityGraph(attributePaths = "employees")
    List<SectionEntity> findAllByOrderByNameAsc();
}
