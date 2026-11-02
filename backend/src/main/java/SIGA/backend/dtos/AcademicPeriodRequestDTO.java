package SIGA.backend.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@Data
public class AcademicPeriodRequestDTO {
    
    @NotNull (message = "El año academico es obligatorio")
    @Min (value = 2000, message = "El año academico no es valido")
    @JsonDeserialize(using = IntegerRequestDeserializer.class)
    private Integer year;

    @NotNull (message = "El semestre es obligatorio")
    @Min (value = 1, message = "El semestre debe ser 1 o 2")
    @Max (value = 2, message = "El semestre debe ser 1 o 2")
    @JsonDeserialize(using = IntegerRequestDeserializer.class)
    private Integer semester;
}
