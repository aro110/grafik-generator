package pl.grafik.grafik_generator.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.grafik.grafik_generator.application.dto.ScheduleAiChangeDto;
import pl.grafik.grafik_generator.application.dto.ScheduleDiffCellDto;
import pl.grafik.grafik_generator.application.dto.ScheduleAiProposedScheduleDto;
import pl.grafik.grafik_generator.domain.context.ProtectedScheduleOverride;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "schedule_ai_edits", indexes = {
        @Index(name = "idx_schedule_ai_edits_source_schedule_id", columnList = "source_schedule_id"),
        @Index(name = "idx_schedule_ai_edits_status", columnList = "status")
})
@Data
@ToString(exclude = { "sourceSchedule", "acceptedSchedule" })
public class ScheduleAiEditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_schedule_id", nullable = false)
    private ScheduleEntity sourceSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_schedule_id")
    private ScheduleEntity acceptedSchedule;

    @Column(name = "source_group_id")
    private Long sourceGroupId;

    @Column(name = "accepted_group_id")
    private Long acceptedGroupId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "accepted_schedule_ids", nullable = false, columnDefinition = "jsonb")
    private List<Long> acceptedScheduleIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleAiEditStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String instruction;

    @Column(nullable = false, length = 120)
    private String model;

    @Column(name = "allow_protected_date_changes", nullable = false)
    private Boolean allowProtectedDateChanges;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<ScheduleAiChangeDto> changes = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<ScheduleDiffCellDto> diff = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> warnings = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> errors = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposed_genes", columnDefinition = "jsonb")
    private List<List<Integer>> proposedGenes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposed_shift_starts", columnDefinition = "jsonb")
    private List<List<Integer>> proposedShiftStarts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "protected_overrides", columnDefinition = "jsonb")
    private List<ProtectedScheduleOverride> protectedOverrides = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposed_schedules", columnDefinition = "jsonb")
    private List<ScheduleAiProposedScheduleDto> proposedSchedules = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        if (status == null) status = ScheduleAiEditStatus.PROPOSED;
        if (allowProtectedDateChanges == null) allowProtectedDateChanges = false;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
