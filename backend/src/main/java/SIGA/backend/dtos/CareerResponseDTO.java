package SIGA.backend.dtos;

import SIGA.backend.entities.CareerEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class CareerResponseDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String status;
    private Integer duration;

    public CareerResponseDTO(CareerEntity career) {
        this.id = career.getId();
        this.code = career.getCode();
        this.name = career.getName();
        this.description = career.getDescription();
        this.status = career.getStatus().name();
        this.duration = career.getDuration();
    }
}
