package SIGA.backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@Data
public class StudentRequestDTO {

    @NotBlank(message = "El RUT es obligatorio")
    @Size(max = 12, message = "El RUT supera la longitud permitida")
    private String rut;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre supera la longitud permitida")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido supera la longitud permitida")
    private String lastName;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es valido")
    @Size(max = 300, message = "El correo supera la longitud permitda")
    private String email;

    @NotNull(message = "La carrera es obligatoria")
    @JsonDeserialize(using = LongRequestDeserializer.class)
    private Long careerId;

    @JsonDeserialize(using = LongRequestDeserializer.class)
    private Long studyPlanId;
}
