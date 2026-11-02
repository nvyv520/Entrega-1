package SIGA.backend.services;

import SIGA.backend.entities.GradeEntity;
import SIGA.backend.entities.GradeResult;
import SIGA.backend.entities.EnrollmentEntity;
import SIGA.backend.entities.EnrollmentStatus;
import SIGA.backend.entities.AcademicPeriodStatus;
import SIGA.backend.entities.AcademicPeriodEntity;
import SIGA.backend.entities.TeacherEntity;
import SIGA.backend.entities.StudentEntity;
import SIGA.backend.entities.Status;

import SIGA.backend.repositories.GradeRepository;
import SIGA.backend.repositories.EnrollmentRepository;
import SIGA.backend.repositories.StudentRepository;
import SIGA.backend.repositories.TeacherRepository;

import SIGA.backend.dtos.GradeRequestDTO;
import SIGA.backend.dtos.GradeResponseDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor

public class GradeService {

    private final GradeRepository gradeRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EntityManager entityManager;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SectionService sectionService;

    // get all grades
    @Transactional (readOnly = true)
    public List<GradeResponseDTO> getAllGrades() {
        List<GradeEntity> grades =  gradeRepository.findAll();
        return grades.stream().map(GradeResponseDTO::new).toList();
    }


    // find grade by id
    private GradeEntity findGradeById(Long id) {
        return gradeRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Calificacion no encontrada"));
    }


    // get grade by id
    @Transactional (readOnly = true)
    public GradeResponseDTO getGradeById(Long id) {
        GradeEntity grade = findGradeById(id);
        return new GradeResponseDTO(grade);
    }


