package SIGA.backend.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;

import lombok.Data;

import java.math.BigDecimal;

@Data

public class GradeRequestDTO {

    @NotNull (message = "La inscripcion es obligatoria")
    private Long enrollmentId;

    @NotNull (message = "La nota final es obligatoria")
    @DecimalMin (value = "1.0", message = "La nota minima es 1.0")
    @DecimalMax (value = "7.0", message = "La nota maxima es 7.0")
    @Digits (integer = 1, fraction = 1, message = "La nota debe tener como maximo un decimal")
    private BigDecimal finalGrade;
    
}
