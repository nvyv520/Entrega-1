package SIGA.backend.services;

import SIGA.backend.entities.AcademicPeriodEntity;
import SIGA.backend.entities.AcademicPeriodStatus;
import SIGA.backend.entities.StudentEntity;
import SIGA.backend.entities.StudentStatus;
import SIGA.backend.entities.SubjectEntity;
import SIGA.backend.entities.EnrollmentEntity;
import SIGA.backend.entities.EnrollmentStatus;
import SIGA.backend.entities.GradeEntity;
import SIGA.backend.entities.GradeResult;
import SIGA.backend.entities.PeriodMatriculationEntity;
import SIGA.backend.entities.SectionEntity;
import SIGA.backend.entities.Status;

import SIGA.backend.repositories.AcademicPeriodRepository;
import SIGA.backend.repositories.SectionRepository;
import SIGA.backend.repositories.EnrollmentRepository;
import SIGA.backend.repositories.PeriodMatriculationRepository;
import SIGA.backend.repositories.StudentRepository;
import SIGA.backend.repositories.SubjectRepository;
import SIGA.backend.repositories.GradeRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import SIGA.backend.dtos.AcademicPeriodRequestDTO;
import SIGA.backend.dtos.AcademicPeriodResponseDTO;
import SIGA.backend.dtos.SectionResponseDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@Service 
@RequiredArgsConstructor 

public class AcademicPeriodService {

    private final AcademicPeriodRepository academicPeriodRepository;
    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PeriodMatriculationRepository periodMatriculationRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final EntityManager entityManager;

    // retrieve all academic periods
    @Transactional (readOnly = true)
    public List<AcademicPeriodResponseDTO> getAllAcademicPeriods() {
        List<AcademicPeriodEntity> periods = academicPeriodRepository.findAll();
        return periods.stream().map(AcademicPeriodResponseDTO::new).toList();
    }


    // find academic period by id
    private AcademicPeriodEntity findAcademicPeriodById(Long id) {
        return academicPeriodRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo academico no encontrado"));
    }


    // get academic period by id
    @Transactional (readOnly = true)
    public AcademicPeriodResponseDTO getAcademicPeriodById(Long id) {
        AcademicPeriodEntity period =  findAcademicPeriodById(id);
        return new AcademicPeriodResponseDTO(period);
    }


