package pl.grafik.grafik_generator.application.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Component;
import pl.grafik.grafik_generator.domain.context.CalendarConfig;
import pl.grafik.grafik_generator.domain.context.ProtectedScheduleOverride;
import pl.grafik.grafik_generator.domain.context.StaffingTargets;
import pl.grafik.grafik_generator.domain.context.StoreHours;
import pl.grafik.grafik_generator.domain.entity.EmployeeEntity;
import pl.grafik.grafik_generator.domain.entity.ScheduleConfigEntity;
import pl.grafik.grafik_generator.domain.entity.ScheduleEntity;
import pl.grafik.grafik_generator.domain.entity.SectionEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class ScheduleExcelExporter {

    private static final Locale POLISH = Locale.forLanguageTag("pl-PL");
    private static final DateTimeFormatter FILE_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM", POLISH);
    private static final DateTimeFormatter SHEET_MONTH_FORMAT = DateTimeFormatter.ofPattern("LLLL yyyy", POLISH);
    private static final DateTimeFormatter DAY_LABEL_FORMAT = DateTimeFormatter.ofPattern("d EEE", POLISH);
    private static final DateTimeFormatter DAY_FULL_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy", POLISH);
    private static final DateTimeFormatter TEMPLATE_DAY_FORMAT = DateTimeFormatter.ofPattern("dd-MMM", Locale.ENGLISH);
    private static final int TEMPLATE_START_HOUR = 10;
    private static final int TEMPLATE_END_HOUR = 21;
    private static final IndexedColors[] SECTION_COLORS = {
            IndexedColors.LIGHT_CORNFLOWER_BLUE,
            IndexedColors.LIGHT_GREEN,
            IndexedColors.LIGHT_YELLOW,
            IndexedColors.LIGHT_ORANGE,
            IndexedColors.LAVENDER,
            IndexedColors.TAN,
            IndexedColors.AQUA,
            IndexedColors.CORAL,
            IndexedColors.SKY_BLUE,
            IndexedColors.LIME
    };

    public ExportedScheduleFile export(ScheduleEntity schedule) {
        if (schedule == null) {
            throw new IllegalArgumentException("Grafik do eksportu nie może być pusty.");
        }
        if (schedule.getRun() == null) {
            throw new IllegalStateException("Nie można wyeksportować grafiku bez powiązanego runa generacji.");
        }
        if (schedule.getRun().getConfig() == null) {
            throw new IllegalStateException("Nie można wyeksportować grafiku bez konfiguracji.");
        }
        if (schedule.getRun().getSection() == null) {
            throw new IllegalStateException(
                    "Nie można wyeksportować grafiku bez przypisanej sekcji. Wygeneruj grafik ponownie po zapisaniu sekcji w runie.");
        }

        ScheduleConfigEntity config = schedule.getRun().getConfig();
        SectionEntity section = schedule.getRun().getSection();
        List<EmployeeEntity> employees = resolveEmployees(section);

        validateMatrixSizes(schedule, employees, config.getCalendar());

        String monthLabel = formatMonthLabel(config.getCalendar());
        String fileName = buildFileName(section, config, schedule.getId());

        log.info("Eksportowanie grafiku {} do pliku Excel dla sekcji {}", schedule.getId(), section.getName());

        try (HSSFWorkbook workbook = new HSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            WorkbookStyles styles = createStyles(workbook, List.of(section));
            createScheduleSheet(workbook, styles, schedule, config, section, employees, monthLabel);
            createStaffingSheet(workbook, styles, schedule, config, section, employees, monthLabel);
            createCoverageSheet(workbook, styles, schedule, config, section, employees, monthLabel);

            workbook.write(outputStream);

            return new ExportedScheduleFile(
                    fileName,
                    "application/vnd.ms-excel",
                    outputStream.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("Nie udało się przygotować pliku Excel dla grafiku.", ex);
        }
    }

    public ExportedScheduleFile exportGroup(List<ScheduleEntity> schedules, Long groupId) {
        if (schedules == null || schedules.isEmpty()) {
            throw new IllegalArgumentException("Lista grafików do eksportu nie może być pusta.");
        }

        List<ScheduleEntity> orderedSchedules = schedules.stream()
                .sorted(Comparator.comparing(schedule -> schedule.getRun().getSection().getName(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        ScheduleConfigEntity config = orderedSchedules.get(0).getRun().getConfig();
        for (ScheduleEntity schedule : orderedSchedules) {
            if (schedule.getRun() == null || schedule.getRun().getConfig() == null
                    || schedule.getRun().getSection() == null) {
                throw new IllegalStateException("Każdy grafik w eksporcie grupy musi mieć konfigurację i sekcję.");
            }
            validateMatrixSizes(schedule, resolveEmployees(schedule.getRun().getSection()), config.getCalendar());
        }

        String fileName = "grafik-warszawa-" + config.getCalendar().yearMonth().format(FILE_MONTH_FORMAT)
                + "-grupa-" + groupId + ".xls";

        try (HSSFWorkbook workbook = new HSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            WorkbookStyles styles = createStyles(workbook, orderedSchedules.stream()
                    .map(schedule -> schedule.getRun().getSection())
                    .toList());
            createTemplateWorkingSheet(workbook, styles, orderedSchedules, config);
            createTemplateScheduleSheet(workbook, styles, orderedSchedules, config);
            createTemplateStaffingSummarySheet(workbook, styles, orderedSchedules, config, "Obsada", false);
            createTrainingSheet(workbook, styles);
            createTemplateStaffingSummarySheet(workbook, styles, orderedSchedules, config, "Zmiany 8", true);

            workbook.write(outputStream);

            return new ExportedScheduleFile(
                    fileName,
                    "application/vnd.ms-excel",
                    outputStream.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("Nie udało się przygotować pliku Excel dla grupy grafików.", ex);
        }
    }

    private void createTemplateScheduleSheet(HSSFWorkbook workbook,
            WorkbookStyles styles,
            List<ScheduleEntity> schedules,
            ScheduleConfigEntity config) {
        Sheet sheet = workbook.createSheet("Grafik_Sala_Sprzedaży");
        CalendarConfig calendar = config.getCalendar();
        int daysInMonth = calendar.daysInMonth();
        int lastColumn = 1;

        Row monthRow = sheet.createRow(0);
        Row namesRow = sheet.createRow(1);
        sheet.createRow(2);
        Row labelsRow = sheet.createRow(3);

        createCell(monthRow, 0, capitalize(calendar.yearMonth().format(DateTimeFormatter.ofPattern("LLLL", POLISH))),
                styles.mainHeader);
        sheet.addMergedRegion(new CellRangeAddress(0, 3, 0, 0));
        applyStyleToMergedRegion(sheet, 0, 3, 0, styles.mainHeader);
        createCell(namesRow, 1, "Dzień tygodnia", styles.headerMedium);
        sheet.addMergedRegion(new CellRangeAddress(1, 3, 1, 1));
        applyStyleToMergedRegion(sheet, 1, 3, 1, styles.headerMedium);

        int column = 2;
        for (int scheduleIndex = 0; scheduleIndex < schedules.size(); scheduleIndex++) {
            ScheduleEntity schedule = schedules.get(scheduleIndex);
            SectionEntity section = schedule.getRun().getSection();
            List<EmployeeEntity> employees = resolveEmployees(section);
            int sectionStart = column;
            boolean firstSection = scheduleIndex == 0;
            CellStyle employeeStyle = firstSection ? styles.employeeHeaderMedium : styles.employeeHeader;
            CellStyle labelStyle = firstSection ? styles.subHeaderMedium : styles.subHeader;
            CellStyle totalStyle = firstSection ? styles.totalMedium : styles.total;

            for (EmployeeEntity employee : employees) {
                createCell(namesRow, column, fullName(employee), employeeStyle);
                sheet.addMergedRegion(new CellRangeAddress(1, 2, column, column + 2));
                applyStyleToMergedRegion(namesRow, column, column + 2, employeeStyle);
                createCell(labelsRow, column, "OD", labelStyle);
                createCell(labelsRow, column + 1, "DO", labelStyle);
                createCell(labelsRow, column + 2, "GODZ", labelStyle);
                column += 3;
            }

            createCell(labelsRow, column, "Total", totalStyle);
            CellStyle sectionStyle = sectionHeaderStyle(styles, scheduleIndex);
            createCell(monthRow, sectionStart, section.getName(), sectionStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, sectionStart, column));
            applyStyleToMergedRegion(monthRow, sectionStart, column, sectionStyle);
            column++;
        }
        lastColumn = Math.max(lastColumn, column - 1);

        for (int dayIndex = 0; dayIndex < daysInMonth; dayIndex++) {
            Row row = sheet.createRow(4 + dayIndex);
            LocalDate date = calendar.dateAt(dayIndex);
            CellStyle dateStyle = templateDayStyle(styles, calendar, dayIndex, false);
            createCell(row, 0, date.format(TEMPLATE_DAY_FORMAT), dateStyle);
            createCell(row, 1, date.getDayOfWeek().getDisplayName(TextStyle.FULL, POLISH), dateStyle);

            column = 2;
            for (int scheduleIndex = 0; scheduleIndex < schedules.size(); scheduleIndex++) {
                ScheduleEntity schedule = schedules.get(scheduleIndex);
                int sectionWorking = 0;
                List<EmployeeEntity> employees = resolveEmployees(schedule.getRun().getSection());
                CellStyle rowStyle = templateDayStyle(styles, calendar, dayIndex, scheduleIndex == 0);
                for (int employeeIndex = 0; employeeIndex < employees.size(); employeeIndex++) {
                    if (isVacation(schedule, employees.get(employeeIndex), dayIndex)) {
                        writeVacationTriplet(row, column, styles.vacationCell);
                        column += 3;
                        continue;
                    }

                    ShiftWindow shift = shiftWindow(schedule, config, employeeIndex, dayIndex);
                    if (shift.length() > 0) {
                        sectionWorking++;
                    }
                    writeShiftTriplet(row, column, shift, rowStyle);
                    column += 3;
                }
                CellStyle totalStyle = scheduleIndex == 0 ? styles.totalMedium : styles.total;
                createCell(row, column++, calendar.isClosedDay(dayIndex) ? 0 : sectionWorking, totalStyle);
            }
        }

        Row sumRow = sheet.createRow(4 + daysInMonth);
        createCell(sumRow, 0, "Suma godzin", styles.summaryLabel);
        createCell(sumRow, 1, "", styles.summaryLabel);
        column = 2;
        for (int scheduleIndex = 0; scheduleIndex < schedules.size(); scheduleIndex++) {
            ScheduleEntity schedule = schedules.get(scheduleIndex);
            List<EmployeeEntity> employees = resolveEmployees(schedule.getRun().getSection());
            CellStyle summaryStyle = scheduleIndex == 0 ? styles.summaryNumberMedium : styles.summaryNumber;
            for (int employeeIndex = 0; employeeIndex < employees.size(); employeeIndex++) {
                createCell(sumRow, column, formatDuration(sumPlannedHours(schedule.getGenes().get(employeeIndex))),
                        summaryStyle);
                sheet.addMergedRegion(new CellRangeAddress(4 + daysInMonth, 4 + daysInMonth, column, column + 2));
                applyStyleToMergedRegion(sumRow, column, column + 2, summaryStyle);
                column += 3;
            }
            createCell(sumRow, column++, "", scheduleIndex == 0 ? styles.totalMedium : styles.total);
        }

        sheet.createFreezePane(2, 4);
        sheet.setColumnWidth(0, 15 * 256);
        sheet.setColumnWidth(1, 16 * 256);
        for (int i = 2; i <= lastColumn; i++) {
            sheet.setColumnWidth(i, 9 * 256);
        }
    }

    private void createTemplateStaffingSummarySheet(HSSFWorkbook workbook,
            WorkbookStyles styles,
            List<ScheduleEntity> schedules,
            ScheduleConfigEntity config,
            String sheetName,
            boolean onlyEightHourShifts) {
        Sheet sheet = workbook.createSheet(sheetName);
        CalendarConfig calendar = config.getCalendar();
        int lastColumn = schedules.size() + 1;

        Row title = sheet.createRow(0);
        createCell(title, 0, onlyEightHourShifts ? "ZMIANY 8" : "OBSADA", styles.mainHeader);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastColumn));
        applyStyleToMergedRegion(title, 0, lastColumn, styles.mainHeader);

        Row header = sheet.createRow(1);
        createCell(header, 0, "DZIEN", styles.headerMedium);
        for (int i = 0; i < schedules.size(); i++) {
            createCell(header, i + 1, schedules.get(i).getRun().getSection().getName(), sectionHeaderStyle(styles, i));
        }
        createCell(header, lastColumn, "GRAND TOTAL", styles.total);

        for (int dayIndex = 0; dayIndex < calendar.daysInMonth(); dayIndex++) {
            Row row = sheet.createRow(dayIndex + 2);
            LocalDate date = calendar.dateAt(dayIndex);
            createCell(row, 0, date.format(TEMPLATE_DAY_FORMAT), templateDayStyle(styles, calendar, dayIndex, false));

            int total = 0;
            for (int i = 0; i < schedules.size(); i++) {
                int count = calendar.isClosedDay(dayIndex)
                        ? 0
                        : countWorkingEmployees(schedules.get(i), dayIndex, onlyEightHourShifts);
                if (calendar.isClosedDay(dayIndex)) {
                    createCell(row, i + 1, "", templateDayStyle(styles, calendar, dayIndex, i == 0));
                } else {
                    createCell(row, i + 1, count, templateDayStyle(styles, calendar, dayIndex, i == 0));
                }
                total += count;
            }
            createCell(row, lastColumn, total, styles.total);
        }

        for (int i = 0; i <= lastColumn; i++) {
            sheet.setColumnWidth(i, 10 * 256);
        }
        sheet.setColumnWidth(0, 18 * 256);
        sheet.createFreezePane(1, 2);
    }

    private void createTemplateWorkingSheet(HSSFWorkbook workbook,
            WorkbookStyles styles,
            List<ScheduleEntity> schedules,
            ScheduleConfigEntity config) {
        Sheet sheet = workbook.createSheet("roboczy");
        CalendarConfig calendar = config.getCalendar();

        Row title = sheet.createRow(2);
        createCell(title, 2, "Godzina", styles.mainHeader);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 2, 2 + (TEMPLATE_END_HOUR - TEMPLATE_START_HOUR)));
        applyStyleToMergedRegion(title, 2, 2 + (TEMPLATE_END_HOUR - TEMPLATE_START_HOUR), styles.mainHeader);

        Row header = sheet.createRow(3);
        createCell(header, 1, "Data", styles.headerMedium);
        for (int hour = TEMPLATE_START_HOUR; hour <= TEMPLATE_END_HOUR; hour++) {
            createCell(header, 2 + hour - TEMPLATE_START_HOUR, hour, styles.subHeader);
        }

        int rowIndex = 4;
        for (int scheduleIndex = 0; scheduleIndex < schedules.size(); scheduleIndex++) {
            ScheduleEntity schedule = schedules.get(scheduleIndex);
            SectionEntity section = schedule.getRun().getSection();
            int sectionStart = rowIndex;
            for (int dayIndex = 0; dayIndex < calendar.daysInMonth(); dayIndex++) {
                Row row = sheet.createRow(rowIndex++);
                if (dayIndex == 0) {
                    createCell(row, 0, section.getName(), sectionHeaderStyle(styles, scheduleIndex));
                }
                createCell(row, 1, dayIndex + 1, templateDayStyle(styles, calendar, dayIndex, false));
                for (int hour = TEMPLATE_START_HOUR; hour <= TEMPLATE_END_HOUR; hour++) {
                    int value = calendar.isClosedDay(dayIndex) ? 0 : countEmployeesAtHour(schedule, config, dayIndex, hour);
                    createCell(row, 2 + hour - TEMPLATE_START_HOUR, value,
                            templateDayStyle(styles, calendar, dayIndex, scheduleIndex == 0));
                }
            }
            sheet.addMergedRegion(new CellRangeAddress(sectionStart, rowIndex - 1, 0, 0));
            applyStyleToMergedRegion(sheet, sectionStart, rowIndex - 1, 0, sectionHeaderStyle(styles, scheduleIndex));
        }

        for (int i = 0; i <= 2 + (TEMPLATE_END_HOUR - TEMPLATE_START_HOUR); i++) {
            sheet.setColumnWidth(i, 10 * 256);
        }
        sheet.createFreezePane(2, 4);
    }

    private void createTrainingSheet(HSSFWorkbook workbook, WorkbookStyles styles) {
        Sheet sheet = workbook.createSheet("Szkolenia");
        Row header = sheet.createRow(0);
        createCell(header, 0, "", styles.header);
        createCell(header, 1, "Kto", styles.header);
        createCell(header, 2, "Kiedy ", styles.header);
        createCell(header, 3, "Jakie szkolenie", styles.header);
        createCell(header, 4, "gdzie", styles.header);
        createCell(header, 5, "GODZINY", styles.header);

        for (int i = 1; i <= 18; i++) {
            Row row = sheet.createRow(i);
            createCell(row, 0, i, styles.integer);
            for (int column = 1; column <= 5; column++) {
                createCell(row, column, "", styles.text);
            }
        }

        sheet.setColumnWidth(0, 4 * 256);
        sheet.setColumnWidth(1, 24 * 256);
        sheet.setColumnWidth(2, 24 * 256);
        sheet.setColumnWidth(3, 31 * 256);
        sheet.setColumnWidth(4, 64 * 256);
        sheet.setColumnWidth(5, 15 * 256);
    }

    private void writeShiftTriplet(Row row, int column, ShiftWindow shift, CellStyle style) {
        if (shift.length() <= 0) {
            createCell(row, column, "", style);
            createCell(row, column + 1, "", style);
            createCell(row, column + 2, "00:00", style);
            return;
        }

        createCell(row, column, formatHour(shift.start()), style);
        createCell(row, column + 1, formatHour(shift.end()), style);
        createCell(row, column + 2, formatDuration(shift.length()), style);
    }

    private void writeVacationTriplet(Row row, int column, CellStyle style) {
        createCell(row, column, "URLOP", style);
        createCell(row, column + 1, "", style);
        createCell(row, column + 2, "00:00", style);
    }

    private ShiftWindow shiftWindow(ScheduleEntity schedule,
            ScheduleConfigEntity config,
            int employeeIndex,
            int dayIndex) {
        if (config.getCalendar().isClosedDay(dayIndex)) {
            return new ShiftWindow(0, 0, 0);
        }

        int length = safeValue(schedule.getGenes().get(employeeIndex).get(dayIndex));
        if (length <= 0) {
            return new ShiftWindow(0, 0, 0);
        }

        int start = safeValue(schedule.getShiftStarts().get(employeeIndex).get(dayIndex));
        if (start <= 0) {
            start = config.getStoreHours().openHour(config.getCalendar().dayOfWeekAt(dayIndex));
        }
        return new ShiftWindow(start, start + length, length);
    }

    private int countWorkingEmployees(ScheduleEntity schedule, int dayIndex, boolean onlyEightHourShifts) {
        int count = 0;
        for (List<Integer> row : schedule.getGenes()) {
            int length = safeValue(row.get(dayIndex));
            if (onlyEightHourShifts ? length == 8 : length > 0) {
                count++;
            }
        }
        return count;
    }

    private int countEmployeesAtHour(ScheduleEntity schedule,
            ScheduleConfigEntity config,
            int dayIndex,
            int hour) {
        int count = 0;
        for (int employeeIndex = 0; employeeIndex < schedule.getGenes().size(); employeeIndex++) {
            ShiftWindow shift = shiftWindow(schedule, config, employeeIndex, dayIndex);
            if (shift.length() > 0 && hour >= shift.start() && hour < shift.end()) {
                count++;
            }
        }
        return count;
    }

    private String fullName(EmployeeEntity employee) {
        return (employee.getName() + " " + employee.getSurname()).trim();
    }

    private String formatDuration(int hours) {
        return String.format("%02d:00", Math.max(0, hours));
    }

    private void validateMatrixSizes(ScheduleEntity schedule, List<EmployeeEntity> employees, CalendarConfig calendar) {
        int employeeCount = employees.size();
        int daysInMonth = calendar.daysInMonth();

        if (schedule.getGenes() == null || schedule.getGenes().size() != employeeCount) {
            throw new IllegalStateException("Liczba wierszy grafiku nie zgadza się z liczbą pracowników sekcji.");
        }
        if (schedule.getShiftStarts() == null || schedule.getShiftStarts().size() != employeeCount) {
            throw new IllegalStateException("Liczba godzin startu zmian nie zgadza się z liczbą pracowników sekcji.");
        }

        for (int i = 0; i < employeeCount; i++) {
            List<Integer> genesRow = schedule.getGenes().get(i);
            List<Integer> startsRow = schedule.getShiftStarts().get(i);

            if (genesRow == null || genesRow.size() != daysInMonth) {
                throw new IllegalStateException(
                        "Nieprawidłowy rozmiar danych zmian dla pracownika w wierszu " + i + ".");
            }
            if (startsRow == null || startsRow.size() != daysInMonth) {
                throw new IllegalStateException(
                        "Nieprawidłowy rozmiar danych godzin startu dla pracownika w wierszu " + i + ".");
            }
        }
    }

    private List<EmployeeEntity> resolveEmployees(SectionEntity section) {
        if (section.getEmployees() == null || section.getEmployees().isEmpty()) {
            throw new IllegalStateException("Sekcja nie ma pracowników do wyeksportowania.");
        }

        return section.getEmployees().stream()
                .sorted(Comparator.comparing(EmployeeEntity::getId))
                .toList();
    }

    private void createScheduleSheet(HSSFWorkbook workbook,
            WorkbookStyles styles,
            ScheduleEntity schedule,
            ScheduleConfigEntity config,
            SectionEntity section,
            List<EmployeeEntity> employees,
            String monthLabel) {
        Sheet sheet = workbook.createSheet("Grafik");
        CalendarConfig calendar = config.getCalendar();
        int daysInMonth = calendar.daysInMonth();

        int lastColumn = daysInMonth + 3;
        int rowIndex = 0;

        rowIndex = createTitleRow(sheet, rowIndex, lastColumn,
                "Grafik pracy - " + section.getName() + " - " + capitalize(monthLabel), styles.title);

        rowIndex = createInfoRow(sheet, rowIndex, lastColumn,
                "Konfiguracja: " + config.getName()
                        + " | Sekcja: " + section.getName()
                        + " | Schedule ID: " + schedule.getId()
                        + " | Fitness: " + schedule.getFitness(),
                styles.info);

        rowIndex = createInfoRow(sheet, rowIndex, lastColumn,
                "Legenda: W = wolne / dzień zamknięty. Godziny zapisane jako start-koniec zmiany.",
                styles.info);

        rowIndex++;

        Row header = sheet.createRow(rowIndex++);
        createCell(header, 0, "Pracownik", styles.header);
        createCell(header, 1, "Etat godzin", styles.header);
        createCell(header, 2, "Zaplanowane", styles.header);

        for (int dayIndex = 0; dayIndex < daysInMonth; dayIndex++) {
            LocalDate date = calendar.dateAt(dayIndex);
            CellStyle dayStyle = isWeekend(date) ? styles.headerWeekend : styles.header;
            createCell(header, dayIndex + 3, formatDayLabel(date), dayStyle);
        }

        for (int employeeIndex = 0; employeeIndex < employees.size(); employeeIndex++) {
            EmployeeEntity employee = employees.get(employeeIndex);
            Row row = sheet.createRow(rowIndex++);
            createCell(row, 0, employee.getName() + " " + employee.getSurname(), styles.employeeName);
            createCell(row, 1, employee.getTotalHours(), styles.integer);
            createCell(row, 2, sumPlannedHours(schedule.getGenes().get(employeeIndex)), styles.integer);

            for (int dayIndex = 0; dayIndex < daysInMonth; dayIndex++) {
                String shiftLabel = formatShiftLabel(schedule, config, employee, employeeIndex, dayIndex);
                CellStyle style = resolveScheduleCellStyle(styles, calendar, dayIndex, shiftLabel);
                createCell(row, dayIndex + 3, shiftLabel, style);
            }
        }

        Row totalRow = sheet.createRow(rowIndex);
        createCell(totalRow, 0, "Suma godzin / dzień", styles.summaryLabel);
        createCell(totalRow, 1, "", styles.summaryLabel);
        createCell(totalRow, 2, totalPlannedHours(schedule), styles.summaryNumber);

        for (int dayIndex = 0; dayIndex < daysInMonth; dayIndex++) {
            int totalDayHours = totalDayHours(schedule, dayIndex);
            CellStyle style = calendar.isClosedDay(dayIndex) ? styles.closedSummary : styles.summaryNumber;
            createCell(totalRow, dayIndex + 3, totalDayHours, style);
        }

        freezeTopRowAndEmployeeColumns(sheet, 4, 5);
        sheet.setColumnWidth(0, 24 * 256);
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(2, 12 * 256);
        for (int dayIndex = 0; dayIndex < daysInMonth; dayIndex++) {
            sheet.setColumnWidth(dayIndex + 3, 12 * 256);
        }
    }

    private void createStaffingSheet(HSSFWorkbook workbook,
            WorkbookStyles styles,
            ScheduleEntity schedule,
            ScheduleConfigEntity config,
            SectionEntity section,
            List<EmployeeEntity> employees,
            String monthLabel) {
        Sheet sheet = workbook.createSheet("Obsada");
        CalendarConfig calendar = config.getCalendar();

        int earliestOpenHour = findEarliestOpenHour(config);
        int latestCloseHour = findLatestCloseHour(config);
        int hourSlots = latestCloseHour - earliestOpenHour;
        int lastColumn = 5 + hourSlots;

        int rowIndex = 0;
        rowIndex = createTitleRow(sheet, rowIndex, lastColumn,
                "Obsada godzinowa - " + section.getName() + " - " + capitalize(monthLabel), styles.title);
        rowIndex = createInfoRow(sheet, rowIndex, lastColumn,
                "Arkusz pokazuje liczbę osób zaplanowanych na każdą godzinę działania sklepu.", styles.info);
        rowIndex++;

        Row header = sheet.createRow(rowIndex++);
        createCell(header, 0, "Data", styles.header);
        createCell(header, 1, "Dzień", styles.header);
        createCell(header, 2, "Godziny sklepu", styles.header);
        createCell(header, 3, "Target %", styles.header);
        createCell(header, 4, "Peak", styles.header);

        for (int hour = earliestOpenHour; hour < latestCloseHour; hour++) {
            createCell(header, 5 + (hour - earliestOpenHour), formatHourRange(hour), styles.header);
        }

        for (int dayIndex = 0; dayIndex < calendar.daysInMonth(); dayIndex++) {
            LocalDate date = calendar.dateAt(dayIndex);
            Row row = sheet.createRow(rowIndex++);

            createCell(row, 0, date.format(DAY_FULL_FORMAT), styles.date);
            createCell(row, 1, date.getDayOfWeek().getDisplayName(TextStyle.FULL, POLISH), styles.text);

            if (calendar.isClosedDay(dayIndex)) {
                createCell(row, 2, "Zamknięte", styles.closedCell);
                createCell(row, 3, "", styles.closedCell);
                createCell(row, 4, "", styles.closedCell);
                for (int hour = earliestOpenHour; hour < latestCloseHour; hour++) {
                    createCell(row, 5 + (hour - earliestOpenHour), "", styles.closedCell);
                }
                continue;
            }

            int openHour = config.getStoreHours().openHour(date.getDayOfWeek());
            int closeHour = config.getStoreHours().closeHour(date.getDayOfWeek());
            int[] peakHours = config.getStaffingTargets().peakHoursArray(date.getDayOfWeek(), openHour, closeHour);
            int[] staffing = calculateHourlyStaffing(schedule, config, dayIndex, earliestOpenHour, latestCloseHour);

            createCell(row, 2, formatHourRange(openHour, closeHour), styles.text);
            createCell(row, 3, formatStaffingTarget(config.getStaffingTargets(), date.getDayOfWeek(), employees.size()),
                    styles.centerText);
            createCell(row, 4, formatPeakWindow(config.getStaffingTargets(), date.getDayOfWeek()), styles.centerText);

            for (int hour = earliestOpenHour; hour < latestCloseHour; hour++) {
                int column = 5 + (hour - earliestOpenHour);

                if (hour < openHour || hour >= closeHour) {
                    createCell(row, column, "", styles.outsideStore);
                    continue;
                }

                int slotIndex = hour - openHour;
                boolean isPeak = slotIndex >= 0 && slotIndex < peakHours.length && peakHours[slotIndex] == 1;
                CellStyle style = isPeak ? styles.peakNumber : styles.integer;
                createCell(row, column, staffing[hour - earliestOpenHour], style);
            }
        }

        freezeTopRowAndEmployeeColumns(sheet, 5, 4);
        sheet.setColumnWidth(0, 12 * 256);
        sheet.setColumnWidth(1, 14 * 256);
        sheet.setColumnWidth(2, 16 * 256);
        sheet.setColumnWidth(3, 10 * 256);
        sheet.setColumnWidth(4, 14 * 256);
        for (int hour = earliestOpenHour; hour < latestCloseHour; hour++) {
            sheet.setColumnWidth(5 + (hour - earliestOpenHour), 10 * 256);
        }
    }

    private void createCoverageSheet(HSSFWorkbook workbook,
            WorkbookStyles styles,
            ScheduleEntity schedule,
            ScheduleConfigEntity config,
            SectionEntity section,
            List<EmployeeEntity> employees,
            String monthLabel) {
        Sheet sheet = workbook.createSheet("Coverage");
        CalendarConfig calendar = config.getCalendar();

        int lastColumn = 9;
        int rowIndex = 0;
        rowIndex = createTitleRow(sheet, rowIndex, lastColumn,
                "Coverage - " + section.getName() + " - " + capitalize(monthLabel), styles.title);
        rowIndex = createInfoRow(sheet, rowIndex, lastColumn,
                "Porównanie wymaganej i rzeczywistej obsady dla każdego dnia miesiąca.", styles.info);
        rowIndex++;

        Row header = sheet.createRow(rowIndex++);
        createCell(header, 0, "Data", styles.header);
        createCell(header, 1, "Dzień", styles.header);
        createCell(header, 2, "Status", styles.header);
        createCell(header, 3, "Target %", styles.header);
        createCell(header, 4, "Wymagana liczba osób", styles.header);
        createCell(header, 5, "Zaplanowana liczba osób", styles.header);
        createCell(header, 6, "Coverage osób", styles.header);
        createCell(header, 7, "Godziny otwarcia", styles.header);
        createCell(header, 8, "Zaplanowane godziny", styles.header);
        createCell(header, 9, "Coverage godzin", styles.header);

        int totalRequiredEmployees = 0;
        int totalActualEmployees = 0;
        int totalOpenHours = 0;
        int totalPlannedHours = 0;

        for (int dayIndex = 0; dayIndex < calendar.daysInMonth(); dayIndex++) {
            LocalDate date = calendar.dateAt(dayIndex);
            Row row = sheet.createRow(rowIndex++);

            createCell(row, 0, date.format(DAY_FULL_FORMAT), styles.date);
            createCell(row, 1, date.getDayOfWeek().getDisplayName(TextStyle.FULL, POLISH), styles.text);

            if (calendar.isClosedDay(dayIndex)) {
                createCell(row, 2, "Zamknięte", styles.closedCell);
                createCell(row, 3, "", styles.closedCell);
                createCell(row, 4, 0, styles.closedSummary);
                createCell(row, 5, 0, styles.closedSummary);
                createCell(row, 6, "-", styles.closedSummary);
                createCell(row, 7, 0, styles.closedSummary);
                createCell(row, 8, 0, styles.closedSummary);
                createCell(row, 9, "-", styles.closedSummary);
                continue;
            }

            DayOfWeek dayOfWeek = date.getDayOfWeek();
            int requiredEmployees = config.getStaffingTargets().requiredEmployees(dayOfWeek, employees.size());
            int actualEmployees = countWorkingEmployees(schedule, dayIndex);
            int openHours = config.getStoreHours().closeHour(dayOfWeek) - config.getStoreHours().openHour(dayOfWeek);
            int plannedHours = totalDayHours(schedule, dayIndex);

            totalRequiredEmployees += requiredEmployees;
            totalActualEmployees += actualEmployees;
            totalOpenHours += openHours;
            totalPlannedHours += plannedHours;

            createCell(row, 2, "Otwarte", styles.text);
            createCell(row, 3, formatStaffingTarget(config.getStaffingTargets(), dayOfWeek, employees.size()),
                    styles.centerText);
            createCell(row, 4, requiredEmployees, styles.integer);
            createCell(row, 5, actualEmployees, styles.integer);
            createCell(row, 6, formatPercent(actualEmployees, requiredEmployees),
                    resolveCoverageStyle(styles, actualEmployees, requiredEmployees));
            createCell(row, 7, openHours, styles.integer);
            createCell(row, 8, plannedHours, styles.integer);
            createCell(row, 9, formatPercent(plannedHours, openHours),
                    resolveCoverageStyle(styles, plannedHours, openHours));
        }

        Row summary = sheet.createRow(rowIndex);
        createCell(summary, 0, "SUMA", styles.summaryLabel);
        createCell(summary, 1, "", styles.summaryLabel);
        createCell(summary, 2, "", styles.summaryLabel);
        createCell(summary, 3, "", styles.summaryLabel);
        createCell(summary, 4, totalRequiredEmployees, styles.summaryNumber);
        createCell(summary, 5, totalActualEmployees, styles.summaryNumber);
        createCell(summary, 6, formatPercent(totalActualEmployees, totalRequiredEmployees),
                resolveCoverageStyle(styles, totalActualEmployees, totalRequiredEmployees));
        createCell(summary, 7, totalOpenHours, styles.summaryNumber);
        createCell(summary, 8, totalPlannedHours, styles.summaryNumber);
        createCell(summary, 9, formatPercent(totalPlannedHours, totalOpenHours),
                resolveCoverageStyle(styles, totalPlannedHours, totalOpenHours));

        freezeTopRowAndEmployeeColumns(sheet, 3, 4);
        for (int i = 0; i <= 9; i++) {
            sheet.setColumnWidth(i, switch (i) {
                case 0 -> 12 * 256;
                case 1 -> 14 * 256;
                case 2 -> 12 * 256;
                case 3 -> 10 * 256;
                case 4, 5 -> 18 * 256;
                case 6, 9 -> 14 * 256;
                case 7, 8 -> 16 * 256;
                default -> 14 * 256;
            });
        }
    }

    private int createTitleRow(Sheet sheet, int rowIndex, int lastColumn, String text, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        createCell(row, 0, text, style);
        mergeRow(sheet, rowIndex, 0, lastColumn);
        return rowIndex + 1;
    }

    private int createInfoRow(Sheet sheet, int rowIndex, int lastColumn, String text, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        createCell(row, 0, text, style);
        mergeRow(sheet, rowIndex, 0, lastColumn);
        return rowIndex + 1;
    }

    private void mergeRow(Sheet sheet, int rowIndex, int firstColumn, int lastColumn) {
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, firstColumn, lastColumn));
    }

    private void freezeTopRowAndEmployeeColumns(Sheet sheet, int firstScrollableColumn, int firstScrollableRow) {
        sheet.createFreezePane(firstScrollableColumn, firstScrollableRow);
    }

    private WorkbookStyles createStyles(HSSFWorkbook workbook, List<SectionEntity> sections) {
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);

        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        CellStyle title = bordered(workbook);
        title.setFont(titleFont);
        title.setAlignment(HorizontalAlignment.LEFT);
        title.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle mainHeader = cloneStyle(workbook, title);
        mainHeader.setAlignment(HorizontalAlignment.CENTER);
        mainHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        mainHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle info = bordered(workbook);
        info.setAlignment(HorizontalAlignment.LEFT);
        info.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle header = bordered(workbook);
        header.setFont(headerFont);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle headerMedium = cloneStyle(workbook, header);
        setBorder(headerMedium, BorderStyle.MEDIUM);

        CellStyle employeeHeader = cloneStyle(workbook, header);
        employeeHeader.setFont(headerFont);

        CellStyle employeeHeaderMedium = cloneStyle(workbook, employeeHeader);
        setBorder(employeeHeaderMedium, BorderStyle.MEDIUM);

        CellStyle subHeader = cloneStyle(workbook, header);
        subHeader.setFont(workbook.createFont());

        CellStyle subHeaderMedium = cloneStyle(workbook, subHeader);
        setBorder(subHeaderMedium, BorderStyle.MEDIUM);

        CellStyle headerWeekend = cloneStyle(workbook, header);
        headerWeekend.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());

        CellStyle employeeName = bordered(workbook);
        employeeName.setAlignment(HorizontalAlignment.LEFT);

        CellStyle text = bordered(workbook);
        text.setAlignment(HorizontalAlignment.LEFT);

        CellStyle centerText = bordered(workbook);
        centerText.setAlignment(HorizontalAlignment.CENTER);

        CellStyle date = cloneStyle(workbook, text);
        date.setAlignment(HorizontalAlignment.CENTER);

        CellStyle integer = bordered(workbook);
        integer.setAlignment(HorizontalAlignment.CENTER);

        CellStyle normalDay = cloneStyle(workbook, centerText);
        normalDay.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        normalDay.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle normalDayMedium = cloneStyle(workbook, normalDay);
        setBorder(normalDayMedium, BorderStyle.MEDIUM);

        CellStyle saturday = cloneStyle(workbook, centerText);
        saturday.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        saturday.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle saturdayMedium = cloneStyle(workbook, saturday);
        setBorder(saturdayMedium, BorderStyle.MEDIUM);

        CellStyle closedDay = cloneStyle(workbook, centerText);
        closedDay.setFont(headerFont);
        closedDay.setFillForegroundColor(IndexedColors.RED.getIndex());
        closedDay.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle closedDayMedium = cloneStyle(workbook, closedDay);
        setBorder(closedDayMedium, BorderStyle.MEDIUM);

        CellStyle tradingSunday = cloneStyle(workbook, centerText);
        tradingSunday.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        tradingSunday.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle tradingSundayMedium = cloneStyle(workbook, tradingSunday);
        setBorder(tradingSundayMedium, BorderStyle.MEDIUM);

        CellStyle workCell = cloneStyle(workbook, centerText);
        workCell.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        workCell.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle freeCell = cloneStyle(workbook, centerText);
        freeCell.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        freeCell.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle vacationCell = cloneStyle(workbook, centerText);
        vacationCell.setFont(boldFont);
        vacationCell.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        vacationCell.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle closedCell = cloneStyle(workbook, centerText);
        closedCell.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        closedCell.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle outsideStore = cloneStyle(workbook, centerText);
        outsideStore.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        outsideStore.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle peakNumber = cloneStyle(workbook, integer);
        peakNumber.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        peakNumber.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle summaryLabel = cloneStyle(workbook, header);
        summaryLabel.setAlignment(HorizontalAlignment.LEFT);

        CellStyle summaryNumber = cloneStyle(workbook, integer);
        summaryNumber.setFont(boldFont);

        CellStyle summaryNumberMedium = cloneStyle(workbook, summaryNumber);
        setBorder(summaryNumberMedium, BorderStyle.MEDIUM);

        CellStyle total = cloneStyle(workbook, summaryNumber);
        total.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        total.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle totalMedium = cloneStyle(workbook, total);
        setBorder(totalMedium, BorderStyle.MEDIUM);

        CellStyle closedSummary = cloneStyle(workbook, summaryNumber);
        closedSummary.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        closedSummary.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle coverageGood = cloneStyle(workbook, centerText);
        coverageGood.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        coverageGood.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle coverageWarning = cloneStyle(workbook, centerText);
        coverageWarning.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        coverageWarning.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle coverageBad = cloneStyle(workbook, centerText);
        coverageBad.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        coverageBad.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        List<CellStyle> sectionHeaders = createSectionHeaderStyles(workbook, sections, titleFont);

        return new WorkbookStyles(
                title,
                mainHeader,
                info,
                header,
                headerMedium,
                employeeHeader,
                employeeHeaderMedium,
                subHeader,
                subHeaderMedium,
                headerWeekend,
                employeeName,
                text,
                centerText,
                date,
                integer,
                normalDay,
                normalDayMedium,
                saturday,
                saturdayMedium,
                closedDay,
                closedDayMedium,
                tradingSunday,
                tradingSundayMedium,
                workCell,
                freeCell,
                vacationCell,
                closedCell,
                outsideStore,
                peakNumber,
                summaryLabel,
                summaryNumber,
                summaryNumberMedium,
                total,
                totalMedium,
                closedSummary,
                coverageGood,
                coverageWarning,
                coverageBad,
                sectionHeaders);
    }

    private CellStyle bordered(HSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        setBorder(style, BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private void setBorder(CellStyle style, BorderStyle borderStyle) {
        style.setBorderTop(borderStyle);
        style.setBorderBottom(borderStyle);
        style.setBorderLeft(borderStyle);
        style.setBorderRight(borderStyle);
    }

    private CellStyle cloneStyle(HSSFWorkbook workbook, CellStyle base) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(base);
        return style;
    }

    private List<CellStyle> createSectionHeaderStyles(HSSFWorkbook workbook,
            List<SectionEntity> sections,
            Font titleFont) {
        List<CellStyle> sectionHeaders = new ArrayList<>();
        for (int i = 0; i < Math.max(1, sections.size()); i++) {
            CellStyle style = bordered(workbook);
            style.setFont(titleFont);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setFillForegroundColor(SECTION_COLORS[i % SECTION_COLORS.length].getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorder(style, BorderStyle.MEDIUM);
            sectionHeaders.add(style);
        }
        return sectionHeaders;
    }

    private CellStyle sectionHeaderStyle(WorkbookStyles styles, int sectionIndex) {
        if (styles.sectionHeaders.isEmpty()) {
            return styles.headerMedium;
        }
        return styles.sectionHeaders.get(sectionIndex % styles.sectionHeaders.size());
    }

    private CellStyle templateDayStyle(WorkbookStyles styles,
            CalendarConfig calendar,
            int dayIndex,
            boolean mediumBorder) {
        LocalDate date = calendar.dateAt(dayIndex);
        boolean closed = calendar.isClosedDay(date);
        boolean tradingSunday = date.getDayOfWeek() == DayOfWeek.SUNDAY
                && calendar.tradingSundays().contains(date);

        if (closed) {
            return mediumBorder ? styles.closedDayMedium : styles.closedDay;
        }
        if (tradingSunday) {
            return mediumBorder ? styles.tradingSundayMedium : styles.tradingSunday;
        }
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return mediumBorder ? styles.saturdayMedium : styles.saturday;
        }
        return mediumBorder ? styles.normalDayMedium : styles.normalDay;
    }

    private void applyStyleToMergedRegion(Row row, int startColumn, int endColumn, CellStyle style) {
        for (int column = startColumn; column <= endColumn; column++) {
            Cell cell = row.getCell(column);
            if (cell == null) {
                cell = row.createCell(column);
            }
            cell.setCellStyle(style);
        }
    }

    private void applyStyleToMergedRegion(Sheet sheet,
            int startRow,
            int endRow,
            int column,
            CellStyle style) {
        for (int rowIndex = startRow; rowIndex <= endRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            Cell cell = row.getCell(column);
            if (cell == null) {
                cell = row.createCell(column);
            }
            cell.setCellStyle(style);
        }
    }

    private CellStyle resolveScheduleCellStyle(WorkbookStyles styles,
            CalendarConfig calendar,
            int dayIndex,
            String shiftLabel) {
        if (calendar.isClosedDay(dayIndex)) {
            return styles.closedCell;
        }
        if ("U".equals(shiftLabel)) {
            return styles.vacationCell;
        }
        if ("W".equals(shiftLabel)) {
            return styles.freeCell;
        }
        return styles.workCell;
    }

    private CellStyle resolveCoverageStyle(WorkbookStyles styles, int actual, int required) {
        if (required <= 0) {
            return styles.centerText;
        }

        double ratio = actual / (double) required;
        if (ratio < 1.0) {
            return styles.coverageBad;
        }
        if (ratio > 1.0) {
            return styles.coverageWarning;
        }
        return styles.coverageGood;
    }

    private String formatShiftLabel(ScheduleEntity schedule,
            ScheduleConfigEntity config,
            EmployeeEntity employee,
            int employeeIndex,
            int dayIndex) {
        CalendarConfig calendar = config.getCalendar();

        if (calendar.isClosedDay(dayIndex)) {
            return "W";
        }
        if (isVacation(schedule, employee, dayIndex)) {
            return "U";
        }

        Integer length = safeValue(schedule.getGenes().get(employeeIndex).get(dayIndex));
        if (length <= 0) {
            return "W";
        }

        int start = safeValue(schedule.getShiftStarts().get(employeeIndex).get(dayIndex));
        if (start <= 0) {
            start = config.getStoreHours().openHour(calendar.dayOfWeekAt(dayIndex));
        }

        return formatHour(start) + "-" + formatHour(start + length);
    }

    private int[] calculateHourlyStaffing(ScheduleEntity schedule, ScheduleConfigEntity config, int dayIndex,
            int earliestOpenHour, int latestCloseHour) {
        int[] staffing = new int[latestCloseHour - earliestOpenHour];

        for (int employeeIndex = 0; employeeIndex < schedule.getGenes().size(); employeeIndex++) {
            int length = safeValue(schedule.getGenes().get(employeeIndex).get(dayIndex));
            if (length <= 0) {
                continue;
            }

            int start = safeValue(schedule.getShiftStarts().get(employeeIndex).get(dayIndex));
            if (start <= 0) {
                start = config.getStoreHours().openHour(config.getCalendar().dayOfWeekAt(dayIndex));
            }

            for (int hour = start; hour < start + length; hour++) {
                int arrayIndex = hour - earliestOpenHour;
                if (arrayIndex >= 0 && arrayIndex < staffing.length) {
                    staffing[arrayIndex]++;
                }
            }
        }

        return staffing;
    }

    private int countWorkingEmployees(ScheduleEntity schedule, int dayIndex) {
        int count = 0;
        for (List<Integer> row : schedule.getGenes()) {
            if (safeValue(row.get(dayIndex)) > 0) {
                count++;
            }
        }
        return count;
    }

    private boolean isVacation(EmployeeEntity employee, int dayIndex) {
        return employee != null
                && employee.getVacations() != null
                && employee.getVacations().contains(dayIndex);
    }

    private boolean isVacation(ScheduleEntity schedule, EmployeeEntity employee, int dayIndex) {
        return isVacation(employee, dayIndex) && !hasProtectedWorkOverride(schedule, employee, dayIndex);
    }

    private boolean hasProtectedWorkOverride(ScheduleEntity schedule, EmployeeEntity employee, int dayIndex) {
        if (schedule == null || employee == null || schedule.getProtectedOverrides() == null) {
            return false;
        }
        return schedule.getProtectedOverrides().stream()
                .anyMatch(override -> employee.getId().equals(override.employeeId())
                        && override.day() == dayIndex + 1
                        && ProtectedScheduleOverride.ALLOW_WORK.equals(override.type()));
    }

    private int totalDayHours(ScheduleEntity schedule, int dayIndex) {
        int total = 0;
        for (List<Integer> row : schedule.getGenes()) {
            total += safeValue(row.get(dayIndex));
        }
        return total;
    }

    private int totalPlannedHours(ScheduleEntity schedule) {
        int total = 0;
        for (List<Integer> row : schedule.getGenes()) {
            total += sumPlannedHours(row);
        }
        return total;
    }

    private int sumPlannedHours(List<Integer> row) {
        int total = 0;
        for (Integer value : row) {
            total += safeValue(value);
        }
        return total;
    }

    private int findEarliestOpenHour(ScheduleConfigEntity config) {
        return config.getStoreHours().hours().values().stream()
                .map(StoreHours.DayHours::open)
                .mapToInt(value -> value.getHour())
                .min()
                .orElse(0);
    }

    private int findLatestCloseHour(ScheduleConfigEntity config) {
        return config.getStoreHours().hours().values().stream()
                .map(StoreHours.DayHours::close)
                .mapToInt(value -> value.getHour())
                .max()
                .orElse(24);
    }

    private String formatStaffingTarget(StaffingTargets staffingTargets, DayOfWeek dayOfWeek, int employeeCount) {
        if (staffingTargets.usesCount(dayOfWeek)) {
            return staffingTargets.countFor(dayOfWeek) + " os.";
        }
        return staffingTargets.percentFor(dayOfWeek) + "%";
    }

    private String formatPeakWindow(StaffingTargets staffingTargets, DayOfWeek dayOfWeek) {
        StaffingTargets.PeakWindow peakWindow = staffingTargets.peakFor(dayOfWeek);
        if (peakWindow == null) {
            return "-";
        }
        return peakWindow.start().toString() + "-" + peakWindow.end().toString();
    }

    private String buildFileName(SectionEntity section, ScheduleConfigEntity config, Long scheduleId) {
        String sectionPart = sanitizeFilePart(section.getName());
        String monthPart = config.getCalendar().yearMonth().format(FILE_MONTH_FORMAT);
        return "grafik-" + sectionPart + "-" + monthPart + "-" + scheduleId + ".xls";
    }

    private String sanitizeFilePart(String value) {
        return value == null
                ? "sekcja"
                : value.trim()
                        .toLowerCase(POLISH)
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("(^-+|-+$)", "");
    }

    private String formatMonthLabel(CalendarConfig calendar) {
        return calendar.yearMonth().format(SHEET_MONTH_FORMAT);
    }

    private String formatDayLabel(LocalDate date) {
        return capitalize(date.format(DAY_LABEL_FORMAT));
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private String formatHourRange(int startHour) {
        return formatHour(startHour) + "-" + formatHour(startHour + 1);
    }

    private String formatHourRange(int startHour, int endHour) {
        return formatHour(startHour) + "-" + formatHour(endHour);
    }

    private String formatHour(int hour) {
        return String.format("%02d:00", hour);
    }

    private String formatPercent(int actual, int required) {
        if (required <= 0) {
            return "-";
        }
        double percent = (actual * 100.0) / required;
        return String.format(POLISH, "%.0f%%", percent);
    }

    private int safeValue(Integer value) {
        return value == null ? 0 : value;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(POLISH) + value.substring(1);
    }

    private void createCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int columnIndex, int value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    public record ExportedScheduleFile(
            String fileName,
            String contentType,
            byte[] content) {
    }

    private record WorkbookStyles(
            CellStyle title,
            CellStyle mainHeader,
            CellStyle info,
            CellStyle header,
            CellStyle headerMedium,
            CellStyle employeeHeader,
            CellStyle employeeHeaderMedium,
            CellStyle subHeader,
            CellStyle subHeaderMedium,
            CellStyle headerWeekend,
            CellStyle employeeName,
            CellStyle text,
            CellStyle centerText,
            CellStyle date,
            CellStyle integer,
            CellStyle normalDay,
            CellStyle normalDayMedium,
            CellStyle saturday,
            CellStyle saturdayMedium,
            CellStyle closedDay,
            CellStyle closedDayMedium,
            CellStyle tradingSunday,
            CellStyle tradingSundayMedium,
            CellStyle workCell,
            CellStyle freeCell,
            CellStyle vacationCell,
            CellStyle closedCell,
            CellStyle outsideStore,
            CellStyle peakNumber,
            CellStyle summaryLabel,
            CellStyle summaryNumber,
            CellStyle summaryNumberMedium,
            CellStyle total,
            CellStyle totalMedium,
            CellStyle closedSummary,
            CellStyle coverageGood,
            CellStyle coverageWarning,
            CellStyle coverageBad,
            List<CellStyle> sectionHeaders) {
    }

    private record ShiftWindow(int start, int end, int length) {
    }
}
