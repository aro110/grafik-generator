package pl.grafik.grafik_generator.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.grafik.grafik_generator.application.dto.EmployeeDto;
import pl.grafik.grafik_generator.application.dto.request.CreateEmployeeRequest;
import pl.grafik.grafik_generator.application.dto.request.UpdateEmployeeRequest;
import pl.grafik.grafik_generator.application.exception.NotFoundException;
import pl.grafik.grafik_generator.application.mapper.EmployeeMapper;
import pl.grafik.grafik_generator.application.service.EmployeeService;
import pl.grafik.grafik_generator.domain.entity.EmployeeEntity;
import pl.grafik.grafik_generator.domain.entity.SectionEntity;
import pl.grafik.grafik_generator.infrastructure.repository.EmployeeRepository;
import pl.grafik.grafik_generator.infrastructure.repository.SectionRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final SectionRepository sectionRepository;
    private final EmployeeMapper employeeMapper;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
            SectionRepository sectionRepository,
            EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.sectionRepository = sectionRepository;
        this.employeeMapper = employeeMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDto> findAll() {
        return employeeRepository.findAll().stream().map(employeeMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDto> findBySectionId(Long sectionId) {
        return employeeRepository.findBySectionId(sectionId).stream().map(employeeMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDto findById(Long id) {
        return employeeMapper.toDto(requireById(id));
    }

    @Override
    @Transactional
    public EmployeeDto create(CreateEmployeeRequest request) {
        SectionEntity section = requireSection(request.sectionId());
        EmployeeEntity entity = new EmployeeEntity();
        entity.setName(request.name());
        entity.setSurname(request.surname());
        entity.setSection(section);
        entity.setTotalHours(request.totalHours());
        entity.setTotalDays(request.totalDays());
        entity.setDaysOff(request.daysOff() == null ? new ArrayList<>() : new ArrayList<>(request.daysOff()));
        entity.setVacations(request.vacations() == null ? new ArrayList<>() : new ArrayList<>(request.vacations()));
        return employeeMapper.toDto(employeeRepository.save(entity));
    }

    @Override
    @Transactional
    public EmployeeDto update(Long id, UpdateEmployeeRequest request) {
        EmployeeEntity entity = requireById(id);
        SectionEntity section = requireSection(request.sectionId());
        entity.setName(request.name());
        entity.setSurname(request.surname());
        entity.setSection(section);
        entity.setTotalHours(request.totalHours());
        entity.setTotalDays(request.totalDays());
        entity.setDaysOff(request.daysOff() == null ? new ArrayList<>() : new ArrayList<>(request.daysOff()));
        entity.setVacations(request.vacations() == null ? new ArrayList<>() : new ArrayList<>(request.vacations()));
        return employeeMapper.toDto(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        EmployeeEntity entity = requireById(id);
        employeeRepository.delete(entity);
    }

    private EmployeeEntity requireById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Pracownik", id));
    }

    private SectionEntity requireSection(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Sekcja", id));
    }
}
