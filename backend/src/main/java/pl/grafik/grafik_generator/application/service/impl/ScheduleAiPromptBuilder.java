package pl.grafik.grafik_generator.application.service.impl;

import org.springframework.stereotype.Component;
import pl.grafik.grafik_generator.domain.context.CalendarConfig;
import pl.grafik.grafik_generator.domain.context.StaffingTargets;
import pl.grafik.grafik_generator.domain.context.StoreHours;
import pl.grafik.grafik_generator.domain.entity.EmployeeEntity;
import pl.grafik.grafik_generator.domain.entity.ScheduleConfigEntity;
import pl.grafik.grafik_generator.domain.entity.ScheduleEntity;
import pl.grafik.grafik_generator.domain.entity.SectionEntity;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;

@Component
public class ScheduleAiPromptBuilder {

    public String build(ScheduleEntity schedule, String instruction, boolean allowProtectedDateChanges) {
        ScheduleConfigEntity config = schedule.getRun().getConfig();
        SectionEntity section = schedule.getRun().getSection();
        List<EmployeeEntity> employees = section.getEmployees().stream()
                .sorted(Comparator.comparing(EmployeeEntity::getId))
                .toList();

        StringBuilder prompt = new StringBuilder();
        prompt.append("# Zadanie\n");
        prompt.append("Jestes asystentem do punktowej edycji grafiku pracy. Zwracasz tylko JSON zgodny ze schematem.\n");
        prompt.append("Nie przepisuj calego grafiku. Zwroc tylko komorki, ktore trzeba zmienic.\n\n");
        prompt.append("JSON ma byc mozliwie krotki: w zmianach podawaj tylko employeeId, day, shiftLength, startHour, scheduleId i sectionId. ")
                .append("Nie dodawaj pola reason przy kazdej zmianie; ogolne uwagi wpisz tylko w warnings.\n\n");
        prompt.append("Polecenie uzytkownika: ").append(instruction).append("\n");
        prompt.append("Czy wolno zmieniac urlopy i recznie wybrane wolne: ")
                .append(allowProtectedDateChanges ? "TAK" : "NIE").append("\n\n");

        prompt.append("# Twarde zasady\n");
        prompt.append("- employeeId musi pochodzic z listy pracownikow.\n");
        prompt.append("- day jest numerem dnia miesiaca, 1-based.\n");
        prompt.append("- shiftLength=0 i startHour=0 oznacza wolne.\n");
        prompt.append("- Dlugosc zmiany musi byc jedna z dozwolonych: ")
                .append(config.getShiftRules().shiftLengths()).append(".\n");
        prompt.append("- Start i koniec zmiany musza miescic sie w godzinach sklepu.\n");
        prompt.append("- Nie planuj pracy w dni zamkniete.\n");
        prompt.append("- Bez jawnej zgody nie planuj pracy w urlop ani w recznie wybrane wolne.\n");
        prompt.append("- Zachowaj laczna liczbe godzin i dni pracy kazdego pracownika wzgledem obecnego grafiku.\n\n");

        appendConfig(prompt, config);
        appendEmployees(prompt, employees);
        appendSchedule(prompt, schedule, config, employees);
        return prompt.toString();
    }

    public String buildGroup(List<ScheduleEntity> schedules, String instruction, boolean allowProtectedDateChanges) {
        if (schedules == null || schedules.isEmpty()) {
            throw new IllegalArgumentException("Brak grafikow w grupie do edycji AI.");
        }

        ScheduleConfigEntity config = schedules.get(0).getRun().getConfig();
        StringBuilder prompt = new StringBuilder();
        prompt.append("# Zadanie\n");
        prompt.append("Jestes asystentem do punktowej edycji grafiku pracy dla calej konfiguracji sklepu.\n");
        prompt.append("Widzisz wszystkie sekcje tej samej konfiguracji. Zwracasz tylko JSON zgodny ze schematem.\n");
        prompt.append("Nie przepisuj calego grafiku. Zwroc tylko komorki, ktore trzeba zmienic.\n");
        prompt.append("Kazda zmiana musi wskazywac scheduleId oraz najlepiej sectionId, zeby bylo jasne ktorej sekcji dotyczy.\n\n");
        prompt.append("JSON ma byc mozliwie krotki: w zmianach podawaj tylko employeeId, day, shiftLength, startHour, scheduleId i sectionId. ")
                .append("Nie dodawaj pola reason przy kazdej zmianie; ogolne uwagi wpisz tylko w warnings.\n\n");
        prompt.append("Polecenie uzytkownika: ").append(instruction).append("\n");
        prompt.append("Czy wolno zmieniac urlopy i recznie wybrane wolne: ")
                .append(allowProtectedDateChanges ? "TAK" : "NIE").append("\n\n");

        prompt.append("# Twarde zasady\n");
        prompt.append("- scheduleId musi pochodzic z listy sekcji/grafikow ponizej.\n");
        prompt.append("- employeeId musi pochodzic z pracownikow tej samej sekcji co scheduleId.\n");
        prompt.append("- day jest numerem dnia miesiaca, 1-based.\n");
        prompt.append("- shiftLength=0 i startHour=0 oznacza wolne.\n");
        prompt.append("- Dlugosc zmiany musi byc jedna z dozwolonych: ")
                .append(config.getShiftRules().shiftLengths()).append(".\n");
        prompt.append("- Start i koniec zmiany musza miescic sie w godzinach sklepu.\n");
        prompt.append("- Nie planuj pracy w dni zamkniete.\n");
        prompt.append("- Bez jawnej zgody nie planuj pracy w urlop ani w recznie wybrane wolne.\n");
        prompt.append("- Zachowaj laczna liczbe godzin i dni pracy kazdego pracownika wzgledem obecnego grafiku.\n");
        prompt.append("- Gdy polecenie dotyczy relacji miedzy sekcjami, patrz na cala konfiguracje, a nie na jedna sekcje osobno.\n\n");

        appendConfig(prompt, config);
        prompt.append("# Sekcje i grafiki w tej konfiguracji\n");
        schedules.stream()
                .sorted(Comparator.comparing(schedule -> schedule.getRun().getSection().getName()))
                .forEach(schedule -> {
                    SectionEntity section = schedule.getRun().getSection();
                    List<EmployeeEntity> employees = section.getEmployees().stream()
                            .sorted(Comparator.comparing(EmployeeEntity::getId))
                            .toList();
                    prompt.append("## Sekcja: ").append(section.getName())
                            .append(" | sectionId=").append(section.getId())
                            .append(" | scheduleId=").append(schedule.getId())
                            .append("\n\n");
                    appendEmployees(prompt, employees);
                    appendSchedule(prompt, schedule, config, employees);
                    prompt.append("\n");
                });
        return prompt.toString();
    }

