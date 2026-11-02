package SIGA.backend.dtos;

import SIGA.backend.entities.TeacherEntity;

import SIGA.backend.entities.Status;
import SIGA.backend.entities.TeacherStatus;

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
    private TeacherStatus status;

    public TeacherResponseDTO(TeacherEntity teacher) {
        this.id = teacher.getId();
        this.rut = teacher.getRut();
        this.firstName = teacher.getFirstName();
        this.lastName = teacher.getLastName();
        this.email = teacher.getEmail();
        this.professionalTitle = teacher.getProfessionalTitle();
        this.academicDegree = teacher.getAcademicDegree();
        // Exposes teacher states while preserving stored status values.
        this.status = teacher.getStatus() == Status.ACTIVA ? TeacherStatus.ACTIVO : TeacherStatus.INACTIVO;
    }
    
}
