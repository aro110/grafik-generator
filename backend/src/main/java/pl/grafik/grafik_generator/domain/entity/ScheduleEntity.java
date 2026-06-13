package pl.grafik.grafik_generator.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.grafik.grafik_generator.domain.context.ProtectedScheduleOverride;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "schedules", indexes = {
        @Index(name = "idx_schedules_run_id", columnList = "run_id"),
        @Index(name = "idx_schedules_published", columnList = "published")
})
@Data
@ToString(exclude = "run")
public class ScheduleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private GenerationRunEntity run;

    @Column(nullable = false)
    private Double fitness;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<List<Integer>> genes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shift_starts", columnDefinition = "jsonb")
    private List<List<Integer>> shiftStarts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "protected_overrides", columnDefinition = "jsonb")
    private List<ProtectedScheduleOverride> protectedOverrides;

    @Column(nullable = false)
    private Boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (published == null) published = false;
    }
}
