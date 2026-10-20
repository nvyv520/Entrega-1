package SIGA.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity 
@Table (name = "grades")
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 

public class GradeEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn (name = "enrollment_id", unique = true, nullable = false)
    private EnrollmentEntity enrollment;

    @Column (nullable = false)
    private BigDecimal finalGrade;

    @Column (nullable = false)
    @Enumerated(EnumType.STRING)
    private GradeResult result;
}
