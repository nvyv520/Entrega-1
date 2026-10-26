package SIGA.backend.entities;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity 
@Table (name = "sections", uniqueConstraints = @UniqueConstraint(columnNames = {"subject_id", "academic_period_id"}))
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 

public class SectionEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn (name = "subject_id", nullable = false)
    private SubjectEntity subject;

    @ManyToOne 
    @JoinColumn (name = "academic_period_id", nullable = false)
    private AcademicPeriodEntity academicPeriod;

    @ManyToOne
    @JoinColumn (name = "teacher_id", nullable = false)
    private TeacherEntity teacher;

    @Column (nullable = false)
    private Integer capacity;

    @Column (nullable = false)
    private Integer enrolledStudents;

    @Column (nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @ElementCollection
    @CollectionTable (name = "section_blocks", joinColumns = @JoinColumn (name = "section_id"))
    @Column (name = "blocks", nullable = false, length = 2)
    private Set<String> blocks = new HashSet<>();
}
