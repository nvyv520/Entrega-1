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
    private Long subjectId;
    private Long academicPeriodId;
    private Long teacherId;
    private Integer capacity;
    private Integer enrolledStudents;
    private Status status;
    private Set<String> blocks;

    public SectionResponseDTO(SectionEntity section) {
        this.id = section.getId();
        this.subjectId = section.getSubject().getId();
        this.academicPeriodId = section.getAcademicPeriod().getId();
        this.teacherId = section.getTeacher().getId();
        this.capacity = section.getCapacity();
        this.enrolledStudents = section.getEnrolledStudents();
        this.status = section.getStatus();
        this.blocks = Set.copyOf(section.getBlocks());
    }
    
}
