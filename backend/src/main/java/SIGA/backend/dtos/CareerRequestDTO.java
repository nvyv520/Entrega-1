package SIGA.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data 

public class CareerRequestDTO {
    @NotBlank(message = "El código es obligatorio")
    @Size(max = 6, message = "El código excede el máximo de 6 caracteres")
    private String code;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message =  "El nombre excede el máximo de 100 caracteres")
    private String name;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 1000, message = "La descripción excede el máximo de 1000 caracteres")
    private String description;
}
