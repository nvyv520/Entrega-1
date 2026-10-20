package SIGA.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity 
@Table (name = "careers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class CareerEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (unique = true, nullable = false, length = 6)
    private String code;

    @Column (nullable = false, length = 100)
    private String name;

    @Column (nullable = false, length = 1000)
    private String description;

    @Column (nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column (nullable = false)
    private Integer duration;
}
