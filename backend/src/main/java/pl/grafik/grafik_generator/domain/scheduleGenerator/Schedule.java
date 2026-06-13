package pl.grafik.grafik_generator.domain.scheduleGenerator;

import pl.grafik.grafik_generator.domain.context.GenerationContext;
import pl.grafik.grafik_generator.domain.context.StoreHours;
import pl.grafik.grafik_generator.domain.ga.Chromosome;
import pl.grafik.grafik_generator.domain.model.Employee;
import pl.grafik.grafik_generator.domain.model.Section;
import pl.grafik.grafik_generator.domain.shiftPoolGenerator.ShiftCombination;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Schedule implements Chromosome {
    private final GenerationContext ctx;
    private final Random random;
    private final int[][] genes;
    private final int employees;
    private final int daysInMonth;
    private final DayOfWeek firstDay;
    private double fitness;

    private static final double PENALTY_DAY_OFF = 1000;
    private static final double PENALTY_NO_COVERAGE = 10_000;
    private static final double PENALTY_CONSECUTIVE_DAYS = 30;
    private static final double PENALTY_DAY_WEIGHT = 15;
    private static final double PENALTY_FREE_DISTRIBUTION = 25;

    public Schedule(Section section, GenerationContext ctx, Random random) {
        this.ctx = ctx;
        this.random = random;
        this.daysInMonth = ctx.daysInMonth();
        this.firstDay = ctx.calendar().firstDayOfWeek();
        this.employees = section.getEmployees().size();
        this.genes = new int[employees][daysInMonth];

        List<Integer> closedDays = ctx.calendar().closedDayIndices();
        for (int i = 0; i < employees; i++) {
            genes[i] = initRow(section.getEmployees().get(i), closedDays);
        }

        calculateFitness();
    }

    public Schedule(Section section, int[][] genes, GenerationContext ctx) {
        this.ctx = ctx;
        this.random = null;
        this.daysInMonth = ctx.daysInMonth();
        this.firstDay = ctx.calendar().firstDayOfWeek();
        this.employees = section.getEmployees().size();
        this.genes = genes;
        calculateFitness();
    }

    private int[] initRow(Employee employee, List<Integer> closedDays) {
        List<ShiftCombination> pool = employee.getShiftPool();
        ShiftCombination combo;

        if (employees <= 2) {
            List<ShiftCombination> highVariance = pool.stream()
                    .filter(sc -> sc.getStdDev() >= 2.0)
                    .toList();
            if (!highVariance.isEmpty()) {
                combo = highVariance.get(random.nextInt(highVariance.size()));
            } else {
                combo = pool.get(random.nextInt(pool.size()));
            }
        } else {
            combo = pool.get(random.nextInt(pool.size()));
        }

        int[] row = new int[daysInMonth];
        List<Integer> shifts = combo.getShifts();
        for (int i = 0; i < shifts.size(); i++) {
            row[i] = shifts.get(i);
        }

        shuffleArray(row);

        Set<Integer> allOff = new HashSet<>(closedDays);
        allOff.addAll(employee.getDaysOff());

        for (int offDay : allOff) {
            if (row[offDay] != 0) {
                for (int j = 0; j < row.length; j++) {
                    if (row[j] == 0 && !allOff.contains(j)) {
                        row[j] = row[offDay];
                        row[offDay] = 0;
                        break;
                    }
                }
            }
        }
        return row;
    }

    private void shuffleArray(int[] ar) {
        for (int i = ar.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    public double calculateFitness() {
        double total = 0;
        for (int day = 0; day < daysInMonth; day++) {
            total += checkDayCoverage(day);
            total += checkStaffingTarget(day);
            total += checkClosedDays(day);
        }

        for (int emp = 0; emp < employees; emp++) {
            total += checkConsecutiveDays(emp);
            total += checkWorkDistribution(emp);
        }

        this.fitness = total;
        return total;
    }

    private double checkConsecutiveDays(int employeeIndex) {
        int consecutiveCount = 0;
        int maxInARow = ctx.shiftRules().maxWorkingDaysInARow();
        for (int day = 0; day < daysInMonth; day++) {
            if (genes[employeeIndex][day] > 0) {
                consecutiveCount++;
                if (consecutiveCount > maxInARow) {
                    return PENALTY_CONSECUTIVE_DAYS;
                }
            } else {
                consecutiveCount = 0;
            }
        }

        return 0;
    }

    private double checkDayCoverage(int dayIndex) {
        LocalDate date = ctx.calendar().dateAt(dayIndex);
        if (ctx.calendar().isClosedDay(date))
            return 0;

        DayOfWeek day = firstDay.plus(dayIndex);
        StoreHours.DayHours dayHours = ctx.storeHours().forDay(day);
        int openHours = dayHours.close().getHour() - dayHours.open().getHour();

        int totalStaffHours = 0;
        for (int i = 0; i < employees; i++) {
            totalStaffHours += genes[i][dayIndex];
        }

        if (totalStaffHours < openHours) {
            return (openHours - totalStaffHours) * PENALTY_NO_COVERAGE;
        }

        return 0;
    }

    private double checkStaffingTarget(int dayIndex) {
        LocalDate date = ctx.calendar().dateAt(dayIndex);
        if (ctx.calendar().isClosedDay(date))
            return 0;

        double target = ctx.requiredEmployees(dayIndex, employees);

        int working = 0;
        for (int i = 0; i < employees; i++) {
            if (genes[i][dayIndex] > 0)
                working++;
        }

        double diff = Math.abs(working - target);
        return diff * PENALTY_DAY_WEIGHT;
    }

    private double checkClosedDays(int dayIndex) {
        LocalDate date = ctx.calendar().dateAt(dayIndex);
        if (!ctx.calendar().isClosedDay(date))
            return 0;

        double penalty = 0;
        for (int i = 0; i < employees; i++) {
            if (genes[i][dayIndex] > 0) {
                penalty += PENALTY_DAY_OFF;
            }
        }
        return penalty;
    }

    private double checkWorkDistribution(int employeeIndex) {
        int quarterLength = daysInMonth / 4;
        int[] workPerQuarter = new int[4];

        for (int day = 0; day < daysInMonth; day++) {
            if (genes[employeeIndex][day] != 0) {
                int quarter = Math.min(day / quarterLength, 3);
                workPerQuarter[quarter]++;
            }
        }

        double penalty = 0;
        for (int i = 0; i < 3; i++) {
            double diff = Math.abs(workPerQuarter[i] - workPerQuarter[i + 1]);
            penalty += diff * PENALTY_FREE_DISTRIBUTION;
        }
        return penalty;
    }

    @Override
    public double getFitness() {
        return fitness;
    }

    public int[][] getGenes() {
        return genes;
    }

    public int getDaysInMonth() {
        return daysInMonth;
    }

    public int getEmployees() {
        return employees;
    }
}
