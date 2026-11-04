package SIGA.backend.services;

import SIGA.backend.entities.CareerEntity;
import SIGA.backend.entities.AcademicPeriodEntity;
import SIGA.backend.entities.PeriodMatriculationEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import SIGA.backend.entities.AcademicPeriodStatus;
import SIGA.backend.entities.Status;
import SIGA.backend.entities.EnrollmentEntity;
import SIGA.backend.entities.GradeEntity;
import SIGA.backend.entities.StudyPlanEntity;
import SIGA.backend.entities.StudyPlanStatus;
import SIGA.backend.entities.StudentEntity;
import SIGA.backend.entities.StudentStatus;

import SIGA.backend.repositories.StudyPlanRepository;
import SIGA.backend.repositories.GradeRepository;
import SIGA.backend.repositories.CareerRepository;
import SIGA.backend.repositories.StudentRepository;
import SIGA.backend.repositories.EnrollmentRepository;
import SIGA.backend.repositories.PeriodMatriculationRepository;
import SIGA.backend.repositories.TeacherRepository;

import SIGA.backend.dtos.StudentRequestDTO;
import SIGA.backend.dtos.StudentResponseDTO;
import SIGA.backend.dtos.StudentAcademicHistoryDTO;
import SIGA.backend.dtos.StudentRegistrationResponseDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.ArrayList;

@Service 
@RequiredArgsConstructor 
public class StudentService {

    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PeriodMatriculationRepository periodMatriculationRepository;
    private final GradeRepository gradeRepository;
    private final TeacherRepository teacherRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final EntityManager entityManager;
    
    // retrieve all student 
    @Transactional (readOnly = true)
    public List<StudentResponseDTO> getAllStudents() {
        List<StudentEntity> students =  studentRepository.findAll();
        return students.stream().map(StudentResponseDTO::new).toList();
    }


