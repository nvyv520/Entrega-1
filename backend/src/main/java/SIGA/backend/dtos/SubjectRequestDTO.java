package SIGA.backend.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Set;

@Data
public class SubjectRequestDTO {
    
    @NotBlank(message = "El código es obligatorio")
    @Size(max = 20, message = "El código no puede superar los 20 caracteres")
    private String code;

    @NotBlank (message = "El nombre es obligatorio")
    @Size (max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String name;

    @NotNull (message = "El semestre es obligatorio")
    @Min (value = 1, message = "EL semestre mínimo es 1")
    @Max (value = 4, message = "El semestre máximo es  4")
    @JsonDeserialize(using = IntegerRequestDeserializer.class)
    private Integer semester;

    @NotNull (message = "Las horas correspondientes a teoría son obligatorias")
    @Min (value = 0, message = "No se puede tener menos de 0 horas")
    @JsonDeserialize(using = IntegerRequestDeserializer.class)
    private Integer theoryHours;

    @NotNull (message = "Las horas correspondientes a ejercicios son obligatorias")
    @Min (value = 0, message = "No se puede tener menos de 0 horas")
    @JsonDeserialize(using = IntegerRequestDeserializer.class)
    private Integer exerciseHours;

    @NotNull (message = "Las horas de laboratorio son obligatorias")
    @Min (value = 0, message = "No se puede tener menos de 0 horas")
    @JsonDeserialize(using = IntegerRequestDeserializer.class)
    private Integer laboratoryHours;

    @NotNull (message = "EL SCT es obligatorio")
    @Min (value = 1, message = "El SCT mínimo es 1")
    @Max (value = 7, message = "El SCT máximo es 7")
    @JsonDeserialize(using = IntegerRequestDeserializer.class)
    private Integer sct;

    @NotNull (message = "El plan de estudio es obligatorio")
    @JsonDeserialize(using = LongRequestDeserializer.class)
    private Long studyPlanId;

    @Size (max = 3, message = "Una asignatura puede tener máximo 3 prerequisitos")
    @JsonDeserialize(contentUsing = LongRequestDeserializer.class)
    private Set<@NotNull Long> prerequisiteIds;
}
 
