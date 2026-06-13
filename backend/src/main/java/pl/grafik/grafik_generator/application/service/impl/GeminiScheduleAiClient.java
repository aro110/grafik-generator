package pl.grafik.grafik_generator.application.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import pl.grafik.grafik_generator.application.dto.ScheduleAiChangeDto;

import java.util.List;
import java.util.Map;

@Component
public class GeminiScheduleAiClient {

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int maxOutputTokens;

    public GeminiScheduleAiClient(
            Environment environment,
            @Value("${gemini.model:gemini-3.5-flash}") String model,
            @Value("${gemini.max-output-tokens:65536}") int maxOutputTokens) {
        this.objectMapper = new ObjectMapper();
        this.apiKey = firstNonBlank(
                environment.getProperty("gemini.api-key"),
                environment.getProperty("GEMINI_API_KEY"),
                environment.getProperty("GOOGLE_API_KEY"));
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
    }

    public String model() {
        return model;
    }

    public GeminiScheduleEditResponse propose(String prompt) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Brakuje klucza Gemini API. Ustaw GEMINI_API_KEY lub GOOGLE_API_KEY.");
        }

        Client client = Client.builder().apiKey(apiKey).build();
        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .candidateCount(1)
                .maxOutputTokens(maxOutputTokens)
                .responseSchema(responseSchema())
                .build();

        GenerateContentResponse response = client.models.generateContent(model, prompt, config);
        String text = response.text();
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Gemini zwrócił pustą odpowiedź.");
        }

        try {
            GeminiScheduleEditResponse parsed = parseResponse(text);
            return new GeminiScheduleEditResponse(
                    parsed.changes() == null ? List.of() : parsed.changes(),
                    parsed.warnings() == null ? List.of() : parsed.warnings());
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Gemini zwrócił odpowiedź w nieprawidłowym formacie JSON. Fragment odpowiedzi: "
                            + preview(text),
                    ex);
        }
    }

    private GeminiScheduleEditResponse parseResponse(String text) throws Exception {
        try {
            return objectMapper.readValue(text, GeminiScheduleEditResponse.class);
        } catch (Exception ignored) {
            String json = extractJson(text);
            JsonNode node = objectMapper.readTree(json);
            if (node.isArray()) {
                List<ScheduleAiChangeDto> changes = objectMapper.readerForListOf(ScheduleAiChangeDto.class).readValue(node);
                return new GeminiScheduleEditResponse(changes, List.of("Gemini zwrócił listę zmian bez obiektu głównego; backend ją zaakceptował."));
            }
            return objectMapper.treeToValue(node, GeminiScheduleEditResponse.class);
        }
    }

    private String extractJson(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z0-9_-]*\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }

        int objectStart = trimmed.indexOf('{');
        int arrayStart = trimmed.indexOf('[');
        int start;
        char open;
        char close;
        if (objectStart >= 0 && (arrayStart < 0 || objectStart < arrayStart)) {
            start = objectStart;
            open = '{';
            close = '}';
        } else if (arrayStart >= 0) {
            start = arrayStart;
            open = '[';
            close = ']';
        } else {
            return trimmed;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < trimmed.length(); i++) {
            char current = trimmed.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == open) {
                depth++;
            } else if (current == close) {
                depth--;
                if (depth == 0) {
                    return trimmed.substring(start, i + 1);
                }
            }
        }

        return trimmed.substring(start);
    }

    private String preview(String text) {
        String compact = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 500) {
            return compact;
        }
        return compact.substring(0, 500) + "...";
    }

    private Schema responseSchema() {
        Schema stringSchema = Schema.builder().type("string").build();
        Schema integerSchema = Schema.builder().type("integer").build();
        Schema changeSchema = Schema.builder()
                .type("object")
                .properties(Map.of(
                        "employeeId", integerSchema,
                        "day", integerSchema,
                        "shiftLength", integerSchema,
                        "startHour", integerSchema,
                        "scheduleId", integerSchema,
                        "sectionId", integerSchema))
                .required("employeeId", "day", "shiftLength", "startHour")
                .build();

        return Schema.builder()
                .type("object")
                .properties(Map.of(
                        "changes", Schema.builder()
                                .type("array")
                                .items(changeSchema)
                                .build(),
                        "warnings", Schema.builder()
                                .type("array")
                                .items(stringSchema)
                                .build()))
                .required("changes")
                .build();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    public record GeminiScheduleEditResponse(
            List<ScheduleAiChangeDto> changes,
            List<String> warnings) {
    }
}
