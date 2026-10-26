package SIGA.backend.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class AcademicPeriodRequestDTO {
    
    @NotNull (message = "El año academico es obligatorio")
    @Min (value = 2000, message = "El año academico no es valido")
    private Integer year;

    @NotNull (message = "El semestre es obligatorio")
    @Min (value = 1, message = "El semestre debe ser 1 o 2")
    @Max (value = 2, message = "El semestre debe ser 1 o 2")
    private Integer semester;
}
