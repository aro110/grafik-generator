package pl.grafik.grafik_generator.application.mapper;

import org.springframework.stereotype.Component;
import pl.grafik.grafik_generator.domain.context.CalendarConfig;
import pl.grafik.grafik_generator.domain.context.ShiftRules;
import pl.grafik.grafik_generator.domain.entity.EmployeeEntity;
import pl.grafik.grafik_generator.domain.entity.SectionEntity;
import pl.grafik.grafik_generator.domain.model.Employee;
import pl.grafik.grafik_generator.domain.model.Section;
import pl.grafik.grafik_generator.domain.shiftPoolGenerator.ShiftPool;

import java.util.List;

@Component
public class DomainMapper {

    public Section toDomainSection(SectionEntity entity, ShiftRules rules, CalendarConfig calendar,
            pl.grafik.grafik_generator.domain.context.VacationConfig vacationConfig) {
        ShiftPool pool = new ShiftPool(rules);
        List<Employee> employees = entity.getEmployees().stream()
                .map(empEntity -> toDomainEmployee(empEntity, entity.getName(), pool, rules, calendar, vacationConfig))
                .toList();
        return new Section(entity.getName(), employees);
    }

    private Employee toDomainEmployee(EmployeeEntity entity, String sectionName,
            ShiftPool pool, ShiftRules rules, CalendarConfig calendar,
            pl.grafik.grafik_generator.domain.context.VacationConfig vacationConfig) {
        return new Employee(
                entity.getName(),
                entity.getSurname(),
                sectionName,
                entity.getTotalHours(),
                entity.getTotalDays(),
                entity.getDaysOff(),
                entity.getVacations(),
                pool,
                rules,
                calendar,
                vacationConfig);
    }
}
