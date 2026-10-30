package SIGA.backend.services;

import SIGA.backend.entities.CareerEntity;
import SIGA.backend.entities.Status;

import SIGA.backend.repositories.CareerRepository;
import SIGA.backend.repositories.StudentRepository;
import SIGA.backend.repositories.StudyPlanRepository;

import SIGA.backend.dtos.CareerRequestDTO;
import SIGA.backend.dtos.CareerResponseDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service 
@RequiredArgsConstructor

public class CareerService {
    private static final Integer CAREER_DURATION_SEMESTERS = 4;

    private final CareerRepository careerRepository;
    private final StudentRepository studentRepository;
    private final StudyPlanRepository studyPlanRepository;


    // retrieve all career and map them to responseDTO
    @Transactional(readOnly = true)
    public List<CareerResponseDTO> getAllCareers(){
        List<CareerEntity> careers = careerRepository.findAll();
        return careers.stream().map(CareerResponseDTO::new).toList();
    }
    

    // find career entity by id
    private CareerEntity findCareerById(Long id) {
        return careerRepository.findById(id).orElseThrow(() -> 
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrera no encontrada por su ID: " + id));
    }


    // find a career by id
    @Transactional(readOnly = true)
    public CareerResponseDTO getCareerById(Long id) {
        CareerEntity career = findCareerById(id);
        return new CareerResponseDTO(career);
    }


    // create a new career and set initial attributes
    @Transactional
    public CareerResponseDTO createCareer(CareerRequestDTO career) {
        String code = career.getCode().trim();

        // already exist a career with that code
        if (careerRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una carrera con el código: " + code);
        }

        // create the new career
        CareerEntity newCareer = new CareerEntity();
        newCareer.setCode(code);
        newCareer.setName(career.getName().trim());
        newCareer.setDescription(career.getDescription().trim());

        // default status and duration
        newCareer.setStatus(Status.ACTIVA);
        newCareer.setDuration(CAREER_DURATION_SEMESTERS);

        CareerEntity saved = careerRepository.save(newCareer);
        return new CareerResponseDTO(saved);
    }


    // update a career by id
    @Transactional
    public CareerResponseDTO updateCareer(Long id, CareerRequestDTO career) {
        CareerEntity currentCareer = findCareerById(id);
        String code = career.getCode().trim();

        careerRepository.findByCode(code).ifPresent(existingCareer -> {
            if (!existingCareer.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una carrera con el código: " + code);
            }
        });

        currentCareer.setCode(code);
        currentCareer.setName(career.getName().trim());
        currentCareer.setDescription(career.getDescription().trim());

        CareerEntity saved = careerRepository.save(currentCareer);
        return new CareerResponseDTO(saved);
    }

    
    // update career status
    @Transactional
    public CareerResponseDTO updateCareerStatus(Long id, Status status) {

        // validate status
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado es obligatorio");
        }

        CareerEntity career = findCareerById(id);

        career.setStatus(status);

        CareerEntity saved = careerRepository.save(career);
        return new CareerResponseDTO(saved);


    }

    
    // delete a career by id
    @Transactional
    public void deleteCareer(Long id) {
        CareerEntity career = findCareerById(id);

        // check if has students and study plans
        if (studentRepository.existsByCareerId(id) || studyPlanRepository.existsByCareerId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No es posible eliminar una carrera con estudiantes o planes asociados");
        }

        careerRepository.delete(career);
    }

}
