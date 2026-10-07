package SIGA.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "grades")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class GradeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "enrollment_id", unique = true, nullable = false)
    private EnrollmentEntity enrollment;

    @Column(nullable = false)
    private Double finalGrade;

    @Column(nullable = false)
    private String result;
}
