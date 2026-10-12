package SIGA.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table (name = "academic_periods")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 

public class AcademicPeriodEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (nullable = false)
    private Integer year;

    @Column (nullable = false)
    private Integer semester;

    @Column (unique = true, nullable = false)
    private String code;

    @Column (nullable = false)
    private String status;
}
