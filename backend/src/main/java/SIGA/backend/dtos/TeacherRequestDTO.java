package SIGA.backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;


@Data 
public class TeacherRequestDTO {

    @NotBlank (message = "EL RUT es obligatorio")
    @Size (max = 12, message = "El RUT supera la longitud permitida")
    private String rut;

    @NotBlank (message = "El nombre es obligatorio")
    @Size (max = 100, message = "El nombre supera la longitud permitida")
    private String firstName;

    @NotBlank (message = "El apellido es obligatorio")
    @Size (max = 100, message = "El apellido supera la longitud permitida")
    private String lastName;

    @NotBlank (message = "El correo es obligatorio")
    @Email (message = "El correo no es valido")
    @Size (max = 300, message = "El correo supera la longitud permitida")
    private String email;

    @NotBlank (message = "El título profesional es obligatorio")
    @Size (max = 150, message = "El título profesional supera la longitud permitida")
    private String professionalTitle;

    @NotBlank (message = "El gardo academico es obligatorio")
    @Size (max = 150, message = "El grado academico supera la longitud permitida")
    private String academicDegree;

}