    // find student by id 
    private StudentEntity findStudentById(Long id) {
        return studentRepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));
    }


    // get student by id
    @Transactional (readOnly = true)
    public StudentResponseDTO getStudentById(Long id) {
        StudentEntity student = findStudentById(id);
        return new StudentResponseDTO(student);
    }


    // get student by rut 
    @Transactional (readOnly = true)
    public StudentResponseDTO getStudentByRut(String rut) {
        StudentEntity student = studentRepository.findByRut(rut.trim()).orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));
        return new StudentResponseDTO(student);
    }


    // get students by career
    @Transactional (readOnly = true)
    public List<StudentResponseDTO> getStudentsByCareerId(Long careerId) {

        // Checks if the career exists
        if (!careerRepository.existsById(careerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrera no encontrada");
        }

        List<StudentEntity> students = studentRepository.findByCareerId(careerId);
        return students.stream().map(StudentResponseDTO::new).toList();
    }


    // get students by status
    @Transactional (readOnly = true)
    public List<StudentResponseDTO> getStudentsByStatus(StudentStatus status) {
        List<StudentEntity> students = studentRepository.findByStatus(status);
        return students.stream().map(StudentResponseDTO::new).toList();
    }


    // get student academic history
    @Transactional(readOnly = true)
    public List<StudentAcademicHistoryDTO> getStudentAcademicHistory(Long id) {

        // Checks if the student exists
        findStudentById(id);

        // Gets the student enrollments
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudentId(id);

        List<StudentAcademicHistoryDTO> history = new ArrayList<>();
        for (EnrollmentEntity enrollment : enrollments) {

            // Only closed periods contribute consolidated academic results.
            if (enrollment.getAcademicPeriod().getStatus() != AcademicPeriodStatus.CERRADO) {
                continue;
            }

            if (!enrollmentRepository.isSelectionConfirmed(id, enrollment.getAcademicPeriod().getId())) {
                continue;
            }

            GradeEntity grade = gradeRepository.findByEnrollmentId(enrollment.getId()).orElse(null);

            // An ungraded enrollment does not establish an academic result.
            if (grade == null) {
                continue;
            }

            // Keep each graded attempt once, identified by enrollment and academic period.
            history.add(new StudentAcademicHistoryDTO(enrollment, grade));
        }


        return history;
    }



    // Finds the current study plan
    // Supports student creation
    private StudyPlanEntity findCurrentStudyPlan(Long careerId) {
        return studyPlanRepository.findByCareerIdAndStatus(careerId, StudyPlanStatus.VIGENTE).orElseThrow(() -> 
                new ResponseStatusException(HttpStatus.CONFLICT, "La carrera no tiene un plan de estudio vigente"));
    }


    // Checks if the student has academic information
    private boolean hasAcademicInformation(Long studentId) {
        return enrollmentRepository.existsByStudentId(studentId);
    }


    
    // Matriculation is specific to an academic period and independent of selection confirmation.
    private AcademicPeriodEntity validateInitialMatriculation(StudentRequestDTO student) {
        if (student.getMatriculated() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica si el estudiante está matriculado: sí o no");
        }
        if (!student.getMatriculated()) {
            if (student.getAcademicPeriodId() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo selecciona un período si el estudiante está matriculado");
            }
            return null;
        }
        if (student.getAcademicPeriodId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El período de matrícula es obligatorio");
        }
        AcademicPeriodEntity period = entityManager.find(AcademicPeriodEntity.class,
                student.getAcademicPeriodId(), LockModeType.PESSIMISTIC_WRITE);
        if (period == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Período académico no encontrado");
        }
        if (period.getStatus() != AcademicPeriodStatus.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La matrícula debe corresponder a un período abierto");
        }
        return period;
    }


    // create a student
    @Transactional
    public StudentRegistrationResponseDTO createStudent(StudentRequestDTO student) {

        String rut = AcademicPersonValidation.normalizeRut(student.getRut(), null);
        String email = AcademicPersonValidation.normalizeEmail(student.getEmail(), null);

        // check if the rut already exists
        if (!studentRepository.findByNormalizedRut(AcademicPersonValidation.compactRut(rut)).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un estudiante con ese RUT");
        }

        // check if the email already exists
        if (studentRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un estudiante con ese correo");
        }

        // find career
        CareerEntity career = careerRepository.findById(student.getCareerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrera no encontrada"));

        // validate active career
        if (career.getStatus() != Status.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pueden registrar estudiantes en una carrera inactiva");
        }

        // assign current study plan
        StudyPlanEntity studyPlan = findCurrentStudyPlan(career.getId());

        String firstName = AcademicPersonValidation.normalizeName(student.getFirstName(), null);
        String lastName = AcademicPersonValidation.normalizeName(student.getLastName(), null);

        AcademicPeriodEntity matriculationPeriod = validateInitialMatriculation(student);

        // create Keycloak user
        KeycloakAdminService.CreatedUser keycloakUser =
                keycloakAdminService.createUser(email, firstName, lastName, "STUDENT");

        // remove newly created account if the transaction rolls back
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    try {
                        keycloakAdminService.deleteCreatedUser(keycloakUser);
                    } catch (Exception exception) {
                        // The failed cleanup must be reported without exposing credentials.
                        org.slf4j.LoggerFactory.getLogger(StudentService.class)
                                .error("Failed to clean up Keycloak account {}; manual reconciliation required", keycloakUser.keycloakSubject());
                    }
                } else if (status == TransactionSynchronization.STATUS_UNKNOWN) {
                    org.slf4j.LoggerFactory.getLogger(StudentService.class)
                            .error("Uncertain database transaction for Keycloak account {}; manual reconciliation required", keycloakUser.keycloakSubject());
                }
            }
        });

        // create student
        StudentEntity newStudent = new StudentEntity();
        newStudent.setRut(rut);
        newStudent.setFirstName(firstName);
        newStudent.setLastName(lastName);
        newStudent.setEmail(email);
        newStudent.setStatus(StudentStatus.REGULAR);
        newStudent.setCareer(career);
        newStudent.setStudyPlan(studyPlan);
        newStudent.setKeycloakSubject(keycloakUser.keycloakSubject());

        StudentEntity saved = studentRepository.saveAndFlush(newStudent);

        if (matriculationPeriod != null) {
            PeriodMatriculationEntity matriculation = new PeriodMatriculationEntity();
            matriculation.setStudent(saved);
            matriculation.setAcademicPeriod(matriculationPeriod);
            matriculation.setEnrollmentConfirmed(false);
            periodMatriculationRepository.save(matriculation);
        }

        return new StudentRegistrationResponseDTO(
                new StudentResponseDTO(saved),
                keycloakUser.username(),
                keycloakUser.temporaryPassword()
        );
    }



    // update student
    @Transactional 
    public StudentResponseDTO updateStudent(Long id, StudentRequestDTO student) {

        StudentEntity current = studentRepository.findByIdForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        String rut = AcademicPersonValidation.normalizeRut(student.getRut(), current.getRut());
        String email = AcademicPersonValidation.normalizeEmail(student.getEmail(), current.getEmail());
        Long careerId = student.getCareerId();

        // Checks if the RUN already exists 
        if (studentRepository.findByNormalizedRut(AcademicPersonValidation.compactRut(rut)).stream()
                .anyMatch(existing -> !existing.getId().equals(id))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un estudiante con ese RUT");
        }

        // Checks if the email already exists
        studentRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un estudiante con ese correo");
            }
        });

        // Finds the career
        CareerEntity career = careerRepository.findById(careerId).orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrera no encontrada"));

        // Checks if the career changed
        boolean careerChanged = !current.getCareer().getId().equals(careerId);

        // Validates that the career is active
        if (careerChanged && career.getStatus() != Status.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cambiar a un acarrera inactiva");
        }

        // Finds the new study plan 
        StudyPlanEntity studyPlan;

        if (student.getStudyPlanId() != null) {
            studyPlan = studyPlanRepository.findById(student.getStudyPlanId()).orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan de estudio no encontrado"));
        }
        else if (careerChanged) {
            studyPlan = findCurrentStudyPlan(careerId);
        }
        else {
            studyPlan = current.getStudyPlan();
        }

        // Validates the new study plan 
        if (!studyPlan.getCareer().getId().equals(careerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EL plan de estudio no corresponde a la carrera");
        }

        // Checks if the study plan changed 
        boolean studyPlanChanged = !current.getStudyPlan().getId().equals(studyPlan.getId());

        // Prevents changes when academic information exists 
        if ((careerChanged || studyPlanChanged) && hasAcademicInformation(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cambiar debido a que ya cuenta con historial academico");
        }

        // Updates the student information
        String firstName = AcademicPersonValidation.normalizeName(student.getFirstName(), current.getFirstName());
        String lastName = AcademicPersonValidation.normalizeName(student.getLastName(), current.getLastName());
        keycloakAdminService.updateUserIdentity(current.getKeycloakSubject(), "STUDENT", current.getEmail(),
                current.getFirstName(), current.getLastName(), email, firstName, lastName);
        current.setRut(rut);
        current.setFirstName(firstName);
        current.setLastName(lastName);
        current.setEmail(email);
        current.setCareer(career);
        current.setStudyPlan(studyPlan);

        StudentEntity saved = studentRepository.save(current);
        return new StudentResponseDTO(saved);
    }


    // update student status
    @Transactional 
    public StudentResponseDTO updateStudentStatus(Long id, StudentStatus status) {

        StudentEntity student = findStudentById(id);

        // Validates the status
        if (status == null || status == StudentStatus.EGRESADO || status == StudentStatus.ELIMINADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No es posible cambiar el estado manualmente");
        }

        // Preserves irreversible statuses
        if (student.getStatus() == StudentStatus.EGRESADO || student.getStatus() == StudentStatus.ELIMINADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pueden modificar los estados ELiminado o Egresado");
        }

        student.setStatus(status);

        StudentEntity saved = studentRepository.save(student);
        return new StudentResponseDTO(saved);
    }


    // delete student
    @Transactional 
    public void deleteStudent(Long id) {

        StudentEntity student = findStudentById(id);

        // Checks if the student has academic information
        if (hasAcademicInformation(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puedde eliminar un estudiante con informacion academica");
        }

        // preserve administrative matriculation references
        if (periodMatriculationRepository.existsByStudentId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar un estudiante con matriculas administrativas asociadas");
        }

        studentRepository.delete(student);
    }


    // link student with Keycloak
    @Transactional
    public StudentResponseDTO linkStudentKeycloak(Long id, String keycloakSubject) {

        StudentEntity student = findStudentById(id);

        if (keycloakSubject == null || keycloakSubject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador de Keycloak es obligatorio");
        }

        String subject;
        try {
            subject = java.util.UUID.fromString(keycloakSubject.trim()).toString();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador de Keycloak inválido");
        }

        studentRepository.findByKeycloakSubject(subject).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta de Keycloak ya esta asociada a otro estudiante");
            }
        });

        if (student.getKeycloakSubject() != null
                && !student.getKeycloakSubject().equals(subject)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante ya tiene una cuenta de Keycloak asociada");
        }

        // Checks that the account does not belong to a teacher
        if (teacherRepository.findByKeycloakSubject(subject).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta de Keycloak ya esta asociada a un docente");
        }

        // validate Keycloak user and role
        keycloakAdminService.validateUserRole(subject, "STUDENT");

        student.setKeycloakSubject(subject);

        StudentEntity saved = studentRepository.save(student);
        return new StudentResponseDTO(saved);
    }


    // get authenticated student
    @Transactional(readOnly = true)
    public StudentResponseDTO getCurrentStudent(String keycloakSubject) {

        StudentEntity student = studentRepository
                .findByKeycloakSubject(keycloakSubject)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un estudiante asociado a esta cuenta"));

        return new StudentResponseDTO(student);
    }


    // get authenticated student academic history
    @Transactional(readOnly = true)
    public List<StudentAcademicHistoryDTO> getCurrentStudentAcademicHistory(String keycloakSubject) {

        StudentEntity student = studentRepository
                .findByKeycloakSubject(keycloakSubject)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un estudiante asociado a esta cuenta"));

        return getStudentAcademicHistory(student.getId());
    }
}
