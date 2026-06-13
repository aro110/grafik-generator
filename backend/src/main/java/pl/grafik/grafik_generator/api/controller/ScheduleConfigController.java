package pl.grafik.grafik_generator.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.grafik.grafik_generator.api.common.ApiResponse;
import pl.grafik.grafik_generator.application.dto.ScheduleConfigDto;
import pl.grafik.grafik_generator.application.dto.request.CreateScheduleConfigRequest;
import pl.grafik.grafik_generator.application.dto.request.UpdateScheduleConfigRequest;
import pl.grafik.grafik_generator.application.service.ScheduleConfigService;

import java.util.List;

@RestController
@RequestMapping("/api/schedule-configs")
@RequiredArgsConstructor
public class ScheduleConfigController {

    private final ScheduleConfigService scheduleConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleConfigDto>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(scheduleConfigService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleConfigDto>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleConfigService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleConfigDto>> create(
            @Valid @RequestBody CreateScheduleConfigRequest request
    ) {
        ScheduleConfigDto created = scheduleConfigService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Konfiguracja grafiku została utworzona."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleConfigDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateScheduleConfigRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        scheduleConfigService.update(id, request),
                        "Konfiguracja grafiku została zaktualizowana."
                )
        );
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<ScheduleConfigDto>> publish(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok(scheduleConfigService.publish(id), "Konfiguracja grafiku została opublikowana.")
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        scheduleConfigService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Konfiguracja grafiku została usunięta."));
    }
}
