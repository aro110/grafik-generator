package pl.grafik.grafik_generator.application.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.grafik.grafik_generator.application.dto.ScheduleDetailsDto;
import pl.grafik.grafik_generator.application.dto.ScheduleDto;
import pl.grafik.grafik_generator.application.dto.ScheduleExportDto;
import pl.grafik.grafik_generator.application.exception.NotFoundException;
import pl.grafik.grafik_generator.application.mapper.ScheduleMapper;
import pl.grafik.grafik_generator.application.service.ScheduleService;
import pl.grafik.grafik_generator.domain.entity.EmployeeEntity;
import pl.grafik.grafik_generator.domain.entity.GenerationRunEntity;
import pl.grafik.grafik_generator.domain.entity.ScheduleConfigEntity;
import pl.grafik.grafik_generator.domain.entity.ScheduleEntity;
import pl.grafik.grafik_generator.domain.entity.SectionEntity;
import pl.grafik.grafik_generator.infrastructure.repository.ScheduleRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import pl.grafik.grafik_generator.application.dto.StoreLevelSummaryDto;
import pl.grafik.grafik_generator.application.dto.StoreLevelSummaryDto.StoreDaySummaryDto;
import pl.grafik.grafik_generator.application.dto.StoreLevelSummaryDto.SectionDaySummaryDto;
import pl.grafik.grafik_generator.application.dto.StoreLevelSummaryDto.HourlyCoverageDto;

@Slf4j
@Service
public class ScheduleServiceImpl implements ScheduleService {

    private static final Locale POLISH = Locale.forLanguageTag("pl-PL");

    private final ScheduleRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;
    private final ScheduleExcelExporter scheduleExcelExporter;
    private final ScheduleDetailsAssembler scheduleDetailsAssembler;

