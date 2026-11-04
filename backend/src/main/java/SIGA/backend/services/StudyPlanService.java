package SIGA.backend.services;

import SIGA.backend.entities.StudyPlanEntity;
import SIGA.backend.entities.StudyPlanStatus;
import SIGA.backend.entities.CareerEntity;

import SIGA.backend.repositories.StudyPlanRepository;
import SIGA.backend.repositories.CareerRepository;
import SIGA.backend.repositories.StudentRepository;
import SIGA.backend.repositories.SubjectRepository;

import SIGA.backend.dtos.StudyPlanRequestDTO;
import SIGA.backend.dtos.StudyPlanResponseDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.util.List;

@Service 
@RequiredArgsConstructor 

public class StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final CareerRepository careerRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final EntityManager entityManager;


    // retrieve all study plans and map them to responseDTO
    @Transactional (readOnly = true)
    public List<StudyPlanResponseDTO> getAllStudyPlans() {
        List<StudyPlanEntity> studyPlans = studyPlanRepository.findAll();
        return studyPlans.stream().map(StudyPlanResponseDTO::new).toList();
    }


    // find study plan entity by id
    private StudyPlanEntity findStudyPlanById(Long id) {
        return studyPlanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan de estudios no encontrado"));
    }


    // get a study plan by id
    @Transactional (readOnly = true)
    public StudyPlanResponseDTO getStudyPlanById(Long id) {
        StudyPlanEntity studyPlan = findStudyPlanById(id);
        return new StudyPlanResponseDTO(studyPlan);
    }


    // get all study plans by career id
    @Transactional (readOnly = true)
    public List<StudyPlanResponseDTO> getStudyPlansByCareerId(Long careerId) {
        if (!careerRepository.existsById(careerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrera no encontrada");
        }

        List<StudyPlanEntity> studyPlans = studyPlanRepository.findByCareerId(careerId);

        return studyPlans.stream().map(StudyPlanResponseDTO::new).toList();
    }


    // create a study plan 
    @Transactional 
    public StudyPlanResponseDTO createStudyPlan(StudyPlanRequestDTO studyPlan) {

        String identifier = studyPlan.getIdentifier().trim();
        Long careerId = studyPlan.getCareerId();

        // validate existence
        CareerEntity career = careerRepository.findById(careerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrera no encontrada"));

        // check duplicate identifier
        if (studyPlanRepository.existsByCareerIdAndIdentifier(careerId, identifier)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un plan de estudios con el identificador");
        }

        // create the new plan 
        StudyPlanEntity newStudyPlan = new StudyPlanEntity();
        newStudyPlan.setIdentifier(identifier);
        newStudyPlan.setCareer(career);
        newStudyPlan.setStatus(StudyPlanStatus.NO_VIGENTE);

        StudyPlanEntity saved = studyPlanRepository.save(newStudyPlan);
        return new StudyPlanResponseDTO(saved);
    }


    // update a study plan by id 
    @Transactional 
    public StudyPlanResponseDTO updateStudyPlan(Long id, StudyPlanRequestDTO studyPlan) {

        StudyPlanEntity current = findStudyPlanById(id);
        String identifier = studyPlan.getIdentifier().trim();
        Long careerId = studyPlan.getCareerId();
        
        // Prevents changes to the associated career
        if (!current.getCareer().getId().equals(careerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede cambiar la carrera a la que pertenece un plan");
        }

        // check duplicate identifier
        studyPlanRepository.findByCareerIdAndIdentifier(careerId, identifier).ifPresent(existingStudyPlan -> {
            if (!existingStudyPlan.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un plan con el id");
            }
        });

        current.setIdentifier(identifier);

        StudyPlanEntity saved = studyPlanRepository.save(current);
        return new StudyPlanResponseDTO(saved);
    }


    // update study plan status
    @Transactional
    public StudyPlanResponseDTO updateStudyPlanStatus(Long id, StudyPlanStatus status) {

        // validate status
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado es obligatorio");
        }

        StudyPlanEntity studyPlan = findStudyPlanById(id);

        // lock career and reload the plan before changing its status
        Long careerId = studyPlan.getCareer().getId();
        entityManager.find(CareerEntity.class, careerId, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(studyPlan);

        // deactivate previous study plan
        if (status == StudyPlanStatus.VIGENTE) {
            studyPlanRepository.findByCareerIdAndStatus(careerId, StudyPlanStatus.VIGENTE)
                .ifPresent(current -> {
                    if (!current.getId().equals(id)) {
                        entityManager.refresh(current);
                        current.setStatus(StudyPlanStatus.NO_VIGENTE);
                        studyPlanRepository.saveAndFlush(current);
                    }
                });
        }

        studyPlan.setStatus(status);

        StudyPlanEntity saved = studyPlanRepository.save(studyPlan);
        return new StudyPlanResponseDTO(saved);
    }


    // delete a study plan by id 
    @Transactional 
    public void deleteStudyPlan(Long id) {
        StudyPlanEntity studyPlan = findStudyPlanById(id);

        // check if has students and subject
        if (studentRepository.existsByStudyPlanId(id) || subjectRepository.existsByStudyPlanId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar, cuenta con estudiantes o asignaturas asociadas");
        }

        studyPlanRepository.delete(studyPlan);
    }
}
