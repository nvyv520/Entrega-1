package SIGA.backend.entities;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity 
@Table (name = "academic_periods", uniqueConstraints = @UniqueConstraint(columnNames = {"year", "semester"}))
@Getter 
@Setter
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

    @Column (unique = true, nullable = false, length = 6)
    private String code;

    @Column (nullable = false)
    @Enumerated(EnumType.STRING)
    private AcademicPeriodStatus status;
} 
