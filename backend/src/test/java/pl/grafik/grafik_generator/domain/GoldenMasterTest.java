package pl.grafik.grafik_generator.domain;

import org.junit.jupiter.api.Test;
import pl.grafik.grafik_generator.domain.context.*;
import pl.grafik.grafik_generator.domain.ga.Population;
import pl.grafik.grafik_generator.domain.model.Employee;
import pl.grafik.grafik_generator.domain.model.Section;
import pl.grafik.grafik_generator.domain.scheduleGenerator.Schedule;
import pl.grafik.grafik_generator.domain.shiftPoolGenerator.ShiftPool;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GoldenMasterTest {

    private static final long SEED = 42L;
    private static final String SNAPSHOT_RESOURCE = "golden-master/snapshot.txt";

    @Test
    void gaProducesStableResult() throws IOException {
        GenerationContext ctx = buildContext();
        Section section = buildSection(ctx);

        Population population = new Population(section, 50, ctx, new Random(SEED));
        Schedule best = population.run(200, 3, 3);

        String actual = serialize(best);

        Path snapshotPath = resolveSnapshotPath();
        if (!Files.exists(snapshotPath)) {
            Files.writeString(snapshotPath, actual);
            System.out.println("[Golden Master] Snapshot zapisany: " + snapshotPath);
            return;
        }

        String expected = Files.readString(snapshotPath);
        assertEquals(expected, actual,
                "Wynik algorytmu GA różni się od snapshotu. Uruchom z -Dgolden.rewrite=true żeby zaktualizować.");
    }

    private GenerationContext buildContext() {
        YearMonth month = YearMonth.of(2026, 4);

        Map<DayOfWeek, StoreHours.DayHours> hours = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day != DayOfWeek.SUNDAY) {
                hours.put(day, new StoreHours.DayHours(LocalTime.of(10, 0), LocalTime.of(22, 0)));
            }
        }
        StoreHours storeHours = new StoreHours(hours);

        Map<DayOfWeek, Integer> staffingPercent = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            staffingPercent.put(day, 50);
        }
        StaffingTargets staffingTargets = new StaffingTargets(staffingPercent, Map.of(), Map.of());

        CalendarConfig calendar = new CalendarConfig(month, Set.of(), Set.of());

        ShiftRules shiftRules = new ShiftRules(List.of(8), 5, false, 2);

        VacationConfig vacationConfig = new VacationConfig(
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                8, true);

        GaParameters gaParameters = new GaParameters(50, 200, 3, 3, 0.05);

        return new GenerationContext(storeHours, staffingTargets, calendar, shiftRules, vacationConfig, gaParameters);
    }

    private Section buildSection(GenerationContext ctx) {
        ShiftPool pool = new ShiftPool(ctx.shiftRules());

        Employee alice = new Employee("Alice", "Smith", "test", 160, 20,
                List.of(), List.of(), pool, ctx.shiftRules(), ctx.calendar(), ctx.vacationConfig());
        Employee bob = new Employee("Bob", "Jones", "test", 160, 20,
                List.of(), List.of(), pool, ctx.shiftRules(), ctx.calendar(), ctx.vacationConfig());

        return new Section("test", List.of(alice, bob));
    }

    private String serialize(Schedule schedule) {
        StringBuilder sb = new StringBuilder();
        sb.append("fitness:").append(schedule.getFitness()).append("\n");
        for (int[] row : schedule.getGenes()) {
            sb.append(Arrays.stream(row)
                    .mapToObj(Integer::toString)
                    .collect(Collectors.joining(" ")));
            sb.append("\n");
        }
        return sb.toString();
    }

    private Path resolveSnapshotPath() {
        URL resource = getClass().getClassLoader().getResource(SNAPSHOT_RESOURCE);
        if (resource != null) {
            return Paths.get(resource.getPath());
        }
        // plik nie istnieje jeszcze — wskaż na katalog testowy
        return Paths.get("backend/src/test/resources/" + SNAPSHOT_RESOURCE);
    }
}
