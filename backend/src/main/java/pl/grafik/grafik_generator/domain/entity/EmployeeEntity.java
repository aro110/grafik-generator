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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees", indexes = @Index(name = "idx_employees_section_id", columnList = "section_id"))
@Data
@ToString(exclude = "section")
public class EmployeeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String surname;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private SectionEntity section;

    @Column(name = "total_hours", nullable = false)
    private Integer totalHours;

    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "days_off", nullable = false, columnDefinition = "jsonb")
    private List<Integer> daysOff = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vacations", nullable = false, columnDefinition = "jsonb")
    private List<Integer> vacations = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
