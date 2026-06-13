package pl.grafik.grafik_generator.domain.context;

public record GenerationContext(
        StoreHours storeHours,
        StaffingTargets staffingTargets,
        CalendarConfig calendar,
        ShiftRules shiftRules,
        VacationConfig vacationConfig,
        GaParameters gaParameters) {

    public int daysInMonth() {
        return calendar.daysInMonth();
    }

    public boolean isClosedDay(int dayIndex) {
        return calendar.isClosedDay(dayIndex);
    }

    public int openHour(int dayIndex) {
        return storeHours.openHour(calendar.dayOfWeekAt(dayIndex));
    }

    public int closeHour(int dayIndex) {
        return storeHours.closeHour(calendar.dayOfWeekAt(dayIndex));
    }

    public int staffingPercent(int dayIndex) {
        return staffingTargets.percentFor(calendar.dayOfWeekAt(dayIndex));
    }

    public int requiredEmployees(int dayIndex, int sectionEmployeeCount) {
        return staffingTargets.requiredEmployees(calendar.dayOfWeekAt(dayIndex), sectionEmployeeCount);
    }

    public int[] peakHoursArray(int dayIndex) {
        var dow = calendar.dayOfWeekAt(dayIndex);
        return staffingTargets.peakHoursArray(
                dow,
                storeHours.openHour(dow),
                storeHours.closeHour(dow));
    }
}
