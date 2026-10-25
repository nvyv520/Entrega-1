package SIGA.backend.services;

import SIGA.backend.entities.TeacherEntity;
import SIGA.backend.repositories.TeacherRepository;
import SIGA.backend.dtos.TeacherRequestDTO;
import SIGA.backend.dtos.TeacherResponseDTO;

import SIGA.backend.entities.Status;

import SIGA.backend.repositories.SectionRepository;

import SIGA.backend.entities.AcademicPeriodStatus;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

import java.util.List;


@Service 
@RequiredArgsConstructor 
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;

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
        TeacherEntity teacher = teacherRepository.findByRut(rut).orElseThrow(() ->
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

        String rut = teacher.getRut().trim();
        String email = teacher.getEmail().trim();

        // ver si esta duplicado el rut
        if (teacherRepository.existsByRut(rut)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un docente con ese RUT");
        }

        // ver si esta duplicado el email
        if (teacherRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un docente con ese correo");
        }

        // create teacher
        TeacherEntity newTeacher = new TeacherEntity();
        newTeacher.setRut(rut);
        newTeacher.setFirstName(teacher.getFirstName());
        newTeacher.setLastName(teacher.getLastName());
        newTeacher.setEmail(email);
        newTeacher.setProfessionalTitle(teacher.getProfessionalTitle());
        newTeacher.setAcademicDegree(teacher.getAcademicDegree());
        newTeacher.setStatus(Status.ACTIVA);

        TeacherEntity saved = teacherRepository.save(newTeacher);
        return new TeacherResponseDTO(saved);
    }


    // update teacher
    @Transactional 
    public TeacherResponseDTO updateTeacher(Long id, TeacherRequestDTO teacher) {

        TeacherEntity current = findTeacherById(id);

        String rut = teacher.getRut().trim();
        String email = teacher.getEmail().trim();

        // ver si esta duplicado el rut
        teacherRepository.findByRut(rut).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un docente con ese RUT");}
        });

        // check duplicate email
        teacherRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un docente con ese correo");}
        });

        // actualizar
        current.setRut(rut);
        current.setFirstName(teacher.getFirstName());
        current.setLastName(teacher.getLastName());
        current.setEmail(email);
        current.setProfessionalTitle(teacher.getProfessionalTitle());
        current.setAcademicDegree(teacher.getAcademicDegree());

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

}