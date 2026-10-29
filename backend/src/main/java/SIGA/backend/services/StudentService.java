package SIGA.backend.services;

import SIGA.backend.entities.CareerEntity;
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

import lombok.RequiredArgsConstructor;

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

        // verificar que la carrera existe
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

        // verificar que el estudiante existe
        findStudentById(id);

        // obtener las inscripciones del estudiante
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudentId(id);

        List<StudentAcademicHistoryDTO> history = new ArrayList<>();

        // recorrer las inscripciones
        for (EnrollmentEntity enrollment : enrollments) {

            // buscar la calificacion de cada inscripcion
            GradeEntity grade = gradeRepository.findByEnrollmentId(enrollment.getId()).orElse(null);

            // agregar la informacion al historial
            history.add(new StudentAcademicHistoryDTO(enrollment, grade));
        }

        return history;
    }



    // encontrar el plan activo
    // auxiliar para crear un estudiante
    private StudyPlanEntity findCurrentStudyPlan(Long careerId) {
        return studyPlanRepository.findByCareerIdAndStatus(careerId, StudyPlanStatus.VIGENTE)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "La carrera no tiene un plan de estudio vigente"));
    }


    // ver si tiene informacion academica
    private boolean hasAcademicInformation(Long studentId) {
        return enrollmentRepository.existsByStudentId(studentId) ||
                periodMatriculationRepository.existsByStudentId(studentId);
    }


    // create a student
    @Transactional 
    public StudentResponseDTO createStudent(StudentRequestDTO student) {

        String rut = student.getRut().trim();
        String email = student.getEmail().trim();

        // ver si ya existe el rut
        if(studentRepository.existsByRut(rut)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un estudiante con ese RUT");
        }

        // ver si ya existe el email
        if (studentRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un estudiante con ese correo");
        }

        // encontrar la carrera
        CareerEntity career = careerRepository.findById(student.getCareerId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrera no encontrada"));

        // validate active career
        if (career.getStatus() != Status.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pueden registrar estudiantes en una carrera inactiva");
        }

        // asignar el plan de estudio vigente
        StudyPlanEntity studyPlan = findCurrentStudyPlan(career.getId());

        // create student
        StudentEntity newStudent = new StudentEntity();
        newStudent.setRut(rut);
        newStudent.setFirstName(student.getFirstName().trim());
        newStudent.setLastName(student.getLastName().trim());
        newStudent.setEmail(email);
        newStudent.setStatus(StudentStatus.REGULAR);
        newStudent.setCareer(career);
        newStudent.setStudyPlan(studyPlan);

        StudentEntity saved  = studentRepository.save(newStudent);
        return new StudentResponseDTO(saved);
    }


    // update student
    @Transactional 
    public StudentResponseDTO updateStudent(Long id, StudentRequestDTO student) {

        StudentEntity current = findStudentById(id);

        String rut = student.getRut().trim();
        String email = student.getEmail().trim();
        Long careerId = student.getCareerId();

        // ver si ya existe el rut 
        studentRepository.findByRut(rut).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un estudiante con ese RUT");
            }
        });

        // ver si ya existe el correo
        studentRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un estudiante con ese correo");
            }
        });

        // encontrar la carrera
        CareerEntity career = careerRepository.findById(careerId).orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrera no encontrada"));

        // confirmar que se ingreso un cambio de carrera
        boolean careerChanged = !current.getCareer().getId().equals(careerId);

        // validar que la carrera esta activa
        if (careerChanged && career.getStatus() != Status.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cambiar a un acarrera inactiva");
        }

        // buscar el nuevo plan de estudio 
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

        // validar el nuevo plan 
        if (!studyPlan.getCareer().getId().equals(careerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EL plan de estudio no corresponde a la carrera");
        }

        // confirmar cambio de plan 
        boolean studyPlanChanged = !current.getStudyPlan().getId().equals(studyPlan.getId());

        // no permitie que se cambie la informacion 
        if ((careerChanged || studyPlanChanged) && hasAcademicInformation(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cambiar debido a que ya cuenta con historial academico");
        }

        // actualizar la informacion del estudiante
        current.setRut(rut);
        current.setFirstName(student.getFirstName().trim());
        current.setLastName(student.getLastName().trim());
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

        // validar el estado
        if (status == null || status == StudentStatus.EGRESADO || status == StudentStatus.ELIMINADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No es posible cambiar el estado manualmente");
        }

        // estados irreversibles
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

        // ver si tiene informacion academica
        if (hasAcademicInformation(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puedde eliminar un estudiante con informacion academica");
        }

        studentRepository.delete(student);
    }

    // link student with Keycloak
    @Transactional
    public StudentResponseDTO linkStudentKeycloak(Long id, String keycloakSubject) {

        StudentEntity student = findStudentById(id);

        if (keycloakSubject == null || keycloakSubject.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador de Keycloak es obligatorio");
        }

        String subject = keycloakSubject.trim();

        studentRepository.findByKeycloakSubject(subject).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La cuenta de Keycloak ya esta asociada a otro estudiante");
            }
        });

        if (student.getKeycloakSubject() != null
                && !student.getKeycloakSubject().equals(subject)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El estudiante ya tiene una cuenta de Keycloak asociada");
        }

        // verificar que la cuenta no pertenezca a un docente
        if (teacherRepository.findByKeycloakSubject(subject).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La cuenta de Keycloak ya esta asociada a un docente");
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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un estudiante asociado a esta cuenta"));

        return new StudentResponseDTO(student);
    }

    // get authenticated student academic history
    @Transactional(readOnly = true)
    public List<StudentAcademicHistoryDTO> getCurrentStudentAcademicHistory(String keycloakSubject) {

        StudentEntity student = studentRepository
                .findByKeycloakSubject(keycloakSubject)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un estudiante asociado a esta cuenta"));

        return getStudentAcademicHistory(student.getId());
    }
}
