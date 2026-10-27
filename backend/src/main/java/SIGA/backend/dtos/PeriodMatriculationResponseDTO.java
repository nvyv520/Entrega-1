package SIGA.backend.dtos;

import SIGA.backend.entities.PeriodMatriculationEntity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class PeriodMatriculationResponseDTO {
    
    private Long id;
    private Long studentId;
    private Long academicPeriodId;

    public PeriodMatriculationResponseDTO(PeriodMatriculationEntity matriculation) {

        this.id = matriculation.getId();
        this.studentId = matriculation.getStudent().getId();
        this.academicPeriodId = matriculation.getAcademicPeriod().getId();
    }
}
