package SIGA.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "study_plans", uniqueConstraints = @UniqueConstraint(columnNames = {"career_id", "identifier"}))
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 

public class StudyPlanEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (nullable = false)
    private String identifier;

    @Column (nullable = false)
    @Enumerated(EnumType.STRING)
    private StudyPlanStatus status;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "career_id", nullable = false)
    private CareerEntity career;
}