    public ScheduleServiceImpl(
            ScheduleRepository scheduleRepository,
            ScheduleMapper scheduleMapper,
            ScheduleExcelExporter scheduleExcelExporter,
            ScheduleDetailsAssembler scheduleDetailsAssembler) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleMapper = scheduleMapper;
        this.scheduleExcelExporter = scheduleExcelExporter;
        this.scheduleDetailsAssembler = scheduleDetailsAssembler;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleDto> findByRunId(Long runId) {
        return scheduleRepository.findByRunId(runId).stream()
                .map(scheduleMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleDto findById(Long id) {
        return scheduleMapper.toDto(requireById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleDetailsDto findDetailsById(Long id) {
        ScheduleEntity schedule = requireDetailedById(id);
        return scheduleDetailsAssembler.build(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreLevelSummaryDto getStoreSummary(Long groupId) {
        List<ScheduleEntity> schedules = scheduleRepository.findByRunGroupId(groupId);

        if (schedules.isEmpty()) {
            throw NotFoundException.of("StoreSummary for Group", groupId);
        }

        // Use the first schedule's config as global configuration
        ScheduleConfigEntity config = schedules.get(0).getRun().getConfig();
        int daysInMonth = config.getCalendar().daysInMonth();

        List<StoreDaySummaryDto> days = new ArrayList<>();

        for (int dayIndex = 0; dayIndex < daysInMonth; dayIndex++) {
            LocalDate date = config.getCalendar().dateAt(dayIndex);
            boolean closed = config.getCalendar().isClosedDay(dayIndex);

            int totalRequired = 0;
            int totalActual = 0;
            List<SectionDaySummaryDto> sectionSummaries = new ArrayList<>();
            Map<Integer, Map<Long, Integer>> hourlyCounts = new HashMap<>();

            for (ScheduleEntity schedule : schedules) {
                SectionEntity section = schedule.getRun().getSection();
                int sectionEmployees = section.getEmployees() == null ? 0 : section.getEmployees().size();

                int sectionRequired = 0;
                int sectionActual = 0;

                if (!closed) {
                    var dayOfWeek = date.getDayOfWeek();
                    sectionRequired = config.getStaffingTargets().requiredEmployees(dayOfWeek, sectionEmployees);

                    List<EmployeeEntity> employees = section.getEmployees().stream()
                            .sorted(Comparator.comparing(EmployeeEntity::getId))
                            .toList();

                    for (int empIdx = 0; empIdx < employees.size(); empIdx++) {
                        Integer shiftLength = schedule.getGenes().get(empIdx).get(dayIndex);
                        if (shiftLength != null && shiftLength > 0) {
                            sectionActual++;

                            Integer startHour = schedule.getShiftStarts().get(empIdx).get(dayIndex);
                            if (startHour == null || startHour <= 0) {
                                startHour = config.getStoreHours().openHour(config.getCalendar().dayOfWeekAt(dayIndex));
                            }

                            for (int h = 0; h < shiftLength; h++) {
                                int hour = startHour + h;
                                hourlyCounts
                                        .computeIfAbsent(hour, k -> new HashMap<>())
                                        .merge(section.getId(), 1, Integer::sum);
                            }
                        }
                    }
                }

                totalRequired += sectionRequired;
                totalActual += sectionActual;
                sectionSummaries.add(new SectionDaySummaryDto(section.getId(), section.getName(), sectionActual));
            }

            Integer percentage = null;
            if (!closed && totalRequired > 0) {
                percentage = (int) Math.round((totalActual * 100.0) / totalRequired);
            }

            List<HourlyCoverageDto> hourlyBreakdown = hourlyCounts.entrySet().stream()
                    .map(e -> new HourlyCoverageDto(e.getKey(), e.getValue()))
                    .sorted(Comparator.comparing(HourlyCoverageDto::hour))
                    .toList();

            days.add(new StoreDaySummaryDto(
                    dayIndex + 1,
                    date,
                    closed,
                    totalRequired,
                    totalActual,
                    percentage,
                    sectionSummaries,
                    hourlyBreakdown));
        }

        return new StoreLevelSummaryDto(
                groupId,
                config.getId(),
                config.getYearMonth(),
                days,
                schedules.stream().map(ScheduleEntity::getId).toList());
    }

    @Override
    @Transactional
    public ScheduleDto publish(Long id) {
        ScheduleEntity entity = requireById(id);
        entity.setPublished(true);
        entity.setPublishedAt(Instant.now());

        log.info("Opublikowano grafik {}", id);
        return scheduleMapper.toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleExportDto exportToExcel(Long id) {
        ScheduleEntity schedule = requireDetailedById(id);
        ScheduleExcelExporter.ExportedScheduleFile exportedFile = scheduleExcelExporter.export(schedule);

        log.info("Przygotowano eksport Excel dla grafiku {}", id);

        return new ScheduleExportDto(
                exportedFile.fileName(),
                exportedFile.contentType(),
                exportedFile.content());
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleExportDto exportGroupToExcel(Long groupId) {
        List<ScheduleEntity> schedules = scheduleRepository.findByRunGroupId(groupId);
        if (schedules.isEmpty()) {
            throw NotFoundException.of("StoreSummary for Group", groupId);
        }

        ScheduleExcelExporter.ExportedScheduleFile exportedFile = scheduleExcelExporter.exportGroup(schedules, groupId);

        log.info("Przygotowano eksport Excel dla grupy generacji {}", groupId);

        return new ScheduleExportDto(
                exportedFile.fileName(),
                exportedFile.contentType(),
                exportedFile.content());
    }

    private ScheduleDetailsDto buildScheduleDetails(ScheduleEntity schedule) {
        GenerationRunEntity run = schedule.getRun();
        if (run == null) {
            throw new IllegalStateException("Grafik nie ma powiązanego runa generacji.");
        }

        ScheduleConfigEntity config = run.getConfig();
        if (config == null) {
            throw new IllegalStateException("Run generacji nie ma konfiguracji grafiku.");
        }

        SectionEntity section = run.getSection();
        if (section == null) {
            throw new IllegalStateException("Run generacji nie ma przypisanej sekcji.");
        }

        List<EmployeeEntity> employees = section.getEmployees() == null
                ? List.of()
                : section.getEmployees().stream()
                        .sorted(Comparator.comparing(EmployeeEntity::getId))
                        .toList();

        if (employees.isEmpty()) {
            throw new IllegalStateException("Sekcja nie ma pracowników do zbudowania widoku grafiku.");
        }

        if (schedule.getGenes() == null || schedule.getShiftStarts() == null) {
            throw new IllegalStateException("Grafik nie zawiera kompletnych danych zmian.");
        }

        int daysInMonth = config.getCalendar().daysInMonth();
        if (schedule.getGenes().size() != employees.size() || schedule.getShiftStarts().size() != employees.size()) {
            throw new IllegalStateException("Dane grafiku nie zgadzają się z liczbą pracowników sekcji.");
        }

        List<EmployeeScheduleRow> employeeRows = new ArrayList<>();
        for (int employeeIndex = 0; employeeIndex < employees.size(); employeeIndex++) {
            EmployeeEntity employee = employees.get(employeeIndex);
            List<String> shifts = new ArrayList<>();

            for (int dayIndex = 0; dayIndex < daysInMonth; dayIndex++) {
                shifts.add(buildShiftLabel(schedule, config, employee, employeeIndex, dayIndex));
            }

            employeeRows.add(new EmployeeScheduleRow(
                    employee.getId(),
                    employee.getName(),
                    employee.getSurname(),
                    employee.getTotalHours(),
                    employee.getTotalDays(),
                    shifts));
        }

        List<DayCoverageRow> coverage = new ArrayList<>();
        for (int dayIndex = 0; dayIndex < daysInMonth; dayIndex++) {
            LocalDate date = config.getCalendar().dateAt(dayIndex);
            boolean closed = config.getCalendar().isClosedDay(dayIndex);

            int required = 0;
            int actual = 0;

            if (!closed) {
                var dayOfWeek = date.getDayOfWeek();
                required = config.getStaffingTargets().requiredEmployees(dayOfWeek, employees.size());
                actual = countWorkingEmployees(schedule, dayIndex);
            }

            coverage.add(new DayCoverageRow(
                    dayIndex + 1,
                    date,
                    date.getDayOfWeek().getDisplayName(TextStyle.SHORT, POLISH),
                    closed,
                    required,
                    actual));
        }

        return new ScheduleDetailsDto(
                schedule.getId(),
                run.getId(),
                config.getId(),
                section.getId(),
                config.getName(),
                section.getName(),
                config.getYearMonth(),
                schedule.getFitness(),
                schedule.getPublished(),
                schedule.getPublishedAt(),
                schedule.getCreatedAt(),
                employeeRows.stream()
                        .map(row -> new ScheduleDetailsDto.EmployeeScheduleRowDto(
                                row.id(),
                                row.name(),
                                row.surname(),
                                row.totalHours(),
                                row.totalDays(),
                                row.shifts()))
                        .toList(),
                coverage.stream()
                        .map(row -> new ScheduleDetailsDto.DailyCoverageDto(
                                row.day(),
                                row.date(),
                                row.required(),
                                row.actual(),
                                row.closed() || row.required() <= 0
                                        ? null
                                        : (int) Math.round((row.actual() * 100.0) / row.required()),
                                row.closed()))
                        .toList());
    }

    private String buildShiftLabel(ScheduleEntity schedule, ScheduleConfigEntity config, EmployeeEntity employee,
            int employeeIndex,
            int dayIndex) {
        if (config.getCalendar().isClosedDay(dayIndex)) {
            return "W";
        }

        if (employee != null && employee.getVacations() != null && employee.getVacations().contains(dayIndex)) {
            return "U";
        }

        Integer shiftLength = schedule.getGenes().get(employeeIndex).get(dayIndex);
        if (shiftLength == null || shiftLength <= 0) {
            return "W";
        }

        Integer startHour = schedule.getShiftStarts().get(employeeIndex).get(dayIndex);
        if (startHour == null || startHour <= 0) {
            startHour = config.getStoreHours().openHour(config.getCalendar().dayOfWeekAt(dayIndex));
        }

        int endHour = startHour + shiftLength;
        return "%02d:00-%02d:00".formatted(startHour, endHour);
    }

    private int countWorkingEmployees(ScheduleEntity schedule, int dayIndex) {
        int count = 0;
        for (List<Integer> row : schedule.getGenes()) {
            Integer value = row.get(dayIndex);
            if (value != null && value > 0) {
                count++;
            }
        }
        return count;
    }

    private ScheduleEntity requireById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Grafik", id));
    }

    private ScheduleEntity requireDetailedById(Long id) {
        return scheduleRepository.findDetailedById(id)
                .orElseThrow(() -> NotFoundException.of("Grafik", id));
    }

    private record EmployeeScheduleRow(
            Long id,
            String name,
            String surname,
            Integer totalHours,
            Integer totalDays,
            List<String> shifts) {
    }

    private record DayCoverageRow(
            Integer day,
            LocalDate date,
            String dayOfWeekLabel,
            boolean closed,
            Integer required,
            Integer actual) {
    }
}
