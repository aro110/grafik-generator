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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "generation_run_groups", indexes = {
        @Index(name = "idx_run_groups_config_id", columnList = "config_id")
})
@Data
@ToString(exclude = {"config", "runs"})
public class GenerationRunGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "config_id", nullable = false)
    private ScheduleConfigEntity config;

    @Column(nullable = false)
    private Long seed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GenerationRunGroupStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<GenerationRunEntity> runs = new ArrayList<>();

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (status == null)
            status = GenerationRunGroupStatus.PENDING;
    }
}
