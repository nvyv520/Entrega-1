package SIGA.backend.services;

import SIGA.backend.entities.EnrollmentEntity;
import SIGA.backend.entities.EnrollmentStatus;
import SIGA.backend.entities.StudentEntity;
import SIGA.backend.entities.StudentStatus;
import SIGA.backend.entities.SectionEntity;
import SIGA.backend.entities.SubjectEntity;
import SIGA.backend.entities.AcademicPeriodStatus;
import SIGA.backend.entities.Status;
import SIGA.backend.entities.GradeResult;

import SIGA.backend.repositories.EnrollmentRepository;
import SIGA.backend.repositories.StudentRepository;
import SIGA.backend.repositories.SectionRepository;
import SIGA.backend.repositories.PeriodMatriculationRepository;
import SIGA.backend.repositories.GradeRepository;

import SIGA.backend.dtos.EnrollmentRequestDTO;
import SIGA.backend.dtos.EnrollmentResponseDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;


@Service 
@RequiredArgsConstructor 
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;
    private final PeriodMatriculationRepository matriculationRepository;
    private final GradeRepository gradeRepository;
    private final EntityManager entityManager;

    // get all enrollments 
    @Transactional (readOnly = true)
    public List<EnrollmentResponseDTO> getAllEnrollments() {
        List<EnrollmentEntity> enrollments = enrollmentRepository.findAll();
        return enrollments.stream().map(EnrollmentResponseDTO::new).toList();
    }


    // find enrollment by id
    private EnrollmentEntity findEnrollmentById(Long id) {
        return enrollmentRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscripcion no encontrada"));
    }

    // get enrollment by id
    @Transactional(readOnly = true)
    public EnrollmentResponseDTO getEnrollmentById(Long id) {
        EnrollmentEntity enrollment = findEnrollmentById(id);
        return new EnrollmentResponseDTO(enrollment);
    }


    // get enrollments by student
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByStudent(Long studentId) {
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudentId(studentId);
        return enrollments.stream().map(EnrollmentResponseDTO::new).toList();
    }


    // get enrollments by section
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsBySection(Long sectionId) {
        List<EnrollmentEntity> enrollments = enrollmentRepository.findBySectionId(sectionId);
        return enrollments.stream().map(EnrollmentResponseDTO::new).toList();
    }


    // get enrollments by academic period
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByAcademicPeriod(Long periodId) {
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByAcademicPeriodId(periodId);
        return enrollments.stream().map(EnrollmentResponseDTO::new).toList();
    }


    // get enrollments by status
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByStatus(EnrollmentStatus status) {
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStatus(status);
        return enrollments.stream().map(EnrollmentResponseDTO::new).toList();
    }


    // find student by id
    private StudentEntity findStudentById(Long id) {
        return studentRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));
    }


    // find and lock section for capacity changes
    private SectionEntity findLockedSectionById(Long id) {
        SectionEntity section = entityManager.find(SectionEntity.class, id, LockModeType.PESSIMISTIC_WRITE);

        if (section == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Seccion no encontrada");
        }

        return section;
    }
    

    //validar condiciones de inscripcion 
    private void validateEnrollment(StudentEntity student, SectionEntity section) {
        
        // el estudiante debe estar habilitado
        if (student.getStatus() != StudentStatus.REGULAR) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante no se encuentra habilitado");
        }

        // el periodo debe estar abierto 
        if (section.getAcademicPeriod().getStatus() != AcademicPeriodStatus.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El periodo debe estar abierto");
        }

        // la seccion debe estar activa
        if(section.getStatus() != Status.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La seccion debe estar activa");
        }

        // revisar que el estudiante aun este matriculado en el periodo correspondiente
        if (!matriculationRepository.existsByStudentIdAndAcademicPeriodId(student.getId(), section.getAcademicPeriod().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante no esta matriculado en el periodo");
        }

        // revisar que la asignatura pertenece a su plan de estudio 
        if (!student.getStudyPlan().getId().equals(section.getSubject().getStudyPlan().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La asignatura no pertenece al plan de estudio del estudiante");
        }
    }


    // validar que los prerequisitos esten aprobados
    private void validatePrerequisites(StudentEntity student, SubjectEntity subject) {
        for (SubjectEntity prerequisite : subject.getPrerequisites()) {
            boolean approved = gradeRepository.existsByEnrollment_Student_IdAndEnrollment_Section_Subject_IdAndResult (
                                student.getId(), prerequisite.getId(), GradeResult.APROBADO);
                            
            if (!approved) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante no cumple con los prerequisitos de la asignatura");
            }
        }
    }


    // validar el horario del estudiante para no tener topes
    private void validateStudentSchedule(StudentEntity student, SectionEntity section) {

        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudentIdAndAcademicPeriodId(student.getId(), section.getAcademicPeriod().getId());
        Set<String> blocks = section.getBlocks();

        for (EnrollmentEntity enrollment : enrollments) {

            if (enrollment.getStatus() != EnrollmentStatus.ACTIVO) {
                continue;
            }

            if (enrollment.getSection().getId().equals(section.getId())) {
                continue;
            }

            for (String block : blocks) {

                if (enrollment.getSection().getBlocks().contains(block)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante tiene un tope horario");
                }

            }
        }

    }


    // validar la capacidad disponible 
    private void validateCapacity(SectionEntity section) {
        Long enrolled = enrollmentRepository.countBySectionIdAndStatus(section.getId(), EnrollmentStatus.ACTIVO);

        if (enrolled >= section.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La seccion no tiene cupos disponibles");
        }
    }


    // create enrollment
    @Transactional 
    public EnrollmentResponseDTO createEnrollment (EnrollmentRequestDTO enrollment) {

        // revisar que se ingresa la informacion completa
        if (enrollment == null || enrollment.getStudentId() == null || enrollment.getSectionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, " El estudiante y seccion son obligatorios");
        }

        StudentEntity student = findStudentById(enrollment.getStudentId());

        // cerrar la seccion antes de cambiar algo 
        SectionEntity section = findLockedSectionById(enrollment.getSectionId());

        // validar la inscripcion
        validateEnrollment(student, section);

        // ver que el estudiante aun no este inscrito 
        EnrollmentEntity existing = enrollmentRepository.findByStudentIdAndSectionId(student.getId(), section.getId()).orElse(null);

        if (existing != null && existing.getStatus() == EnrollmentStatus.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante ya se encuentra inscrito en la seccion");
        }

        // no reactivar alumnos que ya estuvieron inscritos y tienen nota
        if (existing != null && gradeRepository.existsByEnrollmentId(existing.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede resctivar una inscripcion con calificacion");
        }

        // validar prerequisitos, topes horarios y capacidad
        validatePrerequisites(student, section.getSubject());
        validateStudentSchedule(student, section);
        validateCapacity(section);

        EnrollmentEntity saved;

        if (existing != null) {
            existing.setStatus(EnrollmentStatus.ACTIVO);
            existing.setEnrollmentDate(LocalDate.now());
            saved = enrollmentRepository.save(existing);
        }
        else {
            EnrollmentEntity newEnrollment = new EnrollmentEntity();
            newEnrollment.setStudent(student);
            newEnrollment.setSection(section);
            newEnrollment.setAcademicPeriod(section.getAcademicPeriod());
            newEnrollment.setEnrollmentDate(LocalDate.now());
            newEnrollment.setStatus(EnrollmentStatus.ACTIVO);

            saved = enrollmentRepository.save(newEnrollment);
        }

        // actualizar la cantidad de estudiantes inscritos
        enrollmentRepository.flush();
        Long enrolled = enrollmentRepository.countBySectionIdAndStatus(section.getId(), EnrollmentStatus.ACTIVO);
        section.setEnrolledStudents(Math.toIntExact(enrolled));
        sectionRepository.save(section);

        return new EnrollmentResponseDTO(saved);


    }

    // cancel enrollment
    @Transactional 
    public EnrollmentResponseDTO cancelEnrollment(Long id) {

        EnrollmentEntity enrollment = findEnrollmentById(id);

        // cerrar seccion para evitar cambios sobrepuestos
        SectionEntity section = findLockedSectionById(enrollment.getSection().getId());

        // volver a cargar la inscripcion
        entityManager.refresh(enrollment);

        // validar que el periodo este abierto 
        if (section.getAcademicPeriod().getStatus() != AcademicPeriodStatus.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cambiar, el periodo se encuentra cerrado");
        }

        // ver que la inscripcion esta activa
        if (enrollment.getStatus() == EnrollmentStatus.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La inscripcion ya se encuentra cancelada");
        }

        // no cambiar si ya tiene nota
        if (gradeRepository.existsByEnrollmentId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cancelar ya que cuenat con calificacion");
        }

        // cancelar la inscripcion
        enrollment.setStatus(EnrollmentStatus.CANCELADO);
        EnrollmentEntity saved =  enrollmentRepository.save(enrollment);

        // actualizar la cantidad de alumnos inscritos 
        enrollmentRepository.flush();
        Long enrolled = enrollmentRepository.countBySectionIdAndStatus(section.getId(), EnrollmentStatus.ACTIVO);
        section.setEnrolledStudents(Math.toIntExact(enrolled));
        sectionRepository.save(section);

        return new EnrollmentResponseDTO(saved);
    }
}