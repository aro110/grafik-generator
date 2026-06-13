package pl.grafik.grafik_generator.application.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String entity, Long id) {
        return new NotFoundException("%s o id=%d nie istnieje.".formatted(entity, id));
    }
}
