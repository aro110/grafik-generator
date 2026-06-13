package pl.grafik.grafik_generator.domain.ga;

import pl.grafik.grafik_generator.domain.context.GenerationContext;
import pl.grafik.grafik_generator.domain.model.Employee;
import pl.grafik.grafik_generator.domain.model.Section;
import pl.grafik.grafik_generator.domain.scheduleGenerator.Schedule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Population {
    private final Section section;
    private final GenerationContext ctx;
    private final Random random;
    private final int size;
    private List<Schedule> scheduleList;

    public Population(Section section, int size, GenerationContext ctx, Random random) {
        this.section = section;
        this.size = size;
        this.ctx = ctx;
        this.random = random;
        this.scheduleList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            scheduleList.add(new Schedule(section, ctx, random));
        }
    }

    public Schedule run(int generations, int eliteCount, int tournamentSize) {
        for (int i = 0; i < generations; i++) {
            evolve(eliteCount, tournamentSize);
            scheduleList.sort(Comparator.comparingDouble(Schedule::getFitness));
            Schedule best = scheduleList.getFirst();

            if (best.getFitness() == 0) {
                return best;
            }
        }
        return scheduleList.getFirst();
    }

    private Schedule tournamentSelection(int tournamentSize) {
        Schedule bestSchedule = null;
        for (int i = 0; i < tournamentSize; i++) {
            Schedule candidate = scheduleList.get(random.nextInt(size));
            if (bestSchedule == null || candidate.getFitness() < bestSchedule.getFitness()) {
                bestSchedule = candidate;
            }
        }
        return bestSchedule;
    }

    private void evolve(int eliteCount, int tournamentSize) {
        List<Schedule> evolvedSchedules = new ArrayList<>();
        scheduleList.sort(Comparator.comparingDouble(Schedule::getFitness));

        for (int i = 0; i < eliteCount; i++) {
            evolvedSchedules.add(scheduleList.get(i));
        }

        while (evolvedSchedules.size() < size) {
            Schedule parent1 = tournamentSelection(tournamentSize);
            Schedule parent2 = tournamentSelection(tournamentSize);
            Schedule child = crossover(parent1, parent2);
            mutate(child, 0.05);
            evolvedSchedules.add(child);
        }

        this.scheduleList = evolvedSchedules;
    }

    private Schedule crossover(Schedule parent1, Schedule parent2) {
        int[][] childGenes = new int[parent1.getGenes().length][];
        for (int i = 0; i < parent1.getGenes().length; i++) {
            if (random.nextBoolean()) {
                childGenes[i] = parent1.getGenes()[i].clone();
            } else {
                childGenes[i] = parent2.getGenes()[i].clone();
            }
        }
        return new Schedule(section, childGenes, ctx);
    }

    private Schedule mutate(Schedule schedule, double mutationRate) {
        int[][] genes = schedule.getGenes();
        List<Employee> employees = section.getEmployees();
        for (int i = 0; i < genes.length; i++) {
            if (random.nextDouble() < mutationRate) {
                List<Integer> availableFreeDays = new ArrayList<>();
                List<Integer> availableWorkDays = new ArrayList<>();

                for (int day = 0; day < genes[i].length; day++) {
                    boolean unavailable = employees.get(i).getDaysOff().contains(day)
                            || ctx.calendar().isClosedDay(day);
                    if (unavailable) {
                        continue;
                    }

                    if (genes[i][day] == 0) {
                        availableFreeDays.add(day);
                    } else {
                        availableWorkDays.add(day);
                    }
                }

                if (availableFreeDays.isEmpty() || availableWorkDays.isEmpty()) {
                    continue;
                }

                int day1 = availableFreeDays.get(random.nextInt(availableFreeDays.size()));
                int day2 = availableWorkDays.get(random.nextInt(availableWorkDays.size()));

                int temp = genes[i][day1];
                genes[i][day1] = genes[i][day2];
                genes[i][day2] = temp;
            }
        }
        schedule.calculateFitness();
        return schedule;
    }

    public List<Schedule> getScheduleList() {
        return scheduleList;
    }
}
