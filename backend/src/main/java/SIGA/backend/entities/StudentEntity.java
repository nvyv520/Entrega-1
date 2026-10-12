package SIGA.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table (name = "students")
@Data 
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
    private String status;

    @ManyToOne
    @JoinColumn (name = "career_id", nullable = false)
    private CareerEntity career;

    @ManyToOne
    @JoinColumn (name = "study_plan_id", nullable = false)
    private StudyPlanEntity studyPlan;
}
