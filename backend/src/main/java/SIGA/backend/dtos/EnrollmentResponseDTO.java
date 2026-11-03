package SIGA.backend.dtos;

import SIGA.backend.entities.EnrollmentEntity;
import SIGA.backend.entities.EnrollmentStatus;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class EnrollmentResponseDTO {

    private Long id;
    private Long studentId;
    private Long sectionId;
    private Long academicPeriodId;
    private LocalDate enrollmentDate;
    private EnrollmentStatus status;
    private boolean confirmed;

    public EnrollmentResponseDTO(EnrollmentEntity enrrollment) {
        this.id = enrrollment.getId();
        this.studentId = enrrollment.getStudent().getId();
        this.sectionId = enrrollment.getSection().getId();
        this.academicPeriodId = enrrollment.getAcademicPeriod().getId();
        this.enrollmentDate = enrrollment.getEnrollmentDate();
        this.status = enrrollment.getStatus();
    }

    
}
