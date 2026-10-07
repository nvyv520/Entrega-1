package SIGA.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "sections",
    uniqueConstraints = @UniqueConstraint(columnNames = {"subject_id", "academic_period_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor

public class SectionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    private SubjectEntity subject;

    @ManyToOne
    @JoinColumn(name = "academic_period_id", nullable = false)
    private AcademicPeriodEntity academicPeriod;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private TeacherEntity teacher;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Integer enrolledStudents;

    @ElementCollection
    @CollectionTable(name = "section_schedule_blocks", joinColumns = @JoinColumn(name = "section_id"))
    @Column(name = "schedule_block", nullable = false)
    private Set<String> scheduleBlocks = new HashSet<>();
}
