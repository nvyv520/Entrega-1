package SIGA.backend.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import SIGA.backend.entities.AcademicPeriodStatus;
import SIGA.backend.entities.Status;
import SIGA.backend.entities.TeacherStatus;
import SIGA.backend.entities.TeacherEntity;

import SIGA.backend.repositories.SectionRepository;
import SIGA.backend.repositories.StudentRepository;
import SIGA.backend.repositories.TeacherRepository;

import SIGA.backend.dtos.TeacherRequestDTO;
import SIGA.backend.dtos.TeacherResponseDTO;
import lombok.RequiredArgsConstructor;
import SIGA.backend.dtos.TeacherRegistrationResponseDTO;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


@Service 
@RequiredArgsConstructor 
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;
    private final KeycloakAdminService keycloakAdminService;

    // retrieve all teachers
    @Transactional (readOnly = true)
    public List<TeacherResponseDTO> getAllTeachers() {
        List<TeacherEntity> teachers = teacherRepository.findAll();
        return teachers.stream().map(TeacherResponseDTO::new).toList();
    }


    // find teacher by id
    private TeacherEntity findTeacherById(Long id) {
        return teacherRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Docente no encontrado"));
    }


    // get teacher by id
    @Transactional(readOnly = true)
    public TeacherResponseDTO getTeacherById(Long id) {
        TeacherEntity teacher = findTeacherById(id);
        return new TeacherResponseDTO(teacher);
    }


    // get teacher by rut 
    @Transactional (readOnly = true)
    public TeacherResponseDTO getTeacherByRut(String rut) {
        TeacherEntity teacher = teacherRepository.findByRut(rut.trim()).orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Docente no encontrado"));
        return new TeacherResponseDTO(teacher);
    }


    // get teachers by status
    @Transactional (readOnly = true)
    public List<TeacherResponseDTO> getTeachersByStatus(TeacherStatus status) {
        List<TeacherEntity> teachers =  teacherRepository.findByStatus(status == TeacherStatus.ACTIVO ? Status.ACTIVA : Status.INACTIVA);
        return teachers.stream().map(TeacherResponseDTO::new).toList();
    }

    // get teachers by first name
    @Transactional (readOnly = true)
    public List<TeacherResponseDTO> getTeachersByFirstName(String firstName) {
        List<TeacherEntity> teachers = teacherRepository.findByFirstNameContainsIgnoreCase(firstName.trim());
        return teachers.stream().map(TeacherResponseDTO::new).toList();
    }


    // get teachers by last name
    @Transactional (readOnly = true)
    public List<TeacherResponseDTO> getTeachersByLastName(String lastName) {
        List<TeacherEntity> teachers = teacherRepository.findByLastNameContainsIgnoreCase(lastName.trim());
        return teachers.stream().map(TeacherResponseDTO::new).toList();
    }



    // create teacher
    @Transactional
    public TeacherRegistrationResponseDTO createTeacher(TeacherRequestDTO teacher) {

        String rut = AcademicPersonValidation.normalizeRut(teacher.getRut(), null);
        String email = AcademicPersonValidation.normalizeEmail(teacher.getEmail(), null);

        // check duplicate rut
        if (!teacherRepository.findByNormalizedRut(AcademicPersonValidation.compactRut(rut)).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un docente con ese RUT");
        }

        // check duplicate email
        if (teacherRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un docente con ese correo");
        }

        String firstName = AcademicPersonValidation.normalizeName(teacher.getFirstName(), null);
        String lastName = AcademicPersonValidation.normalizeName(teacher.getLastName(), null);
        String professionalTitle = teacher.getProfessionalTitle().trim();
        String academicDegree = teacher.getAcademicDegree().trim();

        // create Keycloak account
        KeycloakAdminService.CreatedUser keycloakUser =
                keycloakAdminService.createUser(email, firstName, lastName, "TEACHER");

        // remove new account if the transaction fails
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    try {
                        keycloakAdminService.deleteCreatedUser(keycloakUser);
                    } catch (Exception exception) {
                        org.slf4j.LoggerFactory.getLogger(TeacherService.class)
                                .error("Failed to clean up Keycloak account {}; manual reconciliation required", keycloakUser.keycloakSubject());
                    }
                } else if (status == TransactionSynchronization.STATUS_UNKNOWN) {
                    org.slf4j.LoggerFactory.getLogger(TeacherService.class)
                            .error("Uncertain database transaction for Keycloak account {}; manual reconciliation required", keycloakUser.keycloakSubject());
                }
            }
        });

        // create teacher
        TeacherEntity newTeacher = new TeacherEntity();
        newTeacher.setRut(rut);
        newTeacher.setFirstName(firstName);
        newTeacher.setLastName(lastName);
        newTeacher.setEmail(email);
        newTeacher.setProfessionalTitle(professionalTitle);
        newTeacher.setAcademicDegree(academicDegree);
        newTeacher.setStatus(Status.ACTIVA);
        newTeacher.setKeycloakSubject(keycloakUser.keycloakSubject());

        TeacherEntity saved = teacherRepository.saveAndFlush(newTeacher);

        return new TeacherRegistrationResponseDTO(
                new TeacherResponseDTO(saved),
                keycloakUser.username(),
                keycloakUser.temporaryPassword()
        );
    }



    // update teacher
    @Transactional 
    public TeacherResponseDTO updateTeacher(Long id, TeacherRequestDTO teacher) {

        TeacherEntity current = teacherRepository.findByIdForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Docente no encontrado"));

        String rut = AcademicPersonValidation.normalizeRut(teacher.getRut(), current.getRut());
        String email = AcademicPersonValidation.normalizeEmail(teacher.getEmail(), current.getEmail());

        // Checks for a duplicate RUN
        if (teacherRepository.findByNormalizedRut(AcademicPersonValidation.compactRut(rut)).stream()
                .anyMatch(existing -> !existing.getId().equals(id))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un docente con ese RUT");
        }

        // check duplicate email
        teacherRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un docente con ese correo");}
        });

        // Updates the teacher information
        String firstName = AcademicPersonValidation.normalizeName(teacher.getFirstName(), current.getFirstName());
        String lastName = AcademicPersonValidation.normalizeName(teacher.getLastName(), current.getLastName());
        keycloakAdminService.updateUserIdentity(current.getKeycloakSubject(), "TEACHER", current.getEmail(),
                current.getFirstName(), current.getLastName(), email, firstName, lastName);
        current.setRut(rut);
        current.setFirstName(firstName);
        current.setLastName(lastName);
        current.setEmail(email);
        current.setProfessionalTitle(teacher.getProfessionalTitle().trim());
        current.setAcademicDegree(teacher.getAcademicDegree().trim());

        TeacherEntity saved = teacherRepository.save(current);
        return new TeacherResponseDTO(saved);
    }


    // update teacher status
    @Transactional 
    public TeacherResponseDTO updateTeacherStatus(Long id, TeacherStatus status) {

        TeacherEntity teacher = findTeacherById(id);

        // Validates the status
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EL estado es obligatorio");
        }

        // Checks the teacher sections in open periods
        if (status == TeacherStatus.INACTIVO && sectionRepository.existsByTeacherIdAndAcademicPeriod_Status(id, AcademicPeriodStatus.ABIERTO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puedde cambiar el estado, ya que cuenta con seccione activas");
        }

        // update 
        teacher.setStatus(status == TeacherStatus.ACTIVO ? Status.ACTIVA : Status.INACTIVA);

        TeacherEntity saved = teacherRepository.save(teacher);
        return new TeacherResponseDTO(saved);

    }


    // delete teacher
    @Transactional 
    public void deleteTeacher(Long id) {
        
        TeacherEntity teacher = findTeacherById(id);

        // Checks for associated sections
        if (sectionRepository.existsByTeacherId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar un docente con secciones asociadas");
        }

        teacherRepository.delete(teacher);
    }


    // link teacher with Keycloak
    @Transactional
    public TeacherResponseDTO linkTeacherKeycloak(Long id, String keycloakSubject) {

        TeacherEntity teacher = findTeacherById(id);

        if (keycloakSubject == null || keycloakSubject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador de Keycloak es obligatorio");
        }

        String subject;
        try {
            subject = java.util.UUID.fromString(keycloakSubject.trim()).toString();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador de Keycloak inválido");
        }

        // Checks that the account is not linked to another teacher
        teacherRepository.findByKeycloakSubject(subject).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta de Keycloak ya esta asociada a otro docente");
            }
        });

        // Prevents replacing an existing account
        if (teacher.getKeycloakSubject() != null
                && !teacher.getKeycloakSubject().equals(subject)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El docente ya tiene una cuenta de Keycloak asociada");
        }

        // Checks that the account does not belong to a student
        if (studentRepository.findByKeycloakSubject(subject).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta de Keycloak ya esta asociada a un estudiante");
        }

        // validate Keycloak user and role
        keycloakAdminService.validateUserRole(subject, "TEACHER");

        // Links the account
        teacher.setKeycloakSubject(subject);

        TeacherEntity saved = teacherRepository.save(teacher);
        return new TeacherResponseDTO(saved);
    }

    // get authenticated teacher
    @Transactional(readOnly = true)
    public TeacherResponseDTO getCurrentTeacher(String keycloakSubject) {

        TeacherEntity teacher = teacherRepository
                .findByKeycloakSubject(keycloakSubject)
                .orElseThrow(() -> new ResponseStatusException( HttpStatus.NOT_FOUND, "No existe un docente asociado a esta cuenta"));

        return new TeacherResponseDTO(teacher);
    }


}
