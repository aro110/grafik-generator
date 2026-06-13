package pl.grafik.grafik_generator.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.grafik.grafik_generator.api.common.ApiResponse;
import pl.grafik.grafik_generator.application.dto.GenerationRunDto;
import pl.grafik.grafik_generator.application.dto.GenerationRunGroupDto;
import pl.grafik.grafik_generator.application.dto.request.FullGenerationRequest;
import pl.grafik.grafik_generator.application.dto.request.StartGenerationRequest;
import pl.grafik.grafik_generator.application.service.GenerationService;

import java.util.List;

@RestController
@RequestMapping("/api/generation-runs")
@RequiredArgsConstructor
public class GenerationRunController {

    private final GenerationService generationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GenerationRunDto>>> findByConfig(
            @RequestParam Long configId) {
        return ResponseEntity.ok(ApiResponse.ok(generationService.findByConfigId(configId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GenerationRunDto>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(generationService.findById(id)));
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<ApiResponse<GenerationRunGroupDto>> findGroupById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(generationService.findGroupById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GenerationRunGroupDto>> start(
            @Valid @RequestBody StartGenerationRequest request) {
        GenerationRunGroupDto started = generationService.start(request);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(started, "Generowanie grafiku zostało rozpoczęte."));
    }

    @PostMapping("/full-start")
    public ResponseEntity<ApiResponse<GenerationRunGroupDto>> startFull(
            @Valid @RequestBody FullGenerationRequest request) {
        GenerationRunGroupDto started = generationService.startFull(request);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(started, "Generowanie grafiku zostało pomyślnie rozpoczęte (pełny proces)."));
    }

    @PostMapping("/groups/{id}/regenerate")
    public ResponseEntity<ApiResponse<GenerationRunGroupDto>> regenerate(@PathVariable Long id) {
        GenerationRunGroupDto started = generationService.regenerate(id);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(started, "Ponowne generowanie grafiku zostało rozpoczęte."));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable Long id) {
        generationService.deleteGroup(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Grafik i powiązane dane zostały usunięte."));
    }
}
