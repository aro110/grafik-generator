package pl.grafik.grafik_generator.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.grafik.grafik_generator.application.dto.ScheduleAiChangeDto;
import pl.grafik.grafik_generator.application.dto.ScheduleAiEditDto;
import pl.grafik.grafik_generator.application.dto.ScheduleAiProposedScheduleDto;
import pl.grafik.grafik_generator.application.dto.ScheduleDetailsDto;
import pl.grafik.grafik_generator.application.dto.ScheduleDiffCellDto;
import pl.grafik.grafik_generator.application.dto.request.CreateScheduleAiEditRequest;
import pl.grafik.grafik_generator.application.exception.NotFoundException;
import pl.grafik.grafik_generator.application.mapper.DomainMapper;
import pl.grafik.grafik_generator.application.mapper.ScheduleConfigMapper;
import pl.grafik.grafik_generator.application.service.ScheduleAiEditService;
import pl.grafik.grafik_generator.domain.context.GenerationContext;
import pl.grafik.grafik_generator.domain.context.ProtectedScheduleOverride;
import pl.grafik.grafik_generator.domain.entity.EmployeeEntity;
import pl.grafik.grafik_generator.domain.entity.GenerationRunEntity;
import pl.grafik.grafik_generator.domain.entity.GenerationRunGroupEntity;
import pl.grafik.grafik_generator.domain.entity.GenerationRunGroupStatus;
import pl.grafik.grafik_generator.domain.entity.GenerationRunStatus;
import pl.grafik.grafik_generator.domain.entity.ScheduleAiEditEntity;
import pl.grafik.grafik_generator.domain.entity.ScheduleAiEditStatus;
import pl.grafik.grafik_generator.domain.entity.ScheduleConfigEntity;
import pl.grafik.grafik_generator.domain.entity.ScheduleEntity;
import pl.grafik.grafik_generator.domain.entity.SectionEntity;
import pl.grafik.grafik_generator.domain.model.Section;
import pl.grafik.grafik_generator.domain.scheduleGenerator.Schedule;
import pl.grafik.grafik_generator.infrastructure.repository.ScheduleAiEditRepository;
import pl.grafik.grafik_generator.infrastructure.repository.GenerationRunGroupRepository;
import pl.grafik.grafik_generator.infrastructure.repository.GenerationRunRepository;
import pl.grafik.grafik_generator.infrastructure.repository.ScheduleRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScheduleAiEditServiceImpl implements ScheduleAiEditService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleAiEditRepository editRepository;
    private final GenerationRunGroupRepository groupRepository;
    private final GenerationRunRepository runRepository;
    private final GeminiScheduleAiClient geminiClient;
    private final ScheduleAiPromptBuilder promptBuilder;
    private final ScheduleDetailsAssembler detailsAssembler;
    private final ScheduleConfigMapper configMapper;
    private final DomainMapper domainMapper;

    @Override
    @Transactional(noRollbackFor = RuntimeException.class)
    public ScheduleAiEditDto propose(Long scheduleId, CreateScheduleAiEditRequest request) {
        ScheduleEntity source = requireSchedule(scheduleId);
        boolean allowProtected = Boolean.TRUE.equals(request.allowProtectedDateChanges());

        ScheduleAiEditEntity edit = new ScheduleAiEditEntity();
        edit.setSourceSchedule(source);
        edit.setInstruction(request.instruction().trim());
        edit.setModel(geminiClient.model());
        edit.setAllowProtectedDateChanges(allowProtected);
        edit.setStatus(ScheduleAiEditStatus.FAILED);
        edit = editRepository.save(edit);

        try {
            String prompt = promptBuilder.build(source, edit.getInstruction(), allowProtected);
            GeminiScheduleAiClient.GeminiScheduleEditResponse response = geminiClient.propose(prompt);
            ValidatedProposal proposal = validateAndBuild(source, response.changes(), response.warnings(), allowProtected);

            edit.setStatus(ScheduleAiEditStatus.PROPOSED);
            edit.setChanges(response.changes());
            edit.setWarnings(proposal.warnings());
            edit.setErrors(List.of());
            edit.setDiff(proposal.diff());
            edit.setProposedGenes(proposal.genes());
            edit.setProposedShiftStarts(proposal.shiftStarts());
            edit.setProtectedOverrides(proposal.protectedOverrides());
            edit = editRepository.save(edit);
            return toDto(edit, proposal.toTransientSchedule(source));
        } catch (RuntimeException ex) {
            edit.setStatus(ScheduleAiEditStatus.FAILED);
            edit.setErrors(List.of(ex.getMessage() == null ? "Nie udało się przygotować propozycji AI." : ex.getMessage()));
            editRepository.save(edit);
            throw ex;
        }
    }

    @Override
    @Transactional(noRollbackFor = RuntimeException.class)
    public ScheduleAiEditDto proposeGroup(Long groupId, CreateScheduleAiEditRequest request) {
        List<ScheduleEntity> sources = requireGroupSchedules(groupId);
        boolean allowProtected = Boolean.TRUE.equals(request.allowProtectedDateChanges());

        ScheduleAiEditEntity edit = new ScheduleAiEditEntity();
        edit.setSourceSchedule(sources.get(0));
        edit.setSourceGroupId(groupId);
        edit.setInstruction(request.instruction().trim());
        edit.setModel(geminiClient.model());
        edit.setAllowProtectedDateChanges(allowProtected);
        edit.setStatus(ScheduleAiEditStatus.FAILED);
        edit = editRepository.save(edit);

        try {
            String prompt = promptBuilder.buildGroup(sources, edit.getInstruction(), allowProtected);
            GeminiScheduleAiClient.GeminiScheduleEditResponse response = geminiClient.propose(prompt);
            ValidatedGroupProposal proposal = validateAndBuildGroup(sources, response.changes(), response.warnings(), allowProtected);

            edit.setStatus(ScheduleAiEditStatus.PROPOSED);
            edit.setChanges(response.changes());
            edit.setWarnings(proposal.warnings());
            edit.setErrors(List.of());
            edit.setDiff(proposal.diff());
            edit.setProposedSchedules(proposal.proposedSchedules());
            edit = editRepository.save(edit);
            return toDto(edit, proposal.transientSchedules());
        } catch (RuntimeException ex) {
            edit.setStatus(ScheduleAiEditStatus.FAILED);
            edit.setErrors(List.of(ex.getMessage() == null ? "Nie udało się przygotować propozycji AI dla grupy." : ex.getMessage()));
            editRepository.save(edit);
            throw ex;
        }
    }

    @Override
    @Transactional
    public ScheduleAiEditDto apply(Long scheduleId, Long editId) {
        ScheduleAiEditEntity edit = requireEdit(editId);
        assertEditBelongsToSchedule(edit, scheduleId);
        if (edit.getStatus() != ScheduleAiEditStatus.PROPOSED) {
            throw new IllegalStateException("Można zaakceptować tylko aktywną propozycję AI.");
        }
        if (edit.getProposedGenes() == null || edit.getProposedShiftStarts() == null) {
            throw new IllegalStateException("Propozycja AI nie zawiera kompletnego grafiku.");
        }

        ScheduleEntity source = edit.getSourceSchedule();
        ScheduleEntity accepted = new ScheduleEntity();
        accepted.setRun(source.getRun());
        accepted.setGenes(deepCopy(edit.getProposedGenes()));
        accepted.setShiftStarts(deepCopy(edit.getProposedShiftStarts()));
        accepted.setProtectedOverrides(edit.getProtectedOverrides() == null ? List.of() : List.copyOf(edit.getProtectedOverrides()));
        accepted.setPublished(false);
        accepted.setFitness(calculateFitness(source, accepted.getGenes()));
        accepted = scheduleRepository.save(accepted);

        edit.setAcceptedSchedule(accepted);
        edit.setStatus(ScheduleAiEditStatus.APPLIED);
        edit.setAppliedAt(Instant.now());
        edit = editRepository.save(edit);
        return toDto(edit, (ScheduleEntity) null);
    }

    @Override
    @Transactional
    public ScheduleAiEditDto applyGroup(Long groupId, Long editId) {
        ScheduleAiEditEntity edit = requireEdit(editId);
        assertEditBelongsToGroup(edit, groupId);
        if (edit.getStatus() != ScheduleAiEditStatus.PROPOSED) {
            throw new IllegalStateException("Można zaakceptować tylko aktywną propozycję AI.");
        }
        if (edit.getProposedSchedules() == null || edit.getProposedSchedules().isEmpty()) {
            throw new IllegalStateException("Propozycja AI nie zawiera kompletnych grafików dla grupy.");
        }

        List<ScheduleEntity> sources = requireGroupSchedules(groupId);
        Map<Long, ScheduleAiProposedScheduleDto> proposalsByScheduleId = new HashMap<>();
        for (ScheduleAiProposedScheduleDto proposed : edit.getProposedSchedules()) {
            proposalsByScheduleId.put(proposed.sourceScheduleId(), proposed);
        }

        GenerationRunGroupEntity sourceGroup = sources.get(0).getRun().getGroup();
        GenerationRunGroupEntity acceptedGroup = new GenerationRunGroupEntity();
        acceptedGroup.setConfig(sourceGroup.getConfig());
        acceptedGroup.setSeed(sourceGroup.getSeed());
        acceptedGroup.setStatus(GenerationRunGroupStatus.SUCCESS);
        acceptedGroup.setFinishedAt(Instant.now());
        acceptedGroup = groupRepository.save(acceptedGroup);

        List<Long> acceptedScheduleIds = new ArrayList<>();
        for (ScheduleEntity source : sources) {
            ScheduleAiProposedScheduleDto proposed = proposalsByScheduleId.get(source.getId());
            if (proposed == null) {
                throw new IllegalStateException("Propozycja AI nie zawiera grafiku dla sekcji " + source.getRun().getSection().getName() + ".");
            }

            GenerationRunEntity acceptedRun = new GenerationRunEntity();
            acceptedRun.setGroup(acceptedGroup);
            acceptedRun.setConfig(source.getRun().getConfig());
            acceptedRun.setSection(source.getRun().getSection());
            acceptedRun.setSeed(source.getRun().getSeed());
            acceptedRun.setStatus(GenerationRunStatus.SUCCESS);
            acceptedRun.setProgress(100);
            acceptedRun.setStartedAt(Instant.now());
            acceptedRun.setFinishedAt(Instant.now());
            acceptedRun = runRepository.save(acceptedRun);

            ScheduleEntity accepted = new ScheduleEntity();
            accepted.setRun(acceptedRun);
            accepted.setGenes(deepCopy(proposed.genes()));
            accepted.setShiftStarts(deepCopy(proposed.shiftStarts()));
            accepted.setProtectedOverrides(proposed.protectedOverrides() == null ? List.of() : List.copyOf(proposed.protectedOverrides()));
            accepted.setPublished(false);
            accepted.setFitness(calculateFitness(source, accepted.getGenes()));
            accepted = scheduleRepository.save(accepted);
            acceptedScheduleIds.add(accepted.getId());
        }

        edit.setAcceptedGroupId(acceptedGroup.getId());
        edit.setAcceptedScheduleIds(acceptedScheduleIds);
        edit.setStatus(ScheduleAiEditStatus.APPLIED);
        edit.setAppliedAt(Instant.now());
        edit = editRepository.save(edit);
        return toDto(edit, (List<ScheduleEntity>) null);
    }

    @Override
    @Transactional
    public ScheduleAiEditDto reject(Long scheduleId, Long editId) {
        ScheduleAiEditEntity edit = requireEdit(editId);
        assertEditBelongsToSchedule(edit, scheduleId);
        if (edit.getStatus() == ScheduleAiEditStatus.APPLIED) {
            throw new IllegalStateException("Zaakceptowanej propozycji AI nie można odrzucić.");
        }
        edit.setStatus(ScheduleAiEditStatus.REJECTED);
        edit = editRepository.save(edit);
        return toDto(edit, (ScheduleEntity) null);
    }

    @Override
    @Transactional
    public ScheduleAiEditDto rejectGroup(Long groupId, Long editId) {
        ScheduleAiEditEntity edit = requireEdit(editId);
        assertEditBelongsToGroup(edit, groupId);
        if (edit.getStatus() == ScheduleAiEditStatus.APPLIED) {
            throw new IllegalStateException("Zaakceptowanej propozycji AI nie można odrzucić.");
        }
        edit.setStatus(ScheduleAiEditStatus.REJECTED);
        edit = editRepository.save(edit);
        return toDto(edit, (List<ScheduleEntity>) null);
    }

    private ValidatedProposal validateAndBuild(ScheduleEntity source, List<ScheduleAiChangeDto> changes,
            List<String> warnings, boolean allowProtected) {
        List<EmployeeEntity> employees = employees(source);
        ScheduleConfigEntity config = source.getRun().getConfig();
        int days = config.getCalendar().daysInMonth();
        Map<Long, Integer> employeeIndexById = new HashMap<>();
        for (int i = 0; i < employees.size(); i++) {
            employeeIndexById.put(employees.get(i).getId(), i);
        }

        List<List<Integer>> genes = deepCopy(source.getGenes());
        List<List<Integer>> starts = deepCopy(source.getShiftStarts());
        List<String> errors = new ArrayList<>();
        List<ProtectedScheduleOverride> protectedOverrides = new ArrayList<>();
        Set<String> overrideKeys = new HashSet<>();

        if (changes == null || changes.isEmpty()) {
            errors.add("Gemini nie zaproponował żadnej zmiany.");
        } else {
            for (ScheduleAiChangeDto change : changes) {
                validateAndApplyChange(change, source, config, employees, employeeIndexById, genes, starts,
                        protectedOverrides, overrideKeys, allowProtected, errors);
            }
        }

        validateMatrixShape(genes, starts, employees.size(), days, errors);
        validateWholeSchedule(source, config, employees, genes, starts, allowProtected, errors);

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", errors));
        }

        ScheduleEntity proposed = transientSchedule(source, genes, starts, protectedOverrides);
        List<ScheduleDiffCellDto> diff = buildDiff(source, proposed);
        return new ValidatedProposal(genes, starts, protectedOverrides, diff,
                warnings == null ? List.of() : warnings);
    }

    private ValidatedGroupProposal validateAndBuildGroup(List<ScheduleEntity> sources, List<ScheduleAiChangeDto> changes,
            List<String> warnings, boolean allowProtected) {
        Map<Long, ProposalState> statesByScheduleId = new LinkedHashMap<>();
        Map<Long, ProposalState> statesBySectionId = new HashMap<>();
        Map<Long, ProposalState> statesByEmployeeId = new HashMap<>();
        List<String> errors = new ArrayList<>();

        for (ScheduleEntity source : sources) {
            List<EmployeeEntity> employees = employees(source);
            Map<Long, Integer> employeeIndexById = new HashMap<>();
            for (int i = 0; i < employees.size(); i++) {
                employeeIndexById.put(employees.get(i).getId(), i);
            }
            ProposalState state = new ProposalState(
                    source,
                    employees,
                    employeeIndexById,
                    deepCopy(source.getGenes()),
                    deepCopy(source.getShiftStarts()),
                    new ArrayList<>(),
                    new HashSet<>());
            statesByScheduleId.put(source.getId(), state);
            statesBySectionId.put(source.getRun().getSection().getId(), state);
            for (EmployeeEntity employee : employees) {
                statesByEmployeeId.put(employee.getId(), state);
            }
        }

        if (changes == null || changes.isEmpty()) {
            errors.add("Gemini nie zaproponował żadnej zmiany.");
        } else {
            for (ScheduleAiChangeDto change : changes) {
                ProposalState state = resolveState(change, statesByScheduleId, statesBySectionId, statesByEmployeeId, errors);
                if (state == null) {
                    continue;
                }
                validateAndApplyChange(change, state.source(), state.source().getRun().getConfig(),
                        state.employees(), state.employeeIndexById(), state.genes(), state.starts(),
                        state.protectedOverrides(), state.overrideKeys(), allowProtected, errors);
            }
        }

        List<ScheduleDiffCellDto> diff = new ArrayList<>();
        List<ScheduleAiProposedScheduleDto> proposedSchedules = new ArrayList<>();
        List<ScheduleEntity> transientSchedules = new ArrayList<>();
        for (ProposalState state : statesByScheduleId.values()) {
            ScheduleConfigEntity config = state.source().getRun().getConfig();
            validateMatrixShape(state.genes(), state.starts(), state.employees().size(), config.getCalendar().daysInMonth(), errors);
            validateWholeSchedule(state.source(), config, state.employees(), state.genes(), state.starts(), allowProtected, errors);
            ScheduleEntity proposed = transientSchedule(state.source(), state.genes(), state.starts(), state.protectedOverrides());
            diff.addAll(buildDiff(state.source(), proposed));
            proposedSchedules.add(new ScheduleAiProposedScheduleDto(
                    state.source().getId(),
                    state.genes(),
                    state.starts(),
                    state.protectedOverrides()));
            transientSchedules.add(proposed);
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", errors));
        }

        return new ValidatedGroupProposal(proposedSchedules, transientSchedules, diff,
                warnings == null ? List.of() : warnings);
    }

    private ProposalState resolveState(ScheduleAiChangeDto change,
            Map<Long, ProposalState> statesByScheduleId,
            Map<Long, ProposalState> statesBySectionId,
            Map<Long, ProposalState> statesByEmployeeId,
            List<String> errors) {
        if (change == null) {
            errors.add("Zmiana AI jest pusta.");
            return null;
        }
        if (change.scheduleId() != null) {
            ProposalState state = statesByScheduleId.get(change.scheduleId());
            if (state == null) {
                errors.add("Nieznany scheduleId=" + change.scheduleId() + ".");
            }
            return state;
        }
        if (change.sectionId() != null) {
            ProposalState state = statesBySectionId.get(change.sectionId());
            if (state == null) {
                errors.add("Nieznany sectionId=" + change.sectionId() + ".");
            }
            return state;
        }
        if (change.employeeId() != null) {
            ProposalState state = statesByEmployeeId.get(change.employeeId());
            if (state == null) {
                errors.add("Nieznany pracownik employeeId=" + change.employeeId() + ".");
            }
            return state;
        }
        errors.add("Każda zmiana grupowa musi zawierać scheduleId, sectionId lub employeeId.");
        return null;
    }

    private void validateAndApplyChange(ScheduleAiChangeDto change,
            ScheduleEntity source,
            ScheduleConfigEntity config,
            List<EmployeeEntity> employees,
            Map<Long, Integer> employeeIndexById,
            List<List<Integer>> genes,
            List<List<Integer>> starts,
            List<ProtectedScheduleOverride> protectedOverrides,
            Set<String> overrideKeys,
            boolean allowProtected,
            List<String> errors) {
        if (change == null || change.employeeId() == null || change.day() == null) {
            errors.add("Każda zmiana musi zawierać employeeId i day.");
            return;
        }
        Integer employeeIndex = employeeIndexById.get(change.employeeId());
        if (employeeIndex == null) {
            errors.add("Nieznany pracownik employeeId=" + change.employeeId() + ".");
            return;
        }
        int day = change.day();
        if (day < 1 || day > config.getCalendar().daysInMonth()) {
            errors.add("Nieprawidłowy dzień " + day + " dla employeeId=" + change.employeeId() + ".");
            return;
        }

        int dayIndex = day - 1;
        int length = change.shiftLength() == null ? 0 : change.shiftLength();
        int start = change.startHour() == null ? 0 : change.startHour();
        EmployeeEntity employee = employees.get(employeeIndex);

        if (length <= 0) {
            genes.get(employeeIndex).set(dayIndex, 0);
            starts.get(employeeIndex).set(dayIndex, 0);
            return;
        }

        if (config.getCalendar().isClosedDay(dayIndex)) {
            errors.add("Nie można zaplanować pracy w dzień zamknięty " + day + ".");
        }
        if (!config.getShiftRules().shiftLengths().contains(length)) {
            errors.add("Niedozwolona długość zmiany " + length + "h dla employeeId=" + employee.getId() + ", dzień " + day + ".");
        }
        if (isProtected(employee, dayIndex) && !allowProtected) {
            errors.add("Zmiana narusza urlop lub ręcznie wybrane wolne pracownika " + fullName(employee) + " w dniu " + day + ".");
        }

        int open = config.getStoreHours().openHour(config.getCalendar().dayOfWeekAt(dayIndex));
        int close = config.getStoreHours().closeHour(config.getCalendar().dayOfWeekAt(dayIndex));
        if (start < open || start + length > close) {
            errors.add("Zmiana " + start + ":00-" + (start + length) + ":00 wykracza poza godziny sklepu w dniu " + day + ".");
        }

        genes.get(employeeIndex).set(dayIndex, length);
        starts.get(employeeIndex).set(dayIndex, start);

        if (isProtected(employee, dayIndex) && allowProtected) {
            String key = employee.getId() + ":" + day;
            if (overrideKeys.add(key)) {
                protectedOverrides.add(new ProtectedScheduleOverride(employee.getId(), day, ProtectedScheduleOverride.ALLOW_WORK));
            }
        }
    }

    private void validateWholeSchedule(ScheduleEntity source,
            ScheduleConfigEntity config,
            List<EmployeeEntity> employees,
            List<List<Integer>> genes,
            List<List<Integer>> starts,
            boolean allowProtected,
            List<String> errors) {
        int days = config.getCalendar().daysInMonth();

        for (int employeeIndex = 0; employeeIndex < employees.size(); employeeIndex++) {
            EmployeeEntity employee = employees.get(employeeIndex);
            List<Integer> sourceRow = source.getGenes().get(employeeIndex);
            List<Integer> proposedRow = genes.get(employeeIndex);
            if (sum(sourceRow) != sum(proposedRow)) {
                errors.add(fullName(employee) + ": propozycja zmienia łączną liczbę godzin pracy.");
            }
            if (countWorkingDays(sourceRow) != countWorkingDays(proposedRow)) {
                errors.add(fullName(employee) + ": propozycja zmienia łączną liczbę dni pracy.");
            }

            int consecutive = 0;
            for (int dayIndex = 0; dayIndex < days; dayIndex++) {
                int length = safe(proposedRow.get(dayIndex));
                if (length > 0) {
                    consecutive++;
                } else {
                    consecutive = 0;
                }
                int aiMaxWorkingDaysInARow = config.getShiftRules().maxWorkingDaysInARow() + 1;
                if (consecutive > aiMaxWorkingDaysInARow) {
                    int sourceMax = maxConsecutive(sourceRow);
                    int proposedMax = maxConsecutive(proposedRow);
                    if (proposedMax > sourceMax) {
                        errors.add(sectionLabel(source) + fullName(employee)
                                + ": propozycja przekracza limit dni pracy z rzędu dla edycji AI.");
                    }
                    break;
                }
                if (config.getCalendar().isClosedDay(dayIndex) && length > 0) {
                    int sourceLength = safe(sourceRow.get(dayIndex));
                    if (sourceLength <= 0) {
                        errors.add(sectionLabel(source) + fullName(employee)
                                + ": propozycja dodaje pracę w dzień zamknięty " + (dayIndex + 1) + ".");
                    }
                }
                if (!allowProtected && isProtected(employee, dayIndex) && length > 0) {
                    int sourceLength = safe(sourceRow.get(dayIndex));
                    if (sourceLength <= 0) {
                        errors.add(sectionLabel(source) + fullName(employee)
                                + ": propozycja dodaje pracę w chroniony dzień " + (dayIndex + 1) + ".");
                    }
                }
            }
        }

        // AI edits intentionally allow any number of employees to start at the same hour.
    }

    private List<ScheduleDiffCellDto> buildDiff(ScheduleEntity source, ScheduleEntity proposed) {
        ScheduleDetailsDto before = detailsAssembler.build(source);
        ScheduleDetailsDto after = detailsAssembler.build(proposed);
        Map<Long, ScheduleDetailsDto.EmployeeScheduleRowDto> afterByEmployee = new LinkedHashMap<>();
        for (ScheduleDetailsDto.EmployeeScheduleRowDto row : after.employees()) {
            afterByEmployee.put(row.employeeId(), row);
        }

        List<ScheduleDiffCellDto> diff = new ArrayList<>();
        for (ScheduleDetailsDto.EmployeeScheduleRowDto beforeRow : before.employees()) {
            ScheduleDetailsDto.EmployeeScheduleRowDto afterRow = afterByEmployee.get(beforeRow.employeeId());
            if (afterRow == null) {
                continue;
            }
            for (int i = 0; i < beforeRow.shifts().size(); i++) {
                String beforeValue = beforeRow.shifts().get(i);
                String afterValue = afterRow.shifts().get(i);
                if (!beforeValue.equals(afterValue)) {
                    EmployeeEntity employee = employees(source).stream()
                            .filter(e -> e.getId().equals(beforeRow.employeeId()))
                            .findFirst()
                            .orElse(null);
                    diff.add(new ScheduleDiffCellDto(
                            beforeRow.employeeId(),
                            i + 1,
                            beforeRow.name() + " " + beforeRow.surname(),
                            beforeValue,
                            afterValue,
                            "Zmiana zaproponowana przez AI",
                            employee != null && isProtected(employee, i)));
                }
            }
        }
        return diff;
    }

    private ScheduleAiEditDto toDto(ScheduleAiEditEntity edit, ScheduleEntity proposedOverride) {
        List<ScheduleEntity> proposedOverrides = proposedOverride == null ? List.of() : List.of(proposedOverride);
        return toDto(edit, proposedOverrides);
    }

    private ScheduleAiEditDto toDto(ScheduleAiEditEntity edit, List<ScheduleEntity> proposedOverrides) {
        List<ScheduleDetailsDto> proposedSchedules = proposedOverrides == null || edit.getStatus() != ScheduleAiEditStatus.PROPOSED
                ? List.of()
                : proposedOverrides.stream()
                        .map(detailsAssembler::build)
                        .toList();
        ScheduleDetailsDto proposed = proposedSchedules.isEmpty() ? null : proposedSchedules.get(0);
        ScheduleDetailsDto accepted = edit.getAcceptedSchedule() == null
                ? null
                : detailsAssembler.build(edit.getAcceptedSchedule());
        List<ScheduleDetailsDto> acceptedSchedules = edit.getAcceptedScheduleIds() == null || edit.getAcceptedScheduleIds().isEmpty()
                ? List.of()
                : scheduleRepository.findAllById(edit.getAcceptedScheduleIds()).stream()
                        .map(detailsAssembler::build)
                        .toList();

        return new ScheduleAiEditDto(
                edit.getId(),
                edit.getSourceSchedule().getId(),
                edit.getSourceGroupId(),
                edit.getAcceptedSchedule() == null ? null : edit.getAcceptedSchedule().getId(),
                edit.getAcceptedGroupId(),
                edit.getAcceptedScheduleIds(),
                edit.getStatus(),
                edit.getInstruction(),
                edit.getModel(),
                edit.getAllowProtectedDateChanges(),
                edit.getChanges(),
                edit.getDiff(),
                edit.getWarnings(),
                edit.getErrors(),
                proposed,
                proposedSchedules,
                accepted,
                acceptedSchedules,
                edit.getCreatedAt(),
                edit.getUpdatedAt());
    }

    private void validateMatrixShape(List<List<Integer>> genes, List<List<Integer>> starts, int employees, int days,
            List<String> errors) {
        if (genes == null || starts == null || genes.size() != employees || starts.size() != employees) {
            errors.add("Propozycja ma nieprawidłową liczbę wierszy grafiku.");
            return;
        }
        for (int i = 0; i < employees; i++) {
            if (genes.get(i).size() != days || starts.get(i).size() != days) {
                errors.add("Propozycja ma nieprawidłową liczbę dni w wierszu pracownika.");
            }
        }
    }

    private double calculateFitness(ScheduleEntity source, List<List<Integer>> genes) {
        ScheduleConfigEntity config = source.getRun().getConfig();
        SectionEntity sectionEntity = source.getRun().getSection();
        GenerationContext ctx = configMapper.toContext(config);
        Section section = domainMapper.toDomainSection(sectionEntity, ctx.shiftRules(), ctx.calendar(), ctx.vacationConfig());
        return new Schedule(section, toArray(genes), ctx).getFitness();
    }

    private int[][] toArray(List<List<Integer>> genes) {
        int[][] result = new int[genes.size()][genes.get(0).size()];
        for (int i = 0; i < genes.size(); i++) {
            for (int d = 0; d < genes.get(i).size(); d++) {
                result[i][d] = safe(genes.get(i).get(d));
            }
        }
        return result;
    }

    private ScheduleEntity transientSchedule(ScheduleEntity source, List<List<Integer>> genes, List<List<Integer>> starts,
            List<ProtectedScheduleOverride> protectedOverrides) {
        ScheduleEntity proposed = new ScheduleEntity();
        proposed.setId(source.getId());
        proposed.setRun(source.getRun());
        proposed.setFitness(source.getFitness());
        proposed.setGenes(deepCopy(genes));
        proposed.setShiftStarts(deepCopy(starts));
        proposed.setProtectedOverrides(protectedOverrides == null ? List.of() : List.copyOf(protectedOverrides));
        proposed.setPublished(false);
        return proposed;
    }

    private ScheduleEntity requireSchedule(Long id) {
        return scheduleRepository.findDetailedById(id)
                .orElseThrow(() -> NotFoundException.of("Grafik", id));
    }

    private List<ScheduleEntity> requireGroupSchedules(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw NotFoundException.of("Grupa generacji", groupId);
        }
        List<ScheduleEntity> schedules = scheduleRepository.findByRunGroupId(groupId).stream()
                .sorted(Comparator
                        .comparing((ScheduleEntity schedule) -> schedule.getRun().getSection().getName())
                        .thenComparing(ScheduleEntity::getId))
                .toList();
        if (schedules.isEmpty()) {
            throw NotFoundException.of("Grafiki grupy generacji", groupId);
        }
        return schedules;
    }

    private ScheduleAiEditEntity requireEdit(Long id) {
        return editRepository.findDetailedById(id)
                .orElseThrow(() -> NotFoundException.of("Propozycja AI", id));
    }

    private void assertEditBelongsToSchedule(ScheduleAiEditEntity edit, Long scheduleId) {
        if (!edit.getSourceSchedule().getId().equals(scheduleId)) {
            throw new IllegalArgumentException("Propozycja AI nie należy do wskazanego grafiku.");
        }
    }

    private void assertEditBelongsToGroup(ScheduleAiEditEntity edit, Long groupId) {
        if (edit.getSourceGroupId() == null || !edit.getSourceGroupId().equals(groupId)) {
            throw new IllegalArgumentException("Propozycja AI nie należy do wskazanej grupy grafików.");
        }
    }

    private List<EmployeeEntity> employees(ScheduleEntity schedule) {
        return schedule.getRun().getSection().getEmployees().stream()
                .sorted(Comparator.comparing(EmployeeEntity::getId))
                .toList();
    }

    private boolean isProtected(EmployeeEntity employee, int dayIndex) {
        return (employee.getVacations() != null && employee.getVacations().contains(dayIndex))
                || (employee.getDaysOff() != null && employee.getDaysOff().contains(dayIndex));
    }

    private int sum(List<Integer> row) {
        return row.stream().mapToInt(this::safe).sum();
    }

    private int countWorkingDays(List<Integer> row) {
        return (int) row.stream().filter(value -> safe(value) > 0).count();
    }

    private int maxConsecutive(List<Integer> row) {
        int max = 0;
        int current = 0;
        for (Integer value : row) {
            if (safe(value) > 0) {
                current++;
                max = Math.max(max, current);
            } else {
                current = 0;
            }
        }
        return max;
    }

    private int countStarts(List<List<Integer>> genes, List<List<Integer>> starts, int dayIndex, int startHour) {
        int count = 0;
        for (int employeeIndex = 0; employeeIndex < genes.size(); employeeIndex++) {
            if (safe(genes.get(employeeIndex).get(dayIndex)) > 0
                    && safe(starts.get(employeeIndex).get(dayIndex)) == startHour) {
                count++;
            }
        }
        return count;
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private String fullName(EmployeeEntity employee) {
        return (employee.getName() + " " + employee.getSurname()).trim();
    }

    private String sectionLabel(ScheduleEntity source) {
        return "[" + source.getRun().getSection().getName() + "] ";
    }

    private List<List<Integer>> deepCopy(List<List<Integer>> source) {
        return source.stream()
                .map(ArrayList::new)
                .map(row -> (List<Integer>) row)
                .toList();
    }

    private record ValidatedProposal(
            List<List<Integer>> genes,
            List<List<Integer>> shiftStarts,
            List<ProtectedScheduleOverride> protectedOverrides,
            List<ScheduleDiffCellDto> diff,
            List<String> warnings) {

        private ScheduleEntity toTransientSchedule(ScheduleEntity source) {
            ScheduleEntity proposed = new ScheduleEntity();
            proposed.setId(source.getId());
            proposed.setRun(source.getRun());
            proposed.setFitness(source.getFitness());
            proposed.setGenes(genes);
            proposed.setShiftStarts(shiftStarts);
            proposed.setProtectedOverrides(protectedOverrides);
            proposed.setPublished(false);
            return proposed;
        }
    }

    private record ProposalState(
            ScheduleEntity source,
            List<EmployeeEntity> employees,
            Map<Long, Integer> employeeIndexById,
            List<List<Integer>> genes,
            List<List<Integer>> starts,
            List<ProtectedScheduleOverride> protectedOverrides,
            Set<String> overrideKeys) {
    }

    private record ValidatedGroupProposal(
            List<ScheduleAiProposedScheduleDto> proposedSchedules,
            List<ScheduleEntity> transientSchedules,
            List<ScheduleDiffCellDto> diff,
            List<String> warnings) {
    }
}
