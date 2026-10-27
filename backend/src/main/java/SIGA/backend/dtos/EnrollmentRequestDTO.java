package SIGA.backend.dtos;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data

public class EnrollmentRequestDTO {

    @NotNull (message = "El estudiante es obligatorio")
    private Long studentId;

    @NotNull (message = "La seccion es obligatoria")
    private Long sectionId;
    
}
