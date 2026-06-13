package pl.grafik.grafik_generator.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.grafik.grafik_generator.api.common.ApiResponse;
import pl.grafik.grafik_generator.application.dto.ScheduleAiEditDto;
import pl.grafik.grafik_generator.application.dto.ScheduleDetailsDto;
import pl.grafik.grafik_generator.application.dto.ScheduleDto;
import pl.grafik.grafik_generator.application.dto.ScheduleExportDto;
import pl.grafik.grafik_generator.application.dto.request.CreateScheduleAiEditRequest;
import pl.grafik.grafik_generator.application.service.ScheduleAiEditService;
import pl.grafik.grafik_generator.application.service.ScheduleService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleAiEditService scheduleAiEditService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleDto>>> findByRun(
            @RequestParam Long runId) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.findByRunId(runId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleDto>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.findById(id)));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<ApiResponse<ScheduleDetailsDto>> findDetailsById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.findDetailsById(id)));
    }

    @GetMapping("/summary/{groupId}")
    public ResponseEntity<ApiResponse<pl.grafik.grafik_generator.application.dto.StoreLevelSummaryDto>> getStoreSummary(
            @PathVariable Long groupId) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.getStoreSummary(groupId)));
    }

    @GetMapping("/summary/{groupId}/export")
    public ResponseEntity<byte[]> exportGroupExcel(@PathVariable Long groupId) {
        ScheduleExportDto export = scheduleService.exportGroupToExcel(groupId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(export.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.fileName())
                        .build()
                        .toString())
                .body(export.content());
    }

    @PostMapping("/summary/{groupId}/ai-edits")
    public ResponseEntity<ApiResponse<ScheduleAiEditDto>> proposeGroupAiEdit(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateScheduleAiEditRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                scheduleAiEditService.proposeGroup(groupId, request),
                "AI przygotowało propozycję zmian dla całej grupy."));
    }

    @PostMapping("/summary/{groupId}/ai-edits/{editId}/apply")
    public ResponseEntity<ApiResponse<ScheduleAiEditDto>> applyGroupAiEdit(
            @PathVariable Long groupId,
            @PathVariable Long editId) {
        return ResponseEntity.ok(ApiResponse.ok(
                scheduleAiEditService.applyGroup(groupId, editId),
                "Propozycja AI została zapisana jako nowa grupa grafików."));
    }

    @PostMapping("/summary/{groupId}/ai-edits/{editId}/reject")
    public ResponseEntity<ApiResponse<ScheduleAiEditDto>> rejectGroupAiEdit(
            @PathVariable Long groupId,
            @PathVariable Long editId) {
        return ResponseEntity.ok(ApiResponse.ok(
                scheduleAiEditService.rejectGroup(groupId, editId),
                "Propozycja AI została odrzucona."));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportExcel(@PathVariable Long id) {
        ScheduleExportDto export = scheduleService.exportToExcel(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(export.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.fileName())
                        .build()
                        .toString())
                .body(export.content());
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<ScheduleDto>> publish(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok(scheduleService.publish(id), "Grafik został opublikowany."));
    }

    @PostMapping("/{id}/ai-edits")
    public ResponseEntity<ApiResponse<ScheduleAiEditDto>> proposeAiEdit(
            @PathVariable Long id,
            @Valid @RequestBody CreateScheduleAiEditRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                scheduleAiEditService.propose(id, request),
                "AI przygotowało propozycję zmian."));
    }

    @PostMapping("/{id}/ai-edits/{editId}/apply")
    public ResponseEntity<ApiResponse<ScheduleAiEditDto>> applyAiEdit(
            @PathVariable Long id,
            @PathVariable Long editId) {
        return ResponseEntity.ok(ApiResponse.ok(
                scheduleAiEditService.apply(id, editId),
                "Propozycja AI została zapisana jako nowy grafik."));
    }

    @PostMapping("/{id}/ai-edits/{editId}/reject")
    public ResponseEntity<ApiResponse<ScheduleAiEditDto>> rejectAiEdit(
            @PathVariable Long id,
            @PathVariable Long editId) {
        return ResponseEntity.ok(ApiResponse.ok(
                scheduleAiEditService.reject(id, editId),
                "Propozycja AI została odrzucona."));
    }
}
