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
import SIGA.backend.entities.AcademicPeriodEntity;
import SIGA.backend.entities.PeriodMatriculationEntity;

import SIGA.backend.repositories.EnrollmentRepository;
import SIGA.backend.repositories.StudentRepository;
import SIGA.backend.repositories.SectionRepository;
import SIGA.backend.repositories.PeriodMatriculationRepository;
import SIGA.backend.repositories.GradeRepository;
import SIGA.backend.repositories.SubjectRepository;

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
    private final SubjectRepository subjectRepository;


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


    // find and lock an open academic period
    private AcademicPeriodEntity lockOpenPeriod(Long periodId) {

        AcademicPeriodEntity period = entityManager.find(AcademicPeriodEntity.class, periodId, LockModeType.PESSIMISTIC_WRITE);

        if (period == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo academico no encontrado");
        }

        if (period.getStatus() != AcademicPeriodStatus.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El periodo academico debe estar abierto");
        }

        return period;
    }


    // find and lock student
    private StudentEntity lockStudent(Long studentId) {

        StudentEntity student = entityManager.find(StudentEntity.class, studentId, LockModeType.PESSIMISTIC_WRITE);

        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado");
        }

        return student;
    }


    // find student matriculation in period
    private PeriodMatriculationEntity findMatriculation(Long studentId, Long periodId) {
        return matriculationRepository.findByStudentIdAndAcademicPeriodId(studentId, periodId).orElseThrow(() -> 
                new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante no esta matriculado en el periodo"));
    }


    // prevent changes to confirmed selections
    private void validateSelectionNotConfirmed(Long studentId, Long periodId) {

        PeriodMatriculationEntity matriculation = findMatriculation(studentId, periodId);

        if (Boolean.TRUE.equals(matriculation.getEnrollmentConfirmed())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La seleccion ya fue confirmada y no se puede modificar");
        }
    }


    // find and lock section for capacity changes
    private SectionEntity findLockedSectionById(Long id) {
        SectionEntity section = entityManager.find(SectionEntity.class, id, LockModeType.PESSIMISTIC_WRITE);

        if (section == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Seccion no encontrada");
        }

        return section;
    }
    

    private void validateStudentStatus(StudentEntity student) {
        if (student.getStatus() != StudentStatus.REGULAR) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante no se encuentra habilitado");
        }
    }


    //validar condiciones de inscripcion
    private void validateEnrollment(StudentEntity student, SectionEntity section) {

        validateStudentStatus(student);

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


    // revisar si el estudiante aprobo una asignatura
    private boolean hasApprovedSubject(Long studentId, Long subjectId) {
        return gradeRepository.existsByEnrollment_Student_IdAndEnrollment_Section_Subject_IdAndResult(
                        studentId, subjectId, GradeResult.APROBADO);
    }


    // validar que la asignatura no este aprobada
    private void validateSubjectNotApproved(StudentEntity student, SubjectEntity subject) {

        boolean approved = gradeRepository.existsByEnrollment_Student_IdAndEnrollment_Section_Subject_IdAndResult(
                        student.getId(), subject.getId(), GradeResult.APROBADO);

        if (approved) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante ya aprobo esta asignatura");
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
        validateCapacity(section, false);
    }

    private void validateCapacity(SectionEntity section, boolean alreadyEnrolled) {
        Long enrolled = enrollmentRepository.countBySectionIdAndStatus(section.getId(), EnrollmentStatus.ACTIVO);

        if (enrolled + (alreadyEnrolled ? 0 : 1) > section.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La seccion no tiene cupos disponibles");
        }
    }


    // validar maximo de asignaturas por periodo
    private void validateMaximumSubjects(StudentEntity student, SectionEntity section) {
        validateMaximumSubjects(student, section, false);
    }

    private void validateMaximumSubjects(StudentEntity student, SectionEntity section, boolean alreadyEnrolled) {

        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudentIdAndAcademicPeriodId(student.getId(), section.getAcademicPeriod().getId());

        long total = enrollments.stream().filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVO).count();

        if (total + (alreadyEnrolled ? 0 : 1) > 6) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante no puede inscribir mas de 6 asignaturas por periodo");
        }
    }


    // validar minimo de asignaturas por periodo
    private void validateMinimumSubjects(StudentEntity student, Long periodId) {

        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudentIdAndAcademicPeriodId(student.getId(), periodId);

        Long total = enrollments.stream().filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVO).count();

        List<SubjectEntity> subjects = subjectRepository.findByStudyPlanId(student.getStudyPlan().getId());

        Long pending = subjects.stream().filter(subject -> !hasApprovedSubject(student.getId(), subject.getId())).count();

        if (pending == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante no tiene asignaturas pendientes para inscribir");
        }

        Long minimum = Math.min(3, pending);

        if (total < minimum) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante no cumple con el minimo de asignaturas del periodo");
        }
    }


    // validar prioridad de asignaturas de semestres inferiores
    private void validateSemesterPriority(StudentEntity student, SectionEntity section) {

        Long studentId = student.getId();
        Long periodId = section.getAcademicPeriod().getId();
        Integer semester = section.getSubject().getSemester();

        // obtener asignaturas del plan del estudiante
        List<SubjectEntity> subjects = subjectRepository.findByStudyPlanId(student.getStudyPlan().getId());

        // obtener inscripciones del estudiante en el periodo
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudentIdAndAcademicPeriodId(studentId, periodId);

        // obtener secciones disponibles en el periodo
        List<SectionEntity> sections = sectionRepository.findByAcademicPeriodId(periodId);

        for (SubjectEntity subject : subjects) {

            // revisar solo semestres inferiores
            if (subject.getSemester() >= semester) {
                continue;
            }

            // ignorar asignaturas aprobadas
            if (hasApprovedSubject(studentId, subject.getId())) {
                continue;
            }

            // revisar si ya esta inscrita
            boolean enrolled = enrollments.stream().anyMatch(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVO &&
                                enrollment.getSection().getSubject().getId().equals(subject.getId()));

            if (enrolled) {
                continue;
            }

            // revisar si cumple los prerequisitos
            boolean prerequisitesApproved = true;

            for (SubjectEntity prerequisite : subject.getPrerequisites()) {
                if (!hasApprovedSubject(studentId, prerequisite.getId())) {
                    prerequisitesApproved = false;
                    break;
                }
            }

            if (!prerequisitesApproved) {
                continue;
            }

            // revisar si existe una seccion activa con cupos
            boolean available = false;

            for (SectionEntity current : sections) {

                if (!current.getSubject().getId().equals(subject.getId()) || current.getStatus() != Status.ACTIVA) {
                    continue;
                }

                Long enrolledStudents = enrollmentRepository.countBySectionIdAndStatus(current.getId(), EnrollmentStatus.ACTIVO);

                // ignore full sections
                if (enrolledStudents >= current.getCapacity()) {
                    continue;
                }

                // check if this section conflicts with the student's schedule
                boolean scheduleConflict = enrollments.stream().filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVO)
                        .anyMatch(enrollment -> enrollment.getSection().getBlocks().stream().anyMatch(current.getBlocks()::contains));

                if (!scheduleConflict) {
                    available = true;
                    break;
                }

            }

            if (available) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Debe priorizar las asignaturas pendientes de semestres inferiores");
            }
        }
    }


    // create enrollment
    @Transactional 
    public EnrollmentResponseDTO createEnrollment (EnrollmentRequestDTO enrollment) {

        // revisar que se ingresa la informacion completa
        if (enrollment == null || enrollment.getStudentId() == null || enrollment.getSectionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, " El estudiante y seccion son obligatorios");
        }
        
        // find section to obtain academic period
        SectionEntity requestedSection = sectionRepository.findById(enrollment.getSectionId()).orElseThrow(() -> 
                                            new ResponseStatusException(HttpStatus.NOT_FOUND, "Seccion no encontrada"));

        Long periodId = requestedSection.getAcademicPeriod().getId();

        // lock period, student and section in this order
        lockOpenPeriod(periodId);

        StudentEntity student = lockStudent(enrollment.getStudentId());

        SectionEntity section = findLockedSectionById(enrollment.getSectionId());

        if (!section.getAcademicPeriod().getId().equals(periodId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La seccion cambio de periodo");
        }

        // do not modify a confirmed selection
        validateSelectionNotConfirmed(student.getId(), periodId);


        // revisar que no tenga la misma asignatura inscrita en otra seccion
        List<EnrollmentEntity> studentEnrollments = enrollmentRepository.findByStudentIdAndAcademicPeriodId(student.getId(), periodId);

        for (EnrollmentEntity current : studentEnrollments) {

            if (current.getStatus() == EnrollmentStatus.ACTIVO &&
                current.getSection().getSubject().getId().equals(section.getSubject().getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante ya tiene inscrita esta asignatura en el periodo");
            }
        }

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

        // validar que la asignatura no este aprobada
        validateSubjectNotApproved(student, section.getSubject());

        // validar los prerequisitos, topes de horario, capacidad, cantidad de asignaturas y prioridad
        validatePrerequisites(student, section.getSubject());
        validateStudentSchedule(student, section);
        validateCapacity(section);
        validateMaximumSubjects(student, section);
        validateSemesterPriority(student, section);

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

        Long periodId = enrollment.getAcademicPeriod().getId();
        Long studentId = enrollment.getStudent().getId();
        Long sectionId = enrollment.getSection().getId();

        // lock in the same order as createEnrollment
        lockOpenPeriod(periodId);
        lockStudent(studentId);

        SectionEntity section = findLockedSectionById(sectionId);

        // reload enrollment after acquiring locks
        entityManager.refresh(enrollment);

        // prevent cancelling a confirmed selection
        validateSelectionNotConfirmed(studentId, periodId);

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


    // confirm subject selection
    @Transactional
    public void confirmEnrollmentSelection(Long studentId, Long periodId) {
        confirmEnrollmentSelection(studentId, periodId, true);
    }
    
    
    // confirm subject selection
    private void confirmEnrollmentSelection(Long studentId, Long periodId, boolean enforceMinimum) {

        // lock academic period and student
        lockOpenPeriod(periodId);
        StudentEntity student = lockStudent(studentId);

        // validate student status
        validateStudentStatus(student);

        // find matriculation
        PeriodMatriculationEntity matriculation = findMatriculation(studentId, periodId);

        // prevent confirming twice
        if (Boolean.TRUE.equals(matriculation.getEnrollmentConfirmed())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La seleccion ya esta confirmada");
        }

        // get student enrollments
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudentIdAndAcademicPeriodId(studentId, periodId);

        List<EnrollmentEntity> activeEnrollments = enrollments.stream().filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVO).toList();

        // prevent confirming an empty selection
        if (activeEnrollments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante debe tener asignaturas inscritas para confirmar");
        }

        // validate current enrollments
        for (EnrollmentEntity enrollment : activeEnrollments) {

            SectionEntity section = findLockedSectionById(enrollment.getSection().getId());

            validateEnrollment(student, section);
            validateSubjectNotApproved(student, section.getSubject());
            validatePrerequisites(student, section.getSubject());
            validateStudentSchedule(student, section);
            validateCapacity(section, true);
            validateMaximumSubjects(student, section, true);
            validateSemesterPriority(student, section);
        }

        // validate minimum subjects when required
        if (enforceMinimum) {
            validateMinimumSubjects(student, periodId);
        }

        // confirm selection
        matriculation.setEnrollmentConfirmed(true);
        matriculationRepository.save(matriculation);
    }


    // the personal flow deliberately defers the minimum-subject requirement
    @Transactional
    public void confirmCurrentStudentEnrollmentSelection(String keycloakSubject, Long periodId) {
        StudentEntity student = studentRepository.findByKeycloakSubject(keycloakSubject).orElseThrow(() -> 
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un estudiante asociado a esta cuenta"));

        confirmEnrollmentSelection(student.getId(), periodId, true);
    }


    // get enrollments of authenticated student
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getCurrentStudentEnrollments(String keycloakSubject) {

        StudentEntity student = studentRepository.findByKeycloakSubject(keycloakSubject).orElseThrow(() -> 
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un estudiante asociado a esta cuenta"));

        return getEnrollmentsByStudent(student.getId());
    }


    // create enrollment for authenticated student
    @Transactional
    public EnrollmentResponseDTO createCurrentStudentEnrollment(String keycloakSubject, Long sectionId) {

        StudentEntity student = studentRepository.findByKeycloakSubject(keycloakSubject).orElseThrow(() -> 
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un estudiante asociado a esta cuenta"));

        if (sectionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La seccion es obligatoria");
        }

        EnrollmentRequestDTO request = new EnrollmentRequestDTO();
        request.setStudentId(student.getId());
        request.setSectionId(sectionId);

        return createEnrollment(request);
    }


    // cancel enrollment for authenticated student
    @Transactional
    public EnrollmentResponseDTO cancelCurrentStudentEnrollment(String keycloakSubject, Long enrollmentId) {

        StudentEntity student = studentRepository.findByKeycloakSubject(keycloakSubject).orElseThrow(() -> 
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un estudiante asociado a esta cuenta"));

        EnrollmentEntity enrollment = findEnrollmentById(enrollmentId);

        if (!enrollment.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos para cancelar esta inscripcion");
        }

        return cancelEnrollment(enrollmentId);
    }


}
