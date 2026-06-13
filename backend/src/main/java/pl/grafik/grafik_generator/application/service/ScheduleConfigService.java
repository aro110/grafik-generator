package pl.grafik.grafik_generator.application.service;

import pl.grafik.grafik_generator.application.dto.ScheduleConfigDto;
import pl.grafik.grafik_generator.application.dto.request.CreateScheduleConfigRequest;
import pl.grafik.grafik_generator.application.dto.request.UpdateScheduleConfigRequest;

import java.util.List;

public interface ScheduleConfigService {

    List<ScheduleConfigDto> findAll();

    ScheduleConfigDto findById(Long id);

    ScheduleConfigDto create(CreateScheduleConfigRequest request);

    ScheduleConfigDto update(Long id, UpdateScheduleConfigRequest request);

    ScheduleConfigDto publish(Long id);

    void delete(Long id);
}
