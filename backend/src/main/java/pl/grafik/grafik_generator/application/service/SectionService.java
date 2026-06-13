package pl.grafik.grafik_generator.application.service;

import pl.grafik.grafik_generator.application.dto.SectionDto;
import pl.grafik.grafik_generator.application.dto.request.CreateSectionRequest;
import pl.grafik.grafik_generator.application.dto.request.UpdateSectionRequest;

import java.util.List;

public interface SectionService {

    List<SectionDto> findAll();

    SectionDto findById(Long id);

    SectionDto create(CreateSectionRequest request);

    SectionDto update(Long id, UpdateSectionRequest request);

    void delete(Long id);
}