    // get academic period by code
    @Transactional (readOnly = true)
    public AcademicPeriodResponseDTO getAcademicPeriodByCode(String code) {
        AcademicPeriodEntity period = academicPeriodRepository.findByCode(code).orElseThrow(() -> 
                                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo academico no encontrado"));
                return new AcademicPeriodResponseDTO(period);
    }


    // get academic period by year
    @Transactional (readOnly = true)
    public List<AcademicPeriodResponseDTO> getAcademicPeriodsByYear(Integer year) {
        List<AcademicPeriodEntity> periods = academicPeriodRepository.findByYear(year);
        return periods.stream().map(AcademicPeriodResponseDTO::new).toList();
    }


    // get academic period by status
    @Transactional (readOnly = true)
    public List<AcademicPeriodResponseDTO> getAcademicPeriodsByStatus(AcademicPeriodStatus status) {
        List<AcademicPeriodEntity> periods = academicPeriodRepository.findByStatus(status);
        return periods.stream().map(AcademicPeriodResponseDTO::new).toList();
    }


    // validate academic period data
    private void validateAcademicPeriodData(AcademicPeriodRequestDTO period) {

        // validate year
        if (period.getYear() == null || period.getYear() < 2000 || period.getYear() > 9999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El año academico no es valido");
        }

        // Validates the semester
        if (period.getSemester() == null || period.getSemester() < 1 || period.getSemester() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El semestre debe ser 1 o 2");
        }
    }


    // Checks if the period has academic information
    private boolean hasAcademicInformation(Long periodId) {
        return sectionRepository.existsByAcademicPeriodId(periodId) ||
                enrollmentRepository.existsByAcademicPeriodId(periodId) ||
                periodMatriculationRepository.existsByAcademicPeriodId(periodId) ;
    }


    // validate grades before closing academic period
    private void validateEnrollmentsForClosing(Long periodId) {

        List<EnrollmentEntity> enrollments = enrollmentRepository.findByAcademicPeriodId(periodId);

        Set<String> pendingSections = new HashSet<>();

        for (EnrollmentEntity enrollment : enrollments) {

            // ignore cancelled enrollments
            if (enrollment.getStatus() != EnrollmentStatus.ACTIVO) {
                continue;
            }

            // Pending selections must be resolved without automatic cancellation.
            if (!enrollmentRepository.isSelectionConfirmed(enrollment.getStudent().getId(), periodId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede cerrar el periodo. Existen selecciones pendientes de confirmacion; deben verificarse o cancelarse mediante las operaciones existentes");
            }

            // check if enrollment has grade
            if (!gradeRepository.existsByEnrollmentId(enrollment.getId())) {
                pendingSections.add(new SectionResponseDTO(enrollment.getSection()).getCode());
            }
        }

        if (!pendingSections.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"No se puede cerrar el periodo. Faltan calificaciones en las secciones: " + pendingSections);
        }
    }


    // check if grade belongs to closed or current period
    private boolean isValidGradeForClosing(GradeEntity grade, Long currentPeriodId) {

        EnrollmentEntity enrollment = grade.getEnrollment();

        // ignore cancelled enrollments
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVO) {
            return false;
        }

        if (!enrollmentRepository.isSelectionConfirmed(enrollment.getStudent().getId(), enrollment.getAcademicPeriod().getId())) {
            return false;
        }

        AcademicPeriodEntity period = enrollment.getAcademicPeriod();

        // include grades from the period being closed
        if (period.getId().equals(currentPeriodId)) {
            return true;
        }

        // include grades from previously closed periods
        return period.getStatus() == AcademicPeriodStatus.CERRADO;
    }

    
    // check if student failed the same subject twice
    private boolean hasFailedSubjectTwice(Long studentId, Long currentPeriodId) {

        List<GradeEntity> grades = gradeRepository.findByEnrollment_Student_Id(studentId);

        Map<Long, Integer> failedSubjects = new HashMap<>();

        for (GradeEntity grade : grades) {

            // ignore grades from other open periods
            if (!isValidGradeForClosing(grade, currentPeriodId)) {
                continue;
            }

            if (grade.getResult() != GradeResult.REPROBADO) {
                continue;
            }

            Long subjectId = grade.getEnrollment().getSection().getSubject().getId();

            int failures = failedSubjects.getOrDefault(subjectId, 0) + 1;

            if (failures >= 2) {
                return true;
            }

            failedSubjects.put(subjectId, failures);
        }

        return false;
    }

    
    // check if student approved all subjects in study plan
    private boolean hasCompletedStudyPlan(StudentEntity student, Long currentPeriodId) {

        List<SubjectEntity> subjects = subjectRepository.findByStudyPlanId(student.getStudyPlan().getId());

        if (subjects.isEmpty()) {
            return false;
        }

        List<GradeEntity> grades = gradeRepository.findByEnrollment_Student_Id(student.getId());

        for (SubjectEntity subject : subjects) {

            boolean approved = false;

            for (GradeEntity grade : grades) {

                if (!isValidGradeForClosing(grade, currentPeriodId)) {
                    continue;
                }

                if (grade.getResult() == GradeResult.APROBADO &&
                    grade.getEnrollment().getSection().getSubject().getId().equals(subject.getId())) {
                        approved = true;
                        break;
                }
            }

            if (!approved) {
                return false;
            }
        }

        return true;
    }


    // update student academic status
    private void updateStudentAcademicStatus(StudentEntity student, Long currentPeriodId) {
        // preserve final academic statuses
        if (student.getStatus() == StudentStatus.EGRESADO || student.getStatus() == StudentStatus.ELIMINADO) {
            return;
        }

        // check repeated failures
        if (hasFailedSubjectTwice(student.getId(), currentPeriodId)) {
            student.setStatus(StudentStatus.ELIMINADO);
        }
        // check study plan completion
        else if (hasCompletedStudyPlan(student, currentPeriodId)) {
            student.setStatus(StudentStatus.EGRESADO);
        }
        // save student if status changed
        else {
            return;
        }

        studentRepository.save(student);
    }


