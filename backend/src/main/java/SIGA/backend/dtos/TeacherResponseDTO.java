package SIGA.backend.dtos;

import SIGA.backend.entities.TeacherEntity;

import SIGA.backend.entities.Status;

import lombok.RequiredArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok. NoArgsConstructor;


@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class TeacherResponseDTO {
    private Long id;
    private String rut;
    private String firstName;
    private String lastName;
    private String email;
    private String professionalTitle;
    private String academicDegree;
    private Status status;

    public TeacherResponseDTO(TeacherEntity teacher) {
        this.id = teacher.getId();
        this.rut = teacher.getRut();
        this.firstName = teacher.getFirstName();
        this.lastName = teacher.getLastName();
        this.email = teacher.getEmail();
        this.professionalTitle = teacher.getProfessionalTitle();
        this.academicDegree = teacher.getAcademicDegree();
        this.status = teacher.getStatus();
    }
    
}
