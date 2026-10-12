package SIGA.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity 
@Table (name = "enrollments")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 

public class EnrollmentEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn (name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne
    @JoinColumn (name = "section_id", nullable = false)
    private SectionEntity section;

    @ManyToOne
    @JoinColumn (name = "academic_period_id", nullable = false)
    private AcademicPeriodEntity academicPeriod;

    @Column (nullable = false)
    private LocalDate enrollmentDate;

    @Column (nullable = false)
    private String status;
}
