package SIGA.backend.dtos;

import lombok.Data;

import jakarta.validation.constraints.NotNull;


@Data
public class PeriodMatriculationRequestDTO {
    
    @NotNull (message = "EL estudiamte es obligatorio")
    private Long studentId;

    @NotNull (message = "El periodo academico es obligatorio")
    private Long academicPeriodId;

}