    private void appendConfig(StringBuilder prompt, ScheduleConfigEntity config) {
        CalendarConfig calendar = config.getCalendar();
        prompt.append("# Konfiguracja\n");
        prompt.append("- Miesiac: ").append(calendar.yearMonth()).append("\n");
        prompt.append("- Swieta/dni zamkniete ustawione przez uzytkownika: ").append(calendar.holidays()).append("\n");
        prompt.append("- Niedziele handlowe: ").append(calendar.tradingSundays()).append("\n");
        prompt.append("- Maksymalnie dni pracy z rzedu dla edycji AI: ")
                .append(config.getShiftRules().maxWorkingDaysInARow() + 1)
                .append(" (standardowy limit konfiguracji + 1).\n");
        prompt.append("- Edycja AI nie ma limitu osob zaczynajacych o tej samej godzinie.\n");
        prompt.append("- Cele obsady/pokrycia:\n");
        StaffingTargets targets = config.getStaffingTargets();
        for (DayOfWeek day : DayOfWeek.values()) {
            prompt.append("  - ").append(day).append(": ");
            if (targets.usesCount(day)) {
                prompt.append(targets.countFor(day)).append(" os.");
            } else {
                prompt.append(targets.percentFor(day)).append("% pracownikow sekcji");
            }
            StaffingTargets.PeakWindow peak = targets.peakFor(day);
            if (peak != null) {
                prompt.append(", peak=").append(peak.start()).append("-").append(peak.end());
            }
            prompt.append("\n");
        }
        prompt.append("- Godziny sklepu:\n");
        StoreHours hours = config.getStoreHours();
        for (var entry : hours.hours().entrySet()) {
            prompt.append("  - ").append(entry.getKey()).append(": ")
                    .append(entry.getValue().open()).append("-").append(entry.getValue().close()).append("\n");
        }
        prompt.append("\n");
    }

    private void appendEmployees(StringBuilder prompt, List<EmployeeEntity> employees) {
        prompt.append("# Pracownicy\n");
        for (EmployeeEntity employee : employees) {
            prompt.append("- id=").append(employee.getId())
                    .append(", name=").append(employee.getName()).append(" ").append(employee.getSurname())
                    .append(", totalHours=").append(employee.getTotalHours())
                    .append(", totalDays=").append(employee.getTotalDays())
                    .append(", recznieWybraneWolne0Based=").append(employee.getDaysOff())
                    .append(", urlopy0Based=").append(employee.getVacations())
                    .append("\n");
        }
        prompt.append("\n");
    }

    private void appendSchedule(StringBuilder prompt, ScheduleEntity schedule, ScheduleConfigEntity config,
            List<EmployeeEntity> employees) {
        prompt.append("# Obecny grafik\n");
        prompt.append("Format: day(date) required=X working=Y coverage=Z%: employeeId=shiftLength@startHour, 0@0 oznacza wolne.\n");
        int days = config.getCalendar().daysInMonth();
        for (int dayIndex = 0; dayIndex < days; dayIndex++) {
            LocalDate date = config.getCalendar().dateAt(dayIndex);
            prompt.append("- ").append(dayIndex + 1).append("(").append(date).append(")");
            if (config.getCalendar().isClosedDay(dayIndex)) {
                prompt.append(" CLOSED");
            }
            int required = config.getStaffingTargets().requiredEmployees(date.getDayOfWeek(), employees.size());
            int working = 0;
            for (int employeeIndex = 0; employeeIndex < employees.size(); employeeIndex++) {
                if (safe(schedule.getGenes().get(employeeIndex).get(dayIndex)) > 0) {
                    working++;
                }
            }
            int coverage = required <= 0 ? 0 : (int) Math.round(working * 100.0 / required);
            prompt.append(" required=").append(required)
                    .append(" working=").append(working)
                    .append(" coverage=").append(coverage).append("%")
                    .append(": ");
            for (int employeeIndex = 0; employeeIndex < employees.size(); employeeIndex++) {
                EmployeeEntity employee = employees.get(employeeIndex);
                int length = safe(schedule.getGenes().get(employeeIndex).get(dayIndex));
                int start = safe(schedule.getShiftStarts().get(employeeIndex).get(dayIndex));
                prompt.append(employee.getId()).append("=").append(length).append("@").append(start);
                if (employeeIndex < employees.size() - 1) {
                    prompt.append(", ");
                }
            }
            prompt.append("\n");
        }
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
