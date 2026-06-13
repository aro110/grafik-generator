package pl.grafik.grafik_generator.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.grafik.grafik_generator.application.exception.NotFoundException;
import pl.grafik.grafik_generator.application.mapper.DomainMapper;
import pl.grafik.grafik_generator.application.mapper.ScheduleConfigMapper;
import pl.grafik.grafik_generator.domain.context.GaParameters;
import pl.grafik.grafik_generator.domain.context.GenerationContext;
import pl.grafik.grafik_generator.domain.entity.GenerationRunEntity;
import pl.grafik.grafik_generator.domain.entity.GenerationRunGroupEntity;
import pl.grafik.grafik_generator.domain.entity.GenerationRunGroupStatus;
import pl.grafik.grafik_generator.domain.entity.GenerationRunStatus;
import pl.grafik.grafik_generator.domain.entity.ScheduleConfigEntity;
import pl.grafik.grafik_generator.domain.entity.ScheduleEntity;
import pl.grafik.grafik_generator.domain.entity.SectionEntity;
import pl.grafik.grafik_generator.domain.ga.Population;
import pl.grafik.grafik_generator.domain.model.Employee;
import pl.grafik.grafik_generator.domain.model.Section;
import pl.grafik.grafik_generator.domain.scheduleGenerator.Schedule;
import pl.grafik.grafik_generator.domain.scheduleGenerator.ShiftAssigner;
import pl.grafik.grafik_generator.infrastructure.repository.GenerationRunGroupRepository;
import pl.grafik.grafik_generator.infrastructure.repository.GenerationRunRepository;
import pl.grafik.grafik_generator.infrastructure.repository.ScheduleConfigRepository;
import pl.grafik.grafik_generator.infrastructure.repository.ScheduleRepository;
import pl.grafik.grafik_generator.infrastructure.repository.SectionRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class GenerationExecutor {

    private static final Logger log = LoggerFactory.getLogger(GenerationExecutor.class);

    private final GenerationRunRepository runRepository;
    private final GenerationRunGroupRepository groupRepository;
    private final ScheduleConfigRepository configRepository;
    private final SectionRepository sectionRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleConfigMapper configMapper;
    private final DomainMapper domainMapper;

    public GenerationExecutor(GenerationRunRepository runRepository,
            GenerationRunGroupRepository groupRepository,
            ScheduleConfigRepository configRepository,
            SectionRepository sectionRepository,
            ScheduleRepository scheduleRepository,
            ScheduleConfigMapper configMapper,
            DomainMapper domainMapper) {
        this.runRepository = runRepository;
        this.groupRepository = groupRepository;
        this.configRepository = configRepository;
        this.sectionRepository = sectionRepository;
        this.scheduleRepository = scheduleRepository;
        this.configMapper = configMapper;
        this.domainMapper = domainMapper;
    }

    @Async("generationTaskExecutor")
    @Transactional
    public void execute(Long runId, Long sectionId) {
        GenerationRunEntity run = runRepository.findById(runId)
                .orElseThrow(() -> NotFoundException.of("Run generacji", runId));
        try {
            run.setStatus(GenerationRunStatus.RUNNING);
            run.setStartedAt(Instant.now());
            runRepository.save(run);
            refreshGroupStatus(run);

            ScheduleConfigEntity config = run.getConfig();
            SectionEntity sectionEntity = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> NotFoundException.of("Sekcja", sectionId));

            GenerationContext ctx = configMapper.toContext(config);
            Section section = domainMapper.toDomainSection(
                    sectionEntity, ctx.shiftRules(), ctx.calendar(), ctx.vacationConfig());

            Random random = new Random(run.getSeed());
            GaParameters params = ctx.gaParameters();
            Population population = new Population(section, params.populationSize(), ctx, random);
            Schedule best = population.run(params.generations(), params.eliteCount(), params.tournamentSize());

            ScheduleEntity scheduleEntity = buildScheduleEntity(best, section, ctx, run);
            scheduleRepository.save(scheduleEntity);

            run.setStatus(GenerationRunStatus.SUCCESS);
            run.setProgress(100);
            run.setFinishedAt(Instant.now());
            runRepository.save(run);
            refreshGroupStatus(run);
        } catch (Exception e) {
            log.error("Generacja {} zakończona błędem", runId, e);
            run.setStatus(GenerationRunStatus.FAILED);
            run.setErrorMessage(e.getMessage());
            run.setFinishedAt(Instant.now());
            runRepository.save(run);
            refreshGroupStatus(run);
        }
    }

    private void refreshGroupStatus(GenerationRunEntity run) {
        if (run.getGroup() == null || run.getGroup().getId() == null) {
            return;
        }

        Long groupId = run.getGroup().getId();
        runRepository.flush();

        GenerationRunGroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> NotFoundException.of("Grupa generacji", run.getGroup().getId()));
        List<GenerationRunEntity> runs = runRepository.findByGroupId(groupId);

        boolean anyRunning = runs.stream()
                .anyMatch(r -> r.getStatus() == GenerationRunStatus.RUNNING || r.getStatus() == GenerationRunStatus.PENDING);
        boolean anyFailed = runs.stream().anyMatch(r -> r.getStatus() == GenerationRunStatus.FAILED);
        boolean anySuccess = runs.stream().anyMatch(r -> r.getStatus() == GenerationRunStatus.SUCCESS);

        if (anyRunning) {
            group.setStatus(GenerationRunGroupStatus.RUNNING);
            group.setFinishedAt(null);
        } else if (anyFailed && anySuccess) {
            group.setStatus(GenerationRunGroupStatus.PARTIAL);
            group.setFinishedAt(Instant.now());
        } else if (anyFailed) {
            group.setStatus(GenerationRunGroupStatus.FAILED);
            group.setFinishedAt(Instant.now());
        } else {
            group.setStatus(GenerationRunGroupStatus.SUCCESS);
            group.setFinishedAt(Instant.now());
        }

        groupRepository.save(group);
    }

    private ScheduleEntity buildScheduleEntity(Schedule best, Section section,
            GenerationContext ctx, GenerationRunEntity run) {
        int daysInMonth = best.getDaysInMonth();
        int employees = best.getEmployees();

        List<List<Integer>> genes = new ArrayList<>(employees);
        for (int i = 0; i < employees; i++) {
            List<Integer> row = new ArrayList<>(daysInMonth);
            for (int d = 0; d < daysInMonth; d++) {
                row.add(best.getGenes()[i][d]);
            }
            genes.add(row);
        }

        ShiftAssigner assigner = new ShiftAssigner(ctx);
        List<List<Integer>> starts = new ArrayList<>(employees);
        for (int i = 0; i < employees; i++) {
            starts.add(new ArrayList<>(java.util.Collections.nCopies(daysInMonth, 0)));
        }
        for (int d = 0; d < daysInMonth; d++) {
            if (ctx.calendar().isClosedDay(d))
                continue;
            Map<Employee, Integer> assignments = assigner.assignStartTimes(best, section, d);
            for (int i = 0; i < employees; i++) {
                Employee emp = section.getEmployees().get(i);
                Integer startHour = assignments.get(emp);
                if (startHour != null) {
                    starts.get(i).set(d, startHour);
                }
            }
        }

        ScheduleEntity scheduleEntity = new ScheduleEntity();
        scheduleEntity.setRun(run);
        scheduleEntity.setFitness(best.getFitness());
        scheduleEntity.setGenes(genes);
        scheduleEntity.setShiftStarts(starts);
        scheduleEntity.setPublished(false);
        return scheduleEntity;
    }
}
