package SIGA.backend.services;

import SIGA.backend.entities.SubjectEntity;
import SIGA.backend.entities.StudyPlanEntity;

import SIGA.backend.repositories.SubjectRepository;
import SIGA.backend.repositories.StudyPlanRepository;
import SIGA.backend.repositories.SectionRepository;
import SIGA.backend.repositories.EnrollmentRepository;

import SIGA.backend.dtos.SubjectRequestDTO;
import SIGA.backend.dtos.SubjectResponseDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service 
@RequiredArgsConstructor 
public class SubjectService {

    private final SubjectRepository  subjectRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;

    // retrieve all subject
    @Transactional (readOnly = true)
    public List<SubjectResponseDTO> getAllSubjects() {
        List<SubjectEntity> subject = subjectRepository.findAll();
        return subject.stream().map(SubjectResponseDTO::new).toList();
    }


    // find subject by id
    private SubjectEntity findSubjectById(Long id) {
        return subjectRepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignatura no encontrada"));
    }

    
    // get subject by id
    @Transactional (readOnly = true)
    public SubjectResponseDTO getSubjectById(Long id) {
        SubjectEntity subject = findSubjectById(id);
        return new SubjectResponseDTO(subject);
    }


    // get subject by study plan id
    @Transactional (readOnly = true)
    public List<SubjectResponseDTO> getSubjectByStudyPlanId(Long studyPlanId) {

        // validate the existence of the plan 
        if (!studyPlanRepository.existsById(studyPlanId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontro el plan de estudio");

        }

        List<SubjectEntity> subjects = subjectRepository.findByStudyPlanId(studyPlanId);
        return subjects.stream().map(SubjectResponseDTO::new).toList();
    }


    // validate information of the subject
    private void validateSubjectData(SubjectRequestDTO subject) {

        // Checks for required information
        if (subject.getCode() == null || subject.getCode().isBlank() ||
            subject.getName() == null || subject.getName().isBlank() ||
            subject.getStudyPlanId() == null){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código, nombre y plan de estudio son obligatorios");
        }

        // Checks that the values do not exceed the allowed length
        if (subject.getCode().trim().length() > 20 || subject.getName().trim().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código o nombre superan la longitud permitida");
        }

        // Checks that the semester is within the allowed range
        if (subject.getSemester() == null || subject.getSemester() < 1 || subject.getSemester() > 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El semestre debe estar entre 1 y 4");
        }

        // Checks that the credits are within the allowed range
        if (subject.getSct() == null || subject.getSct() < 1 || subject.getSct() > 7) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El SCT debe estar entre 1 y 7");
        }

        // Checks the corresponding hours
        Integer theory = subject.getTheoryHours();
        Integer exercise  = subject.getExerciseHours();
        Integer lab = subject.getLaboratoryHours();
        
        if (theory == null || theory < 0 ||
            exercise == null || exercise < 0 ||
            lab == null || lab < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las horas correspondientes son obligatorias");
        }

        if ( (long) theory + exercise + lab > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La suma de horas TEL no puede superar 8 horas semanales");
        }
    }


