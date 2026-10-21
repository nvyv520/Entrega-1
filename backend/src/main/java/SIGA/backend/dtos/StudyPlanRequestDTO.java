package SIGA.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudyPlanRequestDTO {

    @NotBlank(message = "El identificador es obligatorio")
    @Size(max = 50, message = "El identificador excede el máximo de 50 caracteres")
    private String identifier;

    @NotNull(message = "La carrera es obligatoria")
    private Long careerId;
    
}
