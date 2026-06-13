package pl.grafik.grafik_generator.domain.scheduleGenerator;

import pl.grafik.grafik_generator.domain.context.GenerationContext;
import pl.grafik.grafik_generator.domain.model.Employee;
import pl.grafik.grafik_generator.domain.model.Section;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShiftAssigner {

    private static final long SCORE_COVER_OPEN_HOUR = 1_000_000L;
    private static final long SCORE_COVER_PEAK_HOUR = 10_000L;
    private static final long PENALTY_OVERLAP = 100L;
    private static final long PENALTY_START_LIMIT = 1_000L;
    private static final long SCORE_PEAK_FAIRNESS = 10L;
    private static final int BEAM_WIDTH = 10_000;

    private final GenerationContext ctx;
    private final Map<Employee, Integer> cumulativePeakHours = new HashMap<>();

    public ShiftAssigner(GenerationContext ctx) {
        this.ctx = ctx;
    }

    public Map<Employee, Integer> assignStartTimes(Schedule schedule, Section section, int dayIndex) {
        Map<Employee, Integer> workingEmployees = new HashMap<>();
        for (int i = 0; i < section.getEmployees().size(); i++) {
            if (schedule.getGenes()[i][dayIndex] != 0) {
                Employee emp = section.getEmployees().get(i);
                workingEmployees.put(emp, schedule.getGenes()[i][dayIndex]);
            }
        }

        List<Map.Entry<Employee, Integer>> sortedEmployees = new ArrayList<>(workingEmployees.entrySet());
        sortedEmployees.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));

        int openHour = ctx.openHour(dayIndex);
        int closeHour = ctx.closeHour(dayIndex);
        int[] peakHours = ctx.peakHoursArray(dayIndex);

        AssignmentPlan plan = findBestPlan(sortedEmployees, openHour, closeHour, peakHours);
        for (int i = 0; i < sortedEmployees.size(); i++) {
            Employee emp = sortedEmployees.get(i).getKey();
            workingEmployees.put(emp, plan.starts()[i]);
            cumulativePeakHours.merge(emp, countPeakHours(plan.starts()[i], sortedEmployees.get(i).getValue(),
                    openHour, closeHour, peakHours), Integer::sum);
        }

        return workingEmployees;
    }

    private AssignmentPlan findBestPlan(List<Map.Entry<Employee, Integer>> employees,
            int openHour,
            int closeHour,
            int[] peakHours) {
        int totalSlots = closeHour - openHour;
        int maxPerStart = ctx.shiftRules().maxPeoplePerShiftStart();
        List<PartialPlan> states = List.of(new PartialPlan(new int[employees.size()], new int[totalSlots],
                new HashMap<>(), 0));

        double avgPeak = cumulativePeakHours.isEmpty() ? 0
                : cumulativePeakHours.values().stream().mapToInt(Integer::intValue).average().orElse(0);

        for (int employeeIndex = 0; employeeIndex < employees.size(); employeeIndex++) {
            Map.Entry<Employee, Integer> entry = employees.get(employeeIndex);
            int shiftLength = entry.getValue();
            int latestStart = Math.max(0, totalSlots - shiftLength);
            List<PartialPlan> nextStates = new ArrayList<>();

            for (PartialPlan state : states) {
                for (int start = 0; start <= latestStart; start++) {
                    nextStates.add(state.withAssignment(employeeIndex, openHour + start, start, shiftLength,
                            maxPerStart, peakHours, cumulativePeakHours.getOrDefault(entry.getKey(), 0), avgPeak));
                }
            }

            nextStates.sort((left, right) -> Long.compare(right.score(), left.score()));
            if (nextStates.size() > BEAM_WIDTH) {
                nextStates = new ArrayList<>(nextStates.subList(0, BEAM_WIDTH));
            }
            states = nextStates;
        }

        return states.stream()
                .max((left, right) -> Long.compare(left.finalScore(peakHours), right.finalScore(peakHours)))
                .map(state -> new AssignmentPlan(state.starts()))
                .orElseGet(() -> fallbackPlan(employees, openHour));
    }

    private AssignmentPlan fallbackPlan(List<Map.Entry<Employee, Integer>> employees, int openHour) {
        int[] starts = new int[employees.size()];
        for (int i = 0; i < starts.length; i++) {
            starts[i] = openHour;
        }
        return new AssignmentPlan(starts);
    }

    private int countPeakHours(int absoluteStart,
            int shiftLength,
            int openHour,
            int closeHour,
            int[] peakHours) {
        int start = absoluteStart - openHour;
        int totalSlots = closeHour - openHour;
        int shiftEnd = Math.min(start + shiftLength, totalSlots);
        int peakCount = 0;
        for (int slot = start; slot < shiftEnd; slot++) {
            if (slot >= 0 && peakHours[slot] == 1) {
                peakCount++;
            }
        }
        return peakCount;
    }

    private record AssignmentPlan(int[] starts) {
    }

    private record PartialPlan(
            int[] starts,
            int[] coverage,
            Map<Integer, Integer> startTimeCounts,
            long score) {

        private PartialPlan withAssignment(int employeeIndex,
                int absoluteStart,
                int relativeStart,
                int shiftLength,
                int maxPerStart,
                int[] peakHours,
                int empPeakSoFar,
                double avgPeak) {
            int[] nextStarts = starts.clone();
            int[] nextCoverage = coverage.clone();
            Map<Integer, Integer> nextStartTimeCounts = new HashMap<>(startTimeCounts);
            long delta = 0;
            int shiftEnd = Math.min(relativeStart + shiftLength, coverage.length);
            int peakHoursInShift = 0;

            for (int slot = relativeStart; slot < shiftEnd; slot++) {
                if (nextCoverage[slot] == 0) {
                    delta += SCORE_COVER_OPEN_HOUR;
                } else {
                    delta -= PENALTY_OVERLAP * nextCoverage[slot];
                }

                if (peakHours[slot] == 1) {
                    delta += SCORE_COVER_PEAK_HOUR;
                    peakHoursInShift++;
                }
                nextCoverage[slot]++;
            }

            int currentStartCount = nextStartTimeCounts.getOrDefault(absoluteStart, 0);
            if (maxPerStart > 0 && currentStartCount >= maxPerStart) {
                delta -= PENALTY_START_LIMIT;
            }
            nextStartTimeCounts.merge(absoluteStart, 1, Integer::sum);

            if (empPeakSoFar < avgPeak) {
                delta += peakHoursInShift * SCORE_PEAK_FAIRNESS;
            } else if (empPeakSoFar > avgPeak + 2) {
                delta -= peakHoursInShift * SCORE_PEAK_FAIRNESS;
            }

            nextStarts[employeeIndex] = absoluteStart;
            return new PartialPlan(nextStarts, nextCoverage, nextStartTimeCounts, score + delta);
        }

        private long finalScore(int[] peakHours) {
            long finalScore = score;
            for (int slot = 0; slot < coverage.length; slot++) {
                if (coverage[slot] == 0) {
                    finalScore -= SCORE_COVER_OPEN_HOUR * 2;
                    if (peakHours[slot] == 1) {
                        finalScore -= SCORE_COVER_PEAK_HOUR * 2;
                    }
                }
            }
            return finalScore;
        }
    }
}
