package SIGA.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity 
@Table (name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "section_id"}))
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 

public class EnrollmentEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "section_id", nullable = false)
    private SectionEntity section;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "academic_period_id", nullable = false)
    private AcademicPeriodEntity academicPeriod;

    @Column (nullable = false)
    private LocalDate enrollmentDate;

    @Column (nullable = false)
    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status;
}
