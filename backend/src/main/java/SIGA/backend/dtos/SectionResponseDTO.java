package SIGA.backend.dtos;

import SIGA.backend.entities.SectionEntity;
import SIGA.backend.entities.Status;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class SectionResponseDTO {

    private Long id;
    private String code;
    private String subjectCode;
    private String academicPeriodCode;
    private Long subjectId;
    private Long academicPeriodId;
    private Long teacherId;
    private Integer capacity;
    private Integer enrolledStudents;
    private Status status;
    private Set<String> blocks;

    // Expose only the display name, without granting access to teacher administration.
    private String teacherName;

    public SectionResponseDTO(SectionEntity section) {
        this.id = section.getId();
        // Only one section is allowed per subject and academic period.
        this.code = section.getSubject().getCode() +
                String.format(java.util.Locale.ROOT, "%02d", section.getAcademicPeriod().getYear() % 100) +
                section.getAcademicPeriod().getSemester() + "-1";
        this.subjectId = section.getSubject().getId();
        this.subjectCode = section.getSubject().getCode();
        this.academicPeriodCode = section.getAcademicPeriod().getCode();
        this.academicPeriodId = section.getAcademicPeriod().getId();
        this.teacherId = section.getTeacher() == null ? null : section.getTeacher().getId();
        this.teacherName = section.getTeacher() == null ? null :
                section.getTeacher().getFirstName() + " " + section.getTeacher().getLastName();
        this.capacity = section.getCapacity();
        this.enrolledStudents = section.getEnrolledStudents();
        this.status = section.getStatus();
        this.blocks = Set.copyOf(section.getBlocks());
    }
    
}
