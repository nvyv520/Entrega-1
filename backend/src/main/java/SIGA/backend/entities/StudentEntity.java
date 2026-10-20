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

    @Column (unique = true, nullable = false)
    private String rut;

    @Column (nullable = false)
    private String firstName;

    @Column (nullable = false)
    private String lastName;

    @Column (unique = true, nullable = false)
    private String email;

    @Column (nullable = false)
    @Enumerated(EnumType.STRING)
    private StudentStatus status;

    @Column (unique = true)
    private String keycloakSubject;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "career_id", nullable = false)
    private CareerEntity career;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "study_plan_id", nullable = false)
    private StudyPlanEntity studyPlan;
}
