package SIGA.backend.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import SIGA.backend.entities.AcademicPeriodStatus;
import SIGA.backend.entities.Status;
import SIGA.backend.entities.TeacherEntity;

import SIGA.backend.repositories.SectionRepository;
import SIGA.backend.repositories.StudentRepository;
import SIGA.backend.repositories.TeacherRepository;

import SIGA.backend.dtos.TeacherRequestDTO;
import SIGA.backend.dtos.TeacherResponseDTO;
import lombok.RequiredArgsConstructor;


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
    public List<TeacherResponseDTO> getTeachersByStatus(Status status) {
        List<TeacherEntity> teachers =  teacherRepository.findByStatus(status);
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
    public TeacherResponseDTO createTeacher(TeacherRequestDTO teacher) {

        String rut = AcademicPersonValidation.normalizeRut(teacher.getRut(), null);
        String email = AcademicPersonValidation.normalizeEmail(teacher.getEmail(), null);

        // ver si esta duplicado el rut
        if (!teacherRepository.findByNormalizedRut(AcademicPersonValidation.compactRut(rut)).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un docente con ese RUT");
        }

        // ver si esta duplicado el email
        if (teacherRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un docente con ese correo");
        }

        // create teacher
        TeacherEntity newTeacher = new TeacherEntity();
        newTeacher.setRut(rut);
        newTeacher.setFirstName(AcademicPersonValidation.normalizeName(teacher.getFirstName(), null));
        newTeacher.setLastName(AcademicPersonValidation.normalizeName(teacher.getLastName(), null));
        newTeacher.setEmail(email);
        newTeacher.setProfessionalTitle(teacher.getProfessionalTitle().trim());
        newTeacher.setAcademicDegree(teacher.getAcademicDegree().trim());
        newTeacher.setStatus(Status.ACTIVA);

        TeacherEntity saved = teacherRepository.save(newTeacher);
        return new TeacherResponseDTO(saved);
    }


    // update teacher
    @Transactional 
    public TeacherResponseDTO updateTeacher(Long id, TeacherRequestDTO teacher) {

        TeacherEntity current = findTeacherById(id);

        String rut = AcademicPersonValidation.normalizeRut(teacher.getRut(), current.getRut());
        String email = AcademicPersonValidation.normalizeEmail(teacher.getEmail(), current.getEmail());

        // ver si esta duplicado el rut
        if (teacherRepository.findByNormalizedRut(AcademicPersonValidation.compactRut(rut)).stream()
                .anyMatch(existing -> !existing.getId().equals(id))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un docente con ese RUT");
        }

        // check duplicate email
        teacherRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un docente con ese correo");}
        });

        // actualizar
        current.setRut(rut);
        current.setFirstName(AcademicPersonValidation.normalizeName(teacher.getFirstName(), current.getFirstName()));
        current.setLastName(AcademicPersonValidation.normalizeName(teacher.getLastName(), current.getLastName()));
        current.setEmail(email);
        current.setProfessionalTitle(teacher.getProfessionalTitle().trim());
        current.setAcademicDegree(teacher.getAcademicDegree().trim());

        TeacherEntity saved = teacherRepository.save(current);
        return new TeacherResponseDTO(saved);
    }


    // update teacher status
    @Transactional 
    public TeacherResponseDTO updateTeacherStatus(Long id, Status status) {

        TeacherEntity teacher = findTeacherById(id);

        // validar estado
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EL estado es obligatorio");
        }

        // ver secciones activas del docente
        if (status == Status.INACTIVA && sectionRepository.existsByTeacherIdAndAcademicPeriod_Status(id, AcademicPeriodStatus.ABIERTO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puedde cambiar el estado, ya que cuenta con seccione activas");
        }

        // update 
        teacher.setStatus(status);

        TeacherEntity saved = teacherRepository.save(teacher);
        return new TeacherResponseDTO(saved);

    }


    // delete teacher
    @Transactional 
    public void deleteTeacher(Long id) {
        
        TeacherEntity teacher = findTeacherById(id);

        // revisar secciones asociadas
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

        String subject = keycloakSubject.trim();

        // verificar que la cuenta no este asociada a otro docente
        teacherRepository.findByKeycloakSubject(subject).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta de Keycloak ya esta asociada a otro docente");
            }
        });

        // no permitir reemplazar una cuenta existente
        if (teacher.getKeycloakSubject() != null
                && !teacher.getKeycloakSubject().equals(subject)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El docente ya tiene una cuenta de Keycloak asociada");
        }

        // verificar que la cuenta no pertenezca a un estudiante
        if (studentRepository.findByKeycloakSubject(subject).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta de Keycloak ya esta asociada a un estudiante");
        }

        // validate Keycloak user and role
        keycloakAdminService.validateUserRole(subject, "TEACHER");

        // vincular cuenta
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