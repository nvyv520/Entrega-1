package SIGA.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRegistrationResponseDTO {

    private TeacherResponseDTO teacher;
    private String username;
    @lombok.ToString.Exclude
    private String temporaryPassword;

}
