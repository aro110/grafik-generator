package pl.grafik.grafik_generator.application.service;

import pl.grafik.grafik_generator.application.dto.ScheduleDetailsDto;
import pl.grafik.grafik_generator.application.dto.ScheduleDto;
import pl.grafik.grafik_generator.application.dto.ScheduleExportDto;
import pl.grafik.grafik_generator.application.dto.StoreLevelSummaryDto;

import java.util.List;

public interface ScheduleService {

    List<ScheduleDto> findByRunId(Long runId);

    ScheduleDto findById(Long id);

    ScheduleDetailsDto findDetailsById(Long id);

    StoreLevelSummaryDto getStoreSummary(Long groupId);

    ScheduleDto publish(Long id);

    ScheduleExportDto exportToExcel(Long id);

    ScheduleExportDto exportGroupToExcel(Long groupId);
}
