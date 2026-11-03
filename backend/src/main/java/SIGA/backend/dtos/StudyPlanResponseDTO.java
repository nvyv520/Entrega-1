package SIGA.backend.dtos;

import SIGA.backend.entities.StudyPlanEntity;
import SIGA.backend.entities.StudyPlanStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok. NoArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 

public class StudyPlanResponseDTO {

    private Long id;
    private String identifier;
    private StudyPlanStatus status;
    private long careerId;
    private String careerCode;

    public StudyPlanResponseDTO(StudyPlanEntity studyPlan) {
        this.id = studyPlan.getId();
        this.identifier = studyPlan.getIdentifier();
        this.status = studyPlan.getStatus();
        this.careerId = studyPlan.getCareer().getId();
        this.careerCode = studyPlan.getCareer().getCode();
    }

    
}
