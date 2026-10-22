package SIGA.backend.dtos;

import SIGA.backend.entities.SubjectEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class SubjectResponseDTO {
    private Long id;
    private String code;
    private String name;
    private Integer semester;
    private Integer theoryHours;
    private Integer exerciseHours;
    private Integer laboratoryHours;
    private Integer sct;
    private Long studyPlanId;
    private Set<Long> prerequisiteIds;

    public SubjectResponseDTO(SubjectEntity subject) {
        this.id = subject.getId();
        this.code = subject.getCode();
        this.name = subject.getName();
        this.semester = subject.getSemester();
        this.theoryHours = subject.getTheoryHours();
        this.exerciseHours = subject.getExerciseHours();
        this.laboratoryHours = subject.getLaboratoryHours();
        this.sct = subject.getSct();
        this.studyPlanId = subject.getStudyPlan().getId();
        this.prerequisiteIds = subject.getPrerequisites().stream().map(SubjectEntity::getId).collect(Collectors.toSet());
    }
}
