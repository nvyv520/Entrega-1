package SIGA.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentRegistrationResponseDTO {

    private StudentResponseDTO student;
    private String username;
    @lombok.ToString.Exclude
    private String temporaryPassword;

}
