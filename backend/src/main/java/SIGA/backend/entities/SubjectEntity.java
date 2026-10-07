package SIGA.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "subjects",
    uniqueConstraints = @UniqueConstraint(columnNames = {"study_plan_id", "code"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor

public class SubjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer semester;

    @Column(nullable = false)
    private Integer theoryHours;

    @Column(nullable = false)
    private Integer exerciseHours;

    @Column(nullable = false)
    private Integer laboratoryHours;

    @Column(nullable = false)
    private Integer sct;

    @ManyToOne
    @JoinColumn(name = "study_plan_id", nullable = false)
    private StudyPlanEntity studyPlan;

    @ManyToMany
    @JoinTable(
        name = "subject_prerequisites",
        joinColumns = @JoinColumn(name = "subject_id"),
        inverseJoinColumns = @JoinColumn(name = "prerequisite_id")
    )
    private Set<SubjectEntity> prerequisites = new HashSet<>();
}
