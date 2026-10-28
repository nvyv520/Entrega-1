package SIGA.backend.dtos;

import SIGA.backend.entities.GradeEntity;
import SIGA.backend.entities.GradeResult;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class GradeResponseDTO {

    private Long id;
    private Long enrollmentId;
    private BigDecimal finalGrade;
    private GradeResult result;

    public GradeResponseDTO(GradeEntity grade) {
        this.id = grade.getId();
        this.enrollmentId = grade.getEnrollment().getId();
        this.finalGrade = grade.getFinalGrade();
        this.result = grade.getResult();
    }
    
}
