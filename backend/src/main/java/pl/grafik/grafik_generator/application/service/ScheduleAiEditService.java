package pl.grafik.grafik_generator.application.service;

import pl.grafik.grafik_generator.application.dto.ScheduleAiEditDto;
import pl.grafik.grafik_generator.application.dto.request.CreateScheduleAiEditRequest;

public interface ScheduleAiEditService {

    ScheduleAiEditDto propose(Long scheduleId, CreateScheduleAiEditRequest request);

    ScheduleAiEditDto proposeGroup(Long groupId, CreateScheduleAiEditRequest request);

    ScheduleAiEditDto apply(Long scheduleId, Long editId);

    ScheduleAiEditDto applyGroup(Long groupId, Long editId);

    ScheduleAiEditDto reject(Long scheduleId, Long editId);

    ScheduleAiEditDto rejectGroup(Long groupId, Long editId);
}