    // Validates the prerequisites
    private Set<SubjectEntity> validatePrerequisites(Set<Long> prerequisiteIds, Long studyPlanId, Integer semester, Long subjectId) {

        Set<SubjectEntity> prerequisites = new HashSet<>();

        // Handles subjects without prerequisites
        if (prerequisiteIds == null || prerequisiteIds.isEmpty()) {
            return prerequisites;
        }

        // Validates the maximum number of prerequisites
        if (prerequisiteIds.size() > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se excede el máximo de prerequisitos permitidos");
        }

        // Checks the prerequisite rules
        for (Long prerequisiteId : prerequisiteIds) {

            // Checks the prerequisite ID
            if (prerequisiteId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El Id del prerequisito es obligatorio");
            }

            // Prevents a subject from being its own prerequisite
            if (prerequisiteId.equals(subjectId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La asignatura no puede ser su propio prerequisito");
            }

            // Finds the prerequisite
            SubjectEntity prerequisite = subjectRepository.findById(prerequisiteId).orElseThrow(() -> 
                                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Prerequisito no encontrado"));

            // Validates that both subjects belong to the same study plan
            if (!prerequisite.getStudyPlan().getId().equals(studyPlanId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las asignaturas no pertenecen al mismo plan de estudio");
            }

            // Validates the semester order
            if (prerequisite.getSemester() >= semester) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El prerequisito debe ser de un semestre anterior");
            }

            prerequisites.add(prerequisite);

        }

        return prerequisites;
    }


    // create a subject
    @Transactional 
    public SubjectResponseDTO createSubject(SubjectRequestDTO subject) {

        // validate information of the subject
        validateSubjectData(subject);

        String code = subject.getCode().trim();

        // Validates the study plan
        StudyPlanEntity studyPlan = studyPlanRepository.findById(subject.getStudyPlanId())
                                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan de estudio no encontrado"));
        
        // Checks if the subject already exists
        if (subjectRepository.existsByStudyPlanIdAndCode(studyPlan.getId(), code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una asignatura con el código");
        }

        // Validates the prerequisites
        Set<SubjectEntity> prerequisites = validatePrerequisites(subject.getPrerequisiteIds(), studyPlan.getId(), subject.getSemester(), null);

        // create subject
        SubjectEntity newSubject = new SubjectEntity();
        newSubject.setCode(code);
        newSubject.setName(subject.getName().trim());
        newSubject.setSemester(subject.getSemester());
        newSubject.setTheoryHours(subject.getTheoryHours());
        newSubject.setExerciseHours(subject.getExerciseHours());
        newSubject.setLaboratoryHours(subject.getLaboratoryHours());
        newSubject.setSct(subject.getSct());
        newSubject.setStudyPlan(studyPlan);
        newSubject.setPrerequisites(prerequisites);

        SubjectEntity saved = subjectRepository.save(newSubject);
        return new SubjectResponseDTO(saved);
    }


    // update subject
    @Transactional 
    public SubjectResponseDTO updateSubject(Long id, SubjectRequestDTO subject) {

        SubjectEntity current = findSubjectById(id);

        // Validates the data 
        validateSubjectData(subject);

        String code = subject.getCode().trim();
        Long studyPlanId = subject.getStudyPlanId();

        // Prevents changes to the study plan 
        if (!current.getStudyPlan().getId().equals(studyPlanId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede cambiar el plan de estudios al que pertenece");
        }

        // Prevents changes to the semester
        if (!current.getSemester().equals(subject.getSemester())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "No se puede modificar el semestre de una asignatura");
        }

        // Prevents changes to academic information
        if (enrollmentRepository.existsBySection_Subject_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede modificar una asignatura con historial académico");
        }

        // Prevents TEL hour changes when sections exist
        if (sectionRepository.existsBySubjectId(id) &&
            (!current.getTheoryHours().equals(subject.getTheoryHours()) ||
            !current.getExerciseHours().equals(subject.getExerciseHours()) ||
            !current.getLaboratoryHours().equals(subject.getLaboratoryHours()))) {

            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pueden modificar las horas TEL de una asignatura con secciones");
        }

        // Checks for a duplicate subject 
        subjectRepository.findByStudyPlanIdAndCode(studyPlanId, code).ifPresent(existingSubject -> {
            if (!existingSubject.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una asignatura con el codigo");
            }
        });

        // Validates the prerequisites
        Set<SubjectEntity> prerequisites =  validatePrerequisites(subject.getPrerequisiteIds(), studyPlanId, subject.getSemester(), id);

        // Updates the subject information
        current.setCode(code);
        current.setName(subject.getName().trim());
        current.setSemester(subject.getSemester());
        current.setTheoryHours(subject.getTheoryHours());
        current.setExerciseHours(subject.getExerciseHours());
        current.setLaboratoryHours(subject.getLaboratoryHours());
        current.setSct(subject.getSct());
        current.setPrerequisites(prerequisites);

        SubjectEntity saved = subjectRepository.save(current);
        return new SubjectResponseDTO(saved);
    }


    // delete a subject
    @Transactional 
    public void deleteSubject(Long id) {
        SubjectEntity subject = findSubjectById(id);

        // Checks the subject enrollments
        // Preserves the academic records
        if (enrollmentRepository.existsBySection_Subject_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No es posible eliminar la asigantura, ya que forma parte de historiales academicos");
        }

        // Checks for associated sections or active prerequisite references
        if (sectionRepository.existsBySubjectId(id) || subjectRepository.existsByPrerequisites_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar una asignatura con secciones o prerequisito activo");
        }

        subjectRepository.delete(subject);
    }
}
