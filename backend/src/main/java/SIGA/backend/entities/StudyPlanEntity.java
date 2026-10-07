package SIGA.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "study_plans",
    uniqueConstraints = @UniqueConstraint(columnNames = {"career_id", "identifier"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor

public class StudyPlanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String identifier;

    @Column(nullable = false)
    private String status;

    @ManyToOne
    @JoinColumn(name = "career_id", nullable = false)
    private CareerEntity career;
}
