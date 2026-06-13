package pl.grafik.grafik_generator.application.service;

import pl.grafik.grafik_generator.application.dto.GenerationRunDto;
import pl.grafik.grafik_generator.application.dto.GenerationRunGroupDto;
import pl.grafik.grafik_generator.application.dto.request.FullGenerationRequest;
import pl.grafik.grafik_generator.application.dto.request.StartGenerationRequest;

import java.util.List;

public interface GenerationService {

    GenerationRunGroupDto start(StartGenerationRequest request);

    GenerationRunGroupDto startFull(FullGenerationRequest request);

    GenerationRunGroupDto regenerate(Long groupId);

    GenerationRunDto findById(Long id);

    GenerationRunGroupDto findGroupById(Long id);

    List<GenerationRunDto> findByConfigId(Long configId);

    void deleteGroup(Long groupId);
}
