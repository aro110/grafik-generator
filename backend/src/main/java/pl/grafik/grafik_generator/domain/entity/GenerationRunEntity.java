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
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(name = "generation_runs", indexes = {
        @Index(name = "idx_generation_runs_config_id", columnList = "config_id"),
        @Index(name = "idx_generation_runs_section_id", columnList = "section_id"),
        @Index(name = "idx_generation_runs_group_id", columnList = "group_id"),
        @Index(name = "idx_generation_runs_status", columnList = "status")
})
@Data
@ToString(exclude = {"config", "group"})
public class GenerationRunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "config_id", nullable = false)
    private ScheduleConfigEntity config;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private SectionEntity section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private GenerationRunGroupEntity group;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GenerationRunStatus status;

    @Column(nullable = false)
    private Long seed;

    @Column(nullable = false)
    private Integer progress;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (status == null)
            status = GenerationRunStatus.PENDING;
        if (progress == null)
            progress = 0;
    }
}
