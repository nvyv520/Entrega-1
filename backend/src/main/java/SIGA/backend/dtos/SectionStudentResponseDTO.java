
package SIGA.backend.dtos;

import SIGA.backend.entities.StudentEntity;
import SIGA.backend.entities.EnrollmentEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SectionStudentResponseDTO {

    private Long id;
    private Long enrollmentId;
    private String firstName;
    private String lastName;

    public SectionStudentResponseDTO(StudentEntity student) {
        this.id = student.getId();
        this.firstName = student.getFirstName();
        this.lastName = student.getLastName();
    }

    public SectionStudentResponseDTO(EnrollmentEntity enrollment) {
        this(enrollment.getStudent());
        this.enrollmentId = enrollment.getId();
    }
}
