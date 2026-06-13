package pl.grafik.grafik_generator.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.grafik.grafik_generator.application.dto.SectionDto;
import pl.grafik.grafik_generator.application.dto.request.CreateSectionRequest;
import pl.grafik.grafik_generator.application.dto.request.UpdateSectionRequest;
import pl.grafik.grafik_generator.application.exception.NotFoundException;
import pl.grafik.grafik_generator.application.mapper.SectionMapper;
import pl.grafik.grafik_generator.application.service.SectionService;
import pl.grafik.grafik_generator.domain.entity.SectionEntity;
import pl.grafik.grafik_generator.infrastructure.repository.SectionRepository;

import java.util.List;

@Service
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final SectionMapper sectionMapper;

    public SectionServiceImpl(SectionRepository sectionRepository, SectionMapper sectionMapper) {
        this.sectionRepository = sectionRepository;
        this.sectionMapper = sectionMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionDto> findAll() {
        return sectionRepository.findAllByOrderByNameAsc().stream()
                .map(sectionMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SectionDto findById(Long id) {
        return sectionMapper.toDto(requireById(id));
    }

    @Override
    @Transactional
    public SectionDto create(CreateSectionRequest request) {
        SectionEntity entity = new SectionEntity();
        entity.setName(request.name());
        return sectionMapper.toDto(sectionRepository.save(entity));
    }

    @Override
    @Transactional
    public SectionDto update(Long id, UpdateSectionRequest request) {
        SectionEntity entity = requireById(id);
        entity.setName(request.name());
        return sectionMapper.toDto(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SectionEntity entity = requireById(id);
        sectionRepository.delete(entity);
    }

    private SectionEntity requireById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Sekcja", id));
    }
}
