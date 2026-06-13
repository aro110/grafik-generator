package pl.grafik.grafik_generator.domain.shiftPoolGenerator;

import pl.grafik.grafik_generator.domain.context.ShiftRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShiftPool {
    private final List<Integer> shifts;
    private final Map<String, List<ShiftCombination>> cache = new HashMap<>();

    public ShiftPool(ShiftRules rules) {
        this.shifts = rules.shiftLengths();
    }

    public List<ShiftCombination> generateAll(int totalHours, int days) {
        String key = totalHours + "-" + days;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        List<List<Integer>> results = new ArrayList<>();
        backtrack(totalHours, days, 0, new ArrayList<>(), results);
        List<ShiftCombination> shiftCombinations = new ArrayList<>();
        for (List<Integer> combination : results) {
            shiftCombinations.add(new ShiftCombination(combination));
        }
        ShiftClusterer clusterer = new ShiftClusterer(shiftCombinations);
        List<ShiftCombination> representatives = clusterer.getRepresentatives(4);
        cache.put(key, representatives);
        return representatives;
    }

    private void backtrack(int remaining, int slotsLeft, int startIndex,
                           List<Integer> current, List<List<Integer>> results) {
        if (slotsLeft == 0) {
            if (remaining == 0) {
                results.add(new ArrayList<>(current));
            }
            return;
        }

        for (int i = startIndex; i < shifts.size(); i++) {
            int shift = shifts.get(i);

            int maxPossible = shifts.getLast() * slotsLeft;
            if (remaining > maxPossible) break;

            if (shift * slotsLeft > remaining) break;

            if (shift > remaining) break;

            current.add(shift);
            backtrack(remaining - shift, slotsLeft - 1, i, current, results);
            current.removeLast();
        }
    }
}
