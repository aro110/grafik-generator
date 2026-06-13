package pl.grafik.grafik_generator.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pl.grafik.grafik_generator.application.dto.GenerationRunDto;
import pl.grafik.grafik_generator.application.dto.GenerationRunGroupDto;
import pl.grafik.grafik_generator.application.dto.ScheduleConfigDto;
import pl.grafik.grafik_generator.application.dto.SectionDto;
import pl.grafik.grafik_generator.application.dto.request.CreateEmployeeRequest;
import pl.grafik.grafik_generator.application.dto.request.CreateSectionRequest;
import pl.grafik.grafik_generator.application.dto.request.FullGenerationRequest;
import pl.grafik.grafik_generator.application.dto.request.StartGenerationRequest;
import pl.grafik.grafik_generator.application.exception.NotFoundException;
import pl.grafik.grafik_generator.application.mapper.GenerationRunGroupMapper;
import pl.grafik.grafik_generator.application.mapper.GenerationRunMapper;
import pl.grafik.grafik_generator.application.service.EmployeeService;
import pl.grafik.grafik_generator.application.service.GenerationService;
import pl.grafik.grafik_generator.application.service.ScheduleConfigService;
import pl.grafik.grafik_generator.application.service.SectionService;
import pl.grafik.grafik_generator.domain.entity.GenerationRunEntity;
import pl.grafik.grafik_generator.domain.entity.GenerationRunGroupEntity;
import pl.grafik.grafik_generator.domain.entity.GenerationRunGroupStatus;
import pl.grafik.grafik_generator.domain.entity.GenerationRunStatus;
import pl.grafik.grafik_generator.domain.entity.ScheduleConfigEntity;
import pl.grafik.grafik_generator.domain.entity.SectionEntity;
import pl.grafik.grafik_generator.infrastructure.repository.GenerationRunGroupRepository;
import pl.grafik.grafik_generator.infrastructure.repository.GenerationRunRepository;
import pl.grafik.grafik_generator.infrastructure.repository.ScheduleConfigRepository;
import pl.grafik.grafik_generator.infrastructure.repository.SectionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class GenerationServiceImpl implements GenerationService {

    private final GenerationRunRepository runRepository;
    private final GenerationRunGroupRepository groupRepository;
    private final ScheduleConfigRepository configRepository;
    private final SectionRepository sectionRepository;
    private final GenerationRunMapper runMapper;
    private final GenerationRunGroupMapper groupMapper;
    private final GenerationExecutor generationExecutor;
    private final ScheduleConfigService scheduleConfigService;
    private final SectionService sectionService;
    private final EmployeeService employeeService;

    @Override
    @Transactional
    public GenerationRunGroupDto startFull(FullGenerationRequest request) {
        ScheduleConfigDto configDto = scheduleConfigService.create(request.config());
        List<Long> sectionIds = new ArrayList<>();

        for (FullGenerationRequest.FullSectionRequest secReq : request.sections()) {
            SectionDto sectionDto = sectionService.create(new CreateSectionRequest(secReq.name()));
            sectionIds.add(sectionDto.id());

            for (FullGenerationRequest.FullEmployeeRequest empReq : secReq.employees()) {
                employeeService.create(new CreateEmployeeRequest(
                        empReq.name(),
                        empReq.surname(),
                        sectionDto.id(),
                        empReq.totalHours(),
                        empReq.totalDays(),
                        empReq.daysOff(),
                        empReq.vacations()));
            }
        }

        return start(new StartGenerationRequest(configDto.id(), sectionIds, request.seed()));
    }

    @Override
    @Transactional
    public GenerationRunGroupDto start(StartGenerationRequest request) {
        ScheduleConfigEntity config = configRepository.findById(request.configId())
                .orElseThrow(() -> NotFoundException.of("Konfiguracja grafiku", request.configId()));

        Long seed = request.seed() != null ? request.seed() : ThreadLocalRandom.current().nextLong();

        GenerationRunGroupEntity group = new GenerationRunGroupEntity();
        group.setConfig(config);
        group.setSeed(seed);
        group.setStatus(GenerationRunGroupStatus.PENDING);
        group = groupRepository.save(group);

        for (Long sectionId : request.sectionIds()) {
            SectionEntity section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> NotFoundException.of("Sekcja", sectionId));

            GenerationRunEntity run = new GenerationRunEntity();
            run.setConfig(config);
            run.setSection(section);
            run.setGroup(group);
            run.setStatus(GenerationRunStatus.PENDING);
            run.setProgress(0);
            run.setSeed(seed);
            run = runRepository.save(run);

            final Long finalRunId = run.getId();
            final Long finalSectionId = sectionId;

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    generationExecutor.execute(finalRunId, finalSectionId);
                }
            });
        }

        return groupMapper.toDto(group);
    }

    @Override
    @Transactional
    public GenerationRunGroupDto regenerate(Long groupId) {
        GenerationRunGroupEntity oldGroup = groupRepository.findDetailedById(groupId)
                .orElseThrow(() -> NotFoundException.of("Grupa generacji", groupId));

        List<Long> sectionIds = oldGroup.getRuns().stream()
                .filter(r -> r.getSection() != null)
                .map(r -> r.getSection().getId())
                .distinct()
                .toList();

        return start(new StartGenerationRequest(oldGroup.getConfig().getId(), sectionIds, null));
    }

    @Override
    @Transactional(readOnly = true)
    public GenerationRunDto findById(Long id) {
        GenerationRunEntity run = runRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Run generacji", id));
        return runMapper.toDto(run);
    }

    @Override
    @Transactional(readOnly = true)
    public GenerationRunGroupDto findGroupById(Long id) {
        GenerationRunGroupEntity group = groupRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Grupa generacji", id));
        return groupMapper.toDto(group);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GenerationRunDto> findByConfigId(Long configId) {
        return runRepository.findByConfigId(configId).stream()
                .map(runMapper::toDto).toList();
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId) {
        GenerationRunGroupEntity group = groupRepository.findDetailedById(groupId)
                .orElseThrow(() -> NotFoundException.of("Grupa generacji", groupId));

        Long configId = group.getConfig().getId();
        List<Long> sectionIds = group.getRuns().stream()
                .map(run -> run.getSection())
                .filter(Objects::nonNull)
                .map(SectionEntity::getId)
                .distinct()
                .toList();

        groupRepository.delete(group);

        if (configId != null) {
            configRepository.deleteById(configId);
        }

        sectionIds.forEach(sectionRepository::deleteById);
    }
}
