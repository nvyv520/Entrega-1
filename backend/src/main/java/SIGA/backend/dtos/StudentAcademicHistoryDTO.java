package SIGA.backend.dtos;

import SIGA.backend.entities.EnrollmentEntity;
import SIGA.backend.entities.EnrollmentStatus;
import SIGA.backend.entities.GradeEntity;
import SIGA.backend.entities.GradeResult;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAcademicHistoryDTO {

    private Long enrollmentId;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer subjectSemester;
    private Long academicPeriodId;
    private String academicPeriodCode;
    private EnrollmentStatus enrollmentStatus;
    private BigDecimal finalGrade;
    private GradeResult result;


    public StudentAcademicHistoryDTO(EnrollmentEntity enrollment, GradeEntity grade) {

        this.enrollmentId = enrollment.getId();
        this.subjectId = enrollment.getSection().getSubject().getId();
        this.subjectCode = enrollment.getSection().getSubject().getCode();
        this.subjectName = enrollment.getSection().getSubject().getName();
        this.subjectSemester = enrollment.getSection().getSubject().getSemester();
        this.academicPeriodId = enrollment.getAcademicPeriod().getId();
        this.academicPeriodCode = enrollment.getAcademicPeriod().getCode();
        this.enrollmentStatus = enrollment.getStatus();

        if (grade != null) {
            this.finalGrade = grade.getFinalGrade();
            this.result = grade.getResult();
        }
    }
}
