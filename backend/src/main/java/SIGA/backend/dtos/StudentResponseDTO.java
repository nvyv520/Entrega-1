package SIGA.backend.dtos;

import SIGA.backend.entities.StudentEntity;
import SIGA.backend.entities.StudentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok. NoArgsConstructor;


@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class StudentResponseDTO {
    private Long id;
    private String rut;
    private String firstName;
    private String lastName;
    private String email;
    private StudentStatus status;
    private Long careerId;
    private Long studyPlanId;

    public StudentResponseDTO(StudentEntity student) {
        this.id = student.getId();
        this.rut = student.getRut();
        this.firstName = student.getFirstName();
        this.lastName = student.getLastName();
        this.email = student.getEmail();
        this.status = student.getStatus();
        this.careerId = student.getCareer().getId();
        this.studyPlanId = student.getStudyPlan().getId();
    }
    
}
