package pl.grafik.grafik_generator.domain.context;

public record GaParameters(
        int populationSize,
        int generations,
        int eliteCount,
        int tournamentSize,
        double mutationRate) {
    public GaParameters {
        if (populationSize <= 0)
            throw new IllegalArgumentException("populationSize > 0");
        if (generations <= 0)
            throw new IllegalArgumentException("generations > 0");
        if (eliteCount < 0)
            throw new IllegalArgumentException("eliteCount >= 0");
        if (eliteCount >= populationSize)
            throw new IllegalArgumentException("eliteCount < populationSize");
        if (tournamentSize <= 0)
            throw new IllegalArgumentException("tournamentSize > 0");
        if (mutationRate < 0 || mutationRate > 1)
            throw new IllegalArgumentException("mutationRate 0-1");
    }

    public static GaParameters defaults() {
        return new GaParameters(300, 50000, 5, 3, 0.03);
    }
}
