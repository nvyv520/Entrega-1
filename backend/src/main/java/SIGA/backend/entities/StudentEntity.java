package SIGA.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "students")
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 

public class StudentEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (unique = true, nullable = false, length = 12)
    private String rut;

    @Column (nullable = false, length = 100)
    private String firstName;

    @Column (nullable = false, length = 100)
    private String lastName;

    @Column (unique = true, nullable = false, length = 300)
    private String email;

    @Column (nullable = false)
    @Enumerated(EnumType.STRING)
    private StudentStatus status;

    @Column (unique = true, length = 300)
    private String keycloakSubject;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "career_id", nullable = false)
    private CareerEntity career;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "study_plan_id", nullable = false)
    private StudyPlanEntity studyPlan;
}
