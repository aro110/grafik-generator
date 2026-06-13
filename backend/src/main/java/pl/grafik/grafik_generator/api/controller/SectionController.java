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
import pl.grafik.grafik_generator.application.dto.SectionDto;
import pl.grafik.grafik_generator.application.dto.request.CreateSectionRequest;
import pl.grafik.grafik_generator.application.dto.request.UpdateSectionRequest;
import pl.grafik.grafik_generator.application.service.SectionService;

import java.util.List;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SectionDto>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(sectionService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SectionDto>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(sectionService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SectionDto>> create(@Valid @RequestBody CreateSectionRequest request) {
        SectionDto created = sectionService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Sekcja została utworzona."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SectionDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSectionRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(sectionService.update(id, request), "Sekcja została zaktualizowana.")
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        sectionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Sekcja została usunięta."));
    }
}
