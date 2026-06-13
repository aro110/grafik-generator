package pl.grafik.grafik_generator.application.service;

import pl.grafik.grafik_generator.application.dto.EmployeeDto;
import pl.grafik.grafik_generator.application.dto.request.CreateEmployeeRequest;
import pl.grafik.grafik_generator.application.dto.request.UpdateEmployeeRequest;

import java.util.List;

public interface EmployeeService {

    List<EmployeeDto> findAll();

    List<EmployeeDto> findBySectionId(Long sectionId);

    EmployeeDto findById(Long id);

    EmployeeDto create(CreateEmployeeRequest request);

    EmployeeDto update(Long id, UpdateEmployeeRequest request);

    void delete(Long id);
}
