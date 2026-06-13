package pl.grafik.grafik_generator.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.grafik.grafik_generator.application.dto.DashboardDto;
import pl.grafik.grafik_generator.application.service.DashboardService;
import pl.grafik.grafik_generator.domain.entity.GenerationRunEntity;
import pl.grafik.grafik_generator.domain.entity.GenerationRunGroupEntity;
import pl.grafik.grafik_generator.domain.entity.GenerationRunStatus;
import pl.grafik.grafik_generator.domain.entity.ScheduleEntity;
import pl.grafik.grafik_generator.infrastructure.repository.GenerationRunGroupRepository;
import pl.grafik.grafik_generator.infrastructure.repository.ScheduleRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int RECENT_GENERATIONS_LIMIT = 10;
    private static final int RECENT_PUBLISHED_LIMIT = 10;

    private final GenerationRunGroupRepository generationRunGroupRepository;
    private final ScheduleRepository scheduleRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardDto getDashboard() {
        List<GenerationRunGroupEntity> recentGroups = generationRunGroupRepository.findTop10ByOrderByCreatedAtDesc();

        List<DashboardDto.RecentGenerationDto> recentGenerationDtos = recentGroups.stream()
                .limit(RECENT_GENERATIONS_LIMIT)
                .map(group -> {
                    int totalSections = group.getRuns() == null ? 0 : group.getRuns().size();
                    int successSections = 0;
                    int failedSections = 0;
                    int totalProgress = 0;

                    if (group.getRuns() != null) {
                        for (GenerationRunEntity run : group.getRuns()) {
                            if (run.getStatus() == GenerationRunStatus.SUCCESS)
                                successSections++;
                            if (run.getStatus() == GenerationRunStatus.FAILED)
                                failedSections++;
                            totalProgress += run.getProgress() != null ? run.getProgress() : 0;
                        }
                    }

                    int progress = totalSections == 0 ? 0 : totalProgress / totalSections;

                    GenerationRunStatus groupStatus = GenerationRunStatus.PENDING;
                    if (totalSections > 0) {
                        if (successSections == totalSections) {
                            groupStatus = GenerationRunStatus.SUCCESS;
                        } else if (failedSections > 0) {
                            groupStatus = GenerationRunStatus.FAILED;
                        } else if (successSections > 0 || totalProgress > 0) {
                            groupStatus = GenerationRunStatus.RUNNING;
                        }
                    }

                    return new DashboardDto.RecentGenerationDto(
                            group.getId(),
                            group.getConfig() == null ? null : group.getConfig().getId(),
                            group.getConfig() == null ? null : group.getConfig().getName(),
                            groupStatus,
                            progress,
                            group.getCreatedAt(),
                            group.getFinishedAt(),
                            totalSections,
                            successSections,
                            failedSections);
                })
                .toList();

        // Published schedules - grouped by config / group
        List<ScheduleEntity> publishedSchedules = scheduleRepository.findByPublishedTrueOrderByPublishedAtDesc();

        // We'll group them by group_id and show the average fitness
        Map<Long, List<ScheduleEntity>> groupedPublished = publishedSchedules.stream()
                .filter(s -> s.getRun() != null && s.getRun().getGroup() != null)
                .collect(Collectors.groupingBy(s -> s.getRun().getGroup().getId()));

        List<DashboardDto.PublishedScheduleDto> publishedScheduleDtos = groupedPublished.entrySet().stream()
                .limit(RECENT_PUBLISHED_LIMIT)
                .map(entry -> {
                    Long groupId = entry.getKey();
                    List<ScheduleEntity> schedules = entry.getValue();
                    ScheduleEntity first = schedules.get(0);

                    double avgFitness = schedules.stream().mapToDouble(ScheduleEntity::getFitness).average()
                            .orElse(0.0);

                    return new DashboardDto.PublishedScheduleDto(
                            groupId,
                            first.getRun().getConfig().getId(),
                            first.getRun().getConfig().getName(),
                            first.getRun().getConfig().getYearMonth(),
                            avgFitness,
                            first.getPublishedAt());
                })
                .sorted((a, b) -> b.publishedAt().compareTo(a.publishedAt()))
                .toList();

        log.info("Pobrano dane dashboardu: {} grup generacji, {} opublikowanych grafikow",
                recentGenerationDtos.size(),
                publishedScheduleDtos.size());

        return new DashboardDto(recentGenerationDtos, publishedScheduleDtos);
    }
}
