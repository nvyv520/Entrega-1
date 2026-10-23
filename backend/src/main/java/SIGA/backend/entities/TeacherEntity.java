package SIGA.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "teachers")
@Setter 
@Getter 
@NoArgsConstructor 
@AllArgsConstructor 

public class TeacherEntity {
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

    @Column (nullable = false, length = 150)
    private String professionalTitle;

    @Column (nullable = false, length = 150)
    private String academicDegree;

    @Column (nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column (unique = true, length = 100)
    private String keycloakSubject;
}
