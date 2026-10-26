package SIGA.backend.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import lombok.Data;

import java.util.Set;

@Data
public class SectionRequestDTO {

    @NotNull (message = "La asignatura es obligatoria")
    private Long subjectId;

    @NotNull (message = "El periodo academico es obligatorio")
    private Long academicPeriodId;

    @NotNull (message = "El docente es obligatorio")
    private Long teacherId;

    @NotNull (message = "La capacidad es obligatoria")
    @Min (value = 1, message = "La capacidad minima es 1")
    @Max (value = 60, message = "La capacidad maxima es 60")
    private Integer capacity;

    @NotEmpty (message = "Debe ingresar al menos un bloque horario")
    private Set<@Pattern(regexp = "^[LMWJV][1-6]$", message = "El bloque horario no es valido") String> blocks;
    
}
