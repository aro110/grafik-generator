package pl.grafik.grafik_generator.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.grafik.grafik_generator.domain.context.CalendarConfig;
import pl.grafik.grafik_generator.domain.context.GaParameters;
import pl.grafik.grafik_generator.domain.context.ShiftRules;
import pl.grafik.grafik_generator.domain.context.StaffingTargets;
import pl.grafik.grafik_generator.domain.context.StoreHours;
import pl.grafik.grafik_generator.domain.context.VacationConfig;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "schedule_configs", indexes = {
        @Index(name = "idx_schedule_configs_status", columnList = "status"),
        @Index(name = "idx_schedule_configs_year_month", columnList = "year_month")
})
@Data
public class ScheduleConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleConfigStatus status;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "year_month", nullable = false)
    private LocalDate yearMonth;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "store_hours", nullable = false, columnDefinition = "jsonb")
    private StoreHours storeHours;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "staffing_targets", nullable = false, columnDefinition = "jsonb")
    private StaffingTargets staffingTargets;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private CalendarConfig calendar;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shift_rules", nullable = false, columnDefinition = "jsonb")
    private ShiftRules shiftRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vacation_config", columnDefinition = "jsonb")
    private VacationConfig vacationConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ga_parameters", nullable = false, columnDefinition = "jsonb")
    private GaParameters gaParameters;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        if (status == null)
            status = ScheduleConfigStatus.DRAFT;
        if (version == null)
            version = 1;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
