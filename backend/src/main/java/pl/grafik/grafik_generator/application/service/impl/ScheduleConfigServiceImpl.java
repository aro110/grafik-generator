package pl.grafik.grafik_generator.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.grafik.grafik_generator.application.dto.ScheduleConfigDto;
import pl.grafik.grafik_generator.application.dto.request.CreateScheduleConfigRequest;
import pl.grafik.grafik_generator.application.dto.request.UpdateScheduleConfigRequest;
import pl.grafik.grafik_generator.application.exception.NotFoundException;
import pl.grafik.grafik_generator.application.mapper.ScheduleConfigMapper;
import pl.grafik.grafik_generator.application.service.ScheduleConfigService;
import pl.grafik.grafik_generator.application.validation.ScheduleConfigValidator;
import pl.grafik.grafik_generator.domain.entity.ScheduleConfigEntity;
import pl.grafik.grafik_generator.domain.entity.ScheduleConfigStatus;
import pl.grafik.grafik_generator.infrastructure.repository.ScheduleConfigRepository;

import java.util.List;

@Service
public class ScheduleConfigServiceImpl implements ScheduleConfigService {

    private final ScheduleConfigRepository scheduleConfigRepository;
    private final ScheduleConfigMapper scheduleConfigMapper;

    public ScheduleConfigServiceImpl(ScheduleConfigRepository scheduleConfigRepository,
            ScheduleConfigMapper scheduleConfigMapper) {
        this.scheduleConfigRepository = scheduleConfigRepository;
        this.scheduleConfigMapper = scheduleConfigMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleConfigDto> findAll() {
        return scheduleConfigRepository.findAll().stream()
                .map(scheduleConfigMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleConfigDto findById(Long id) {
        return scheduleConfigMapper.toDto(requireById(id));
    }

    @Override
    @Transactional
    public ScheduleConfigDto create(CreateScheduleConfigRequest request) {
        ScheduleConfigValidator.validate(
                request.storeHours(), request.staffingTargets(), request.calendar());

        ScheduleConfigEntity entity = new ScheduleConfigEntity();
        entity.setName(request.name());
        entity.setYearMonth(request.yearMonth());
        entity.setStoreHours(request.storeHours());
        entity.setStaffingTargets(request.staffingTargets());
        entity.setCalendar(request.calendar());
        entity.setShiftRules(request.shiftRules());
        entity.setVacationConfig(request.vacationConfig());
        entity.setGaParameters(request.gaParameters());
        entity.setStatus(ScheduleConfigStatus.DRAFT);
        entity.setVersion(1);
        return scheduleConfigMapper.toDto(scheduleConfigRepository.save(entity));
    }

    @Override
    @Transactional
    public ScheduleConfigDto update(Long id, UpdateScheduleConfigRequest request) {
        ScheduleConfigValidator.validate(
                request.storeHours(), request.staffingTargets(), request.calendar());

        ScheduleConfigEntity entity = requireById(id);
        entity.setName(request.name());
        entity.setYearMonth(request.yearMonth());
        entity.setStoreHours(request.storeHours());
        entity.setStaffingTargets(request.staffingTargets());
        entity.setCalendar(request.calendar());
        entity.setShiftRules(request.shiftRules());
        entity.setVacationConfig(request.vacationConfig());
        entity.setGaParameters(request.gaParameters());
        entity.setVersion(entity.getVersion() + 1);
        return scheduleConfigMapper.toDto(entity);
    }

    @Override
    @Transactional
    public ScheduleConfigDto publish(Long id) {
        ScheduleConfigEntity entity = requireById(id);
        entity.setStatus(ScheduleConfigStatus.PUBLISHED);
        return scheduleConfigMapper.toDto(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ScheduleConfigEntity entity = requireById(id);
        scheduleConfigRepository.delete(entity);
    }

    private ScheduleConfigEntity requireById(Long id) {
        return scheduleConfigRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Konfiguracja grafiku", id));
    }
}
