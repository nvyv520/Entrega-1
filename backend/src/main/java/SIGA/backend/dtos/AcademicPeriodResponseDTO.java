package SIGA.backend.dtos;

import SIGA.backend.entities.AcademicPeriodEntity;
import SIGA.backend.entities.AcademicPeriodStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class AcademicPeriodResponseDTO {

    private Long id;
    private Integer year;
    private Integer semester;
    private String code;
    private AcademicPeriodStatus status;
    

    public AcademicPeriodResponseDTO(AcademicPeriodEntity period) {
        this.id = period.getId();
        this.year = period.getYear();
        this.semester = period.getSemester();
        this.code = period.getCode();
        this.status = period.getStatus();
    }
}
