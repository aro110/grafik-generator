package pl.grafik.grafik_generator.domain.context;

import java.util.List;

public record ShiftRules(List<Integer> shiftLengths, int maxWorkingDaysInARow, boolean grantFreeWeekend,
        int maxPeoplePerShiftStart) {
    public ShiftRules {
        if (shiftLengths == null || shiftLengths.isEmpty()) {
            throw new IllegalArgumentException("shiftLengths nie może być puste");
        }
        shiftLengths = List.copyOf(shiftLengths);

        if (maxWorkingDaysInARow <= 0) {
            throw new IllegalArgumentException("maxWorkingDaysInARow musi być > 0");
        }
    }

    public int minShift() {
        return shiftLengths.getFirst();
    }

    public int maxShift() {
        return shiftLengths.getLast();
    }
}
