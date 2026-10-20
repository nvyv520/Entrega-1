package SIGA.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "period_matriculations", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "academic_period_id"}))
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 

public class PeriodMatriculationEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "academic_period_id", nullable = false)
    private AcademicPeriodEntity academicPeriod;
}
