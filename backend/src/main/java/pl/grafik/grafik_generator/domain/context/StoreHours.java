package pl.grafik.grafik_generator.domain.context;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;

public record StoreHours(Map<DayOfWeek, DayHours> hours) {

    public record DayHours(LocalTime open, LocalTime close) {

        public DayHours {
            if (!close.isAfter(open)) {
                throw new IllegalArgumentException(
                        "Godzina zamknięcia (%s) musi być po otwarciu (%s)".formatted(close, open));
            }
        }

        public int durationHours() {
            return close.getHour() - open.getHour();
        }
    }

    public StoreHours {
        hours = Map.copyOf(hours);
    }

    public DayHours forDay(DayOfWeek day) {
        DayHours dh = hours.get(day);
        if (dh == null) {
            throw new IllegalArgumentException("Brak godzin dla: " + day);
        }
        return dh;
    }

    public int openHour(DayOfWeek day) {
        return forDay(day).open().getHour();
    }

    public int closeHour(DayOfWeek day) {
        return forDay(day).close().getHour();
    }
}