    // create academic period
    @Transactional 
    public AcademicPeriodResponseDTO createAcademicPeriod(AcademicPeriodRequestDTO period) {
        
        // Validates the academic information
        validateAcademicPeriodData(period);

        // Uses the validated academic period data 
        Integer year = period.getYear();
        Integer semester = period.getSemester();
        String code = year + "-" + semester;

        // Checks for a duplicate academic period
        if (academicPeriodRepository.findByYearAndSemester(year, semester).isPresent() ||
            academicPeriodRepository.findByCode(code).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un periodo academico con ese año y semestre");
        }

        AcademicPeriodEntity newPeriod = new AcademicPeriodEntity();
        newPeriod.setYear(year);
        newPeriod.setSemester(semester);
        newPeriod.setCode(code);
        newPeriod.setStatus(AcademicPeriodStatus.ABIERTO);

        AcademicPeriodEntity saved = academicPeriodRepository.save(newPeriod);
        return new AcademicPeriodResponseDTO(saved);
    }


    // update academic period
    @Transactional 
    public AcademicPeriodResponseDTO updateAcademicPeriod(Long id, AcademicPeriodRequestDTO period) {

        AcademicPeriodEntity current = findAcademicPeriodById(id);

        // Validates the information 
        validateAcademicPeriodData(period);

        // Prevents updates to closed periods
        if (current.getStatus() == AcademicPeriodStatus.CERRADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede modificar un periodo cerrado");
        }

        // Uses the validated academic period data 
        Integer year = period.getYear();
        Integer semester = period.getSemester();
        String code = year + "-" + semester;

        // Checks if the year or semester changed
        boolean changes = !current.getYear().equals(year) || !current.getSemester().equals(semester);

        // Prevents changes when academic information exists 
        if (changes && hasAcademicInformation(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede modificar un periodo academico que cuenta con informacion");
        }

        // Checks for a duplicate academic period
        academicPeriodRepository.findByCode(code).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un periodo academico con ese codigo");
            }
        });

        current.setYear(year);
        current.setSemester(semester);
        current.setCode(code);

        AcademicPeriodEntity saved = academicPeriodRepository.save(current);
        return new AcademicPeriodResponseDTO(saved);
    }


    // delete academic period
    @Transactional 
    public void deleteAcademicPeriod(Long id) {

        AcademicPeriodEntity period = findAcademicPeriodById(id);

        // Prevents deleting closed periods 
        if (period.getStatus() == AcademicPeriodStatus.CERRADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar un periodo cerrado");
        }

        // Checks that the period has no academic information 
        if (hasAcademicInformation(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar un periodo con informacion");
        }

        academicPeriodRepository.delete(period);
    }


    // close academic period
    @Transactional
    public AcademicPeriodResponseDTO closeAcademicPeriod(Long id) {

        // lock academic period during closing
        AcademicPeriodEntity period = entityManager.find(AcademicPeriodEntity.class, id, LockModeType.PESSIMISTIC_WRITE);

        if (period == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo academico no encontrado");
        }

        // check academic period status
        if (period.getStatus() != AcademicPeriodStatus.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El periodo academico ya esta cerrado");
        }

        List<PeriodMatriculationEntity> matriculations = periodMatriculationRepository.findByAcademicPeriodId(id);

        // verify all active enrollments have grades
        validateEnrollmentsForClosing(id);

        // update academic status of each student
        for (PeriodMatriculationEntity matriculation : matriculations) {
            StudentEntity student = matriculation.getStudent();
            updateStudentAcademicStatus(student, id);
        }

        // Inactivate sections in the same transaction as the academic closing.
        List<SectionEntity> sections = sectionRepository.findByAcademicPeriodId(id);
        for (SectionEntity section : sections) {
            section.setStatus(Status.INACTIVA);
        }
        sectionRepository.saveAll(sections);

        // close academic period
        period.setStatus(AcademicPeriodStatus.CERRADO);

        AcademicPeriodEntity saved = academicPeriodRepository.save(period);
        return new AcademicPeriodResponseDTO(saved);
    }

}
