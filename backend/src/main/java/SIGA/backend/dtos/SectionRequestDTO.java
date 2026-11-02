package SIGA.backend.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

@Data
public class SectionRequestDTO {

    @NotNull (message = "La asignatura es obligatoria")
    @JsonDeserialize(using = LongRequestDeserializer.class)
    private Long subjectId;

    @NotNull (message = "El periodo academico es obligatorio")
    @JsonDeserialize(using = LongRequestDeserializer.class)
    private Long academicPeriodId;

    @NotNull (message = "El docente es obligatorio")
    @JsonDeserialize(using = LongRequestDeserializer.class)
    private Long teacherId;

    @NotNull (message = "La capacidad es obligatoria")
    @Min (value = 1, message = "La capacidad minima es 1")
    @Max (value = 60, message = "La capacidad maxima es 60")
    @JsonDeserialize(using = IntegerRequestDeserializer.class)
    private Integer capacity;

    @NotEmpty (message = "Debe ingresar al menos un bloque horario")
    private List<@Pattern(regexp = "^[LMWJV][1-6]$", message = "El bloque horario no es valido") String> blocks;
    
}