    // get grade by enrollment
    @Transactional (readOnly = true)
    public GradeResponseDTO getGradeByEnrollment(Long enrollmentId) {
        GradeEntity grade = gradeRepository.findByEnrollmentId(enrollmentId).orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.NOT_FOUND, "Calificacion no encontrada"));
        return new GradeResponseDTO(grade);
    }


    // get grades by result
    @Transactional (readOnly = true)
    public List<GradeResponseDTO> getGradesByResult(GradeResult result) {
        List<GradeEntity> grades = gradeRepository.findByResult(result);
        return grades.stream().map(GradeResponseDTO::new).toList();
    }


    //get grades by student
    @Transactional (readOnly = true)
    public List<GradeResponseDTO> getGradesByStudent(Long studentId) {
        List<GradeEntity> grades = gradeRepository.findByEnrollment_Student_Id(studentId);
        return grades.stream().map(GradeResponseDTO::new).toList();
    }


    // get grades by acadmeic period
    @Transactional (readOnly = true)
    public List<GradeResponseDTO> getGradesByAcademicPeriod(Long periodId) {
        List<GradeEntity> grades = gradeRepository.findByEnrollment_AcademicPeriod_Id(periodId);
        return grades.stream().map(GradeResponseDTO::new).toList();
    }


    // find enrollment by id
    private EnrollmentEntity findEnrollmentById(Long id) {
        return enrollmentRepository.findById(id).orElseThrow(() -> 
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscripcion no encontrada"));
    }


    // validate grade data
    private void validateGradeData(GradeRequestDTO grade) {

        // validate enrollment
        if (grade == null || grade.getEnrollmentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La inscripcion es obligatoria");
        }

        // validate final grade
        BigDecimal finalGrade = grade.getFinalGrade();

        // si la nota no esta en el rango 
        if (finalGrade == null || 
            finalGrade.compareTo(new BigDecimal("1.0")) < 0 ||
            finalGrade.compareTo(new BigDecimal("7.0")) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nota debe estar entre 1.0 y 7.0");
        }

        // validar la cantidad de decimales
        if (finalGrade.stripTrailingZeros().scale() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nota debe tener solo un decimal");
        }
    }


    // validate enrollment
    private void validateEnrollment(EnrollmentEntity enrollment) {

        // validate enrollment status
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La inscripcion debe estar activa");
        }

        // revisar el estado del periodo academico
        if (enrollment.getAcademicPeriod().getStatus() != AcademicPeriodStatus.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El periodo academico debe estar abierto");
        }
        if (enrollment.getSection().getStatus() != Status.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La seccion debe estar activa para registrar o modificar calificaciones");
        }
    }


    // validate authenticated user permissions
    private void validateGradeAccess(EnrollmentEntity enrollment) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        // administrators can manage grades
        if (authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            return;
        }

        // only teachers can register or update grades
        if (authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_TEACHER"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos para modificar calificaciones");
        }

        String keycloakSubject = jwtAuth.getToken().getSubject();

        TeacherEntity teacher = teacherRepository.findByKeycloakSubject(keycloakSubject).orElseThrow(() -> 
                                new ResponseStatusException(HttpStatus.FORBIDDEN, "No existe un docente asociado a esta cuenta"));

        Long assignedTeacherId = enrollment.getSection().getTeacher().getId();

        if (!teacher.getId().equals(assignedTeacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El docente no esta asignado a esta seccion");
        }
    }


    // calculate grade result
    private GradeResult calculateResult(BigDecimal finalGrade) {
        if (finalGrade.compareTo(new BigDecimal("4.0")) >= 0) {
            return GradeResult.APROBADO;
        }

        return GradeResult.REPROBADO;
    }


    // lock academic period before modifying grades
    private void lockOpenPeriod(Long periodId) {

        AcademicPeriodEntity period = entityManager.find(AcademicPeriodEntity.class, periodId, LockModeType.PESSIMISTIC_WRITE);

        if (period == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo academico no encontrado");
        }

        if (period.getStatus() != AcademicPeriodStatus.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El periodo academico esta cerrado");
        }
    }



    // create grade 
    @Transactional 
    public GradeResponseDTO createGrade(GradeRequestDTO grade) {

        // validate grade information
        validateGradeData(grade);

        EnrollmentEntity enrollment = findEnrollmentById(grade.getEnrollmentId());
        validateGradeAccess(enrollment);
        lockOpenPeriod(enrollment.getAcademicPeriod().getId());
        entityManager.refresh(enrollment);

        // validate enrollment and academic period
        validateEnrollment(enrollment);

        // check duplicate grade
        if (gradeRepository.existsByEnrollmentId(enrollment.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La inscripcion ya cuenta con una calificacion");
        }

        GradeResult result = calculateResult(grade.getFinalGrade());

        GradeEntity newGrade = new GradeEntity();
        newGrade.setEnrollment(enrollment);
        newGrade.setFinalGrade(grade.getFinalGrade());
        newGrade.setResult(result);

        GradeEntity saved = gradeRepository.save(newGrade);
        return new GradeResponseDTO(saved);
    }


    // update grade
    @Transactional 
    public GradeResponseDTO updateGrade(Long id, GradeRequestDTO grade) {

        GradeEntity current = findGradeById(id);
        validateGradeAccess(current.getEnrollment());
        lockOpenPeriod(current.getEnrollment().getAcademicPeriod().getId());
        entityManager.refresh(current);

        // validate grade information
        validateGradeData(grade);

        // prevent changing enrollment
        if(!current.getEnrollment().getId().equals(grade.getEnrollmentId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cambiar la inscripcion de una calificacion");
        }

        // validate enrollment and academic period
        validateEnrollment(current.getEnrollment());

        // calculate result
        GradeResult result = calculateResult(grade.getFinalGrade());

        current.setFinalGrade(grade.getFinalGrade());
        current.setResult(result);

        GradeEntity saved = gradeRepository.save(current);
        return new GradeResponseDTO(saved);
    }


    // get grades of authenticated student
    @Transactional(readOnly = true)
    public List<GradeResponseDTO> getCurrentStudentGrades(String keycloakSubject) {

        StudentEntity student = studentRepository.findByKeycloakSubject(keycloakSubject).orElseThrow(() -> 
                                new ResponseStatusException(HttpStatus.NOT_FOUND,  "No existe un estudiante asociado a esta cuenta"));

        return getGradesByStudent(student.getId());
    }

    // get grades only from a section assigned to the authenticated teacher
    @Transactional(readOnly = true)
    public List<GradeResponseDTO> getCurrentTeacherSectionGrades(String keycloakSubject, Long sectionId) {

        sectionService.findCurrentTeacherSection(keycloakSubject, sectionId);
        return gradeRepository.findByEnrollment_Section_IdAndEnrollment_Status(sectionId, EnrollmentStatus.ACTIVO)
                .stream().map(GradeResponseDTO::new).toList();
    }

}
