package SIGA.backend.services;

import java.time.LocalDate;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import SIGA.backend.dtos.EnrollmentRequestDTO;
import SIGA.backend.dtos.EnrollmentResponseDTO;
import SIGA.backend.dtos.SectionResponseDTO;
import SIGA.backend.entities.AcademicPeriodEntity;
import SIGA.backend.entities.AcademicPeriodStatus;
import SIGA.backend.entities.EnrollmentEntity;
import SIGA.backend.entities.EnrollmentStatus;
import SIGA.backend.entities.GradeResult;
import SIGA.backend.entities.PeriodMatriculationEntity;
import SIGA.backend.entities.SectionEntity;
import SIGA.backend.entities.Status;
import SIGA.backend.entities.StudentEntity;
import SIGA.backend.entities.StudentStatus;
import SIGA.backend.entities.SubjectEntity;
import SIGA.backend.repositories.EnrollmentRepository;
import SIGA.backend.repositories.GradeRepository;
import SIGA.backend.repositories.PeriodMatriculationRepository;
import SIGA.backend.repositories.SectionRepository;
import SIGA.backend.repositories.StudentRepository;
import SIGA.backend.repositories.SubjectRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;


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

    // Keeps provisional reservations visible with their confirmation status.
    private EnrollmentResponseDTO enrollmentResponse(EnrollmentEntity enrollment) {
        EnrollmentResponseDTO response = new EnrollmentResponseDTO(enrollment);
        response.setConfirmed(enrollmentRepository.isSelectionConfirmed(
                enrollment.getStudent().getId(), enrollment.getAcademicPeriod().getId()));
        return response;
    }


    // get all enrollments 
    @Transactional (readOnly = true)
    public List<EnrollmentResponseDTO> getAllEnrollments() {
        List<EnrollmentEntity> enrollments = enrollmentRepository.findAll();
        return enrollments.stream().map(this::enrollmentResponse).toList();
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
        return enrollmentResponse(enrollment);
    }


    // get enrollments by student
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByStudent(Long studentId) {
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudentId(studentId);
        return enrollments.stream().map(this::enrollmentResponse).toList();
    }


    // get enrollments by section
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsBySection(Long sectionId) {
        List<EnrollmentEntity> enrollments = enrollmentRepository.findBySectionId(sectionId);
        return enrollments.stream().map(this::enrollmentResponse).toList();
    }


    // get enrollments by academic period
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByAcademicPeriod(Long periodId) {
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByAcademicPeriodId(periodId);
        return enrollments.stream().map(this::enrollmentResponse).toList();
    }


    // get enrollments by status
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByStatus(EnrollmentStatus status) {
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStatus(status);
        return enrollments.stream().map(this::enrollmentResponse).toList();
    }


    // find and lock an open academic period
    private AcademicPeriodEntity lockOpenPeriod(Long periodId) {

        AcademicPeriodEntity period = entityManager.find(AcademicPeriodEntity.class, periodId, LockModeType.PESSIMISTIC_WRITE);

        if (period == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo academico no encontrado");
        }

        // reload the period after acquiring its lock
        entityManager.refresh(period);

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

        // reload the student status after acquiring the lock
        entityManager.refresh(student);

        return student;
    }


    // find the prior administrative matriculation for the student and period
    private PeriodMatriculationEntity findMatriculation(Long studentId, Long periodId) {
        return matriculationRepository.findByStudentIdAndAcademicPeriodId(studentId, periodId).orElseThrow(() -> 
                new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante no está matriculado en este período académico"));
    }


    // Protects the complete selection once any grade has been recorded.
    private void validateSelectionChange(Long studentId, Long periodId) {
        if (!gradeRepository.findGradedSectionIdsByStudentAndPeriod(studentId, periodId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No puedes modificar tu seleccion porque ya existen calificaciones registradas en este periodo");
        }
    }


    private void invalidateSelectionVerification(Long studentId, Long periodId) {

        PeriodMatriculationEntity matriculation =
                findMatriculation(studentId, periodId);

        matriculation.setEnrollmentConfirmed(false);
        matriculationRepository.save(matriculation);
    }



    // find and lock section for capacity changes
    private SectionEntity findLockedSectionById(Long id) {
        SectionEntity section = entityManager.find(SectionEntity.class, id);

        if (section == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Seccion no encontrada");
        }

        // lock and reload the section, including its current version and capacity
        entityManager.refresh(section, LockModeType.PESSIMISTIC_WRITE);

        return section;
    }
    

    private void validateStudentStatus(StudentEntity student) {
        if (student.getStatus() != StudentStatus.REGULAR) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante no se encuentra habilitado");
        }
    }


    // validates the enrollment conditions
    private void validateEnrollment(StudentEntity student, SectionEntity section) {

        validateStudentStatus(student);

        // Checks that the period is open 
        if (section.getAcademicPeriod().getStatus() != AcademicPeriodStatus.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El periodo debe estar abierto");
        }

        // Checks that the section is active
        if(section.getStatus() != Status.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La seccion debe estar activa");
        }

        // Checks that the subject belongs to the student study plan 
        if (!student.getStudyPlan().getId().equals(section.getSubject().getStudyPlan().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La asignatura no pertenece al plan de estudio del estudiante");
        }
    }


    private boolean hasApprovedPrerequisites(SubjectEntity subject, Predicate<Long> approvedSubject) {
        return subject.getPrerequisites().stream()
                .allMatch(prerequisite -> approvedSubject.test(prerequisite.getId()));
    }

    // The catalogue and final enrollment validation share the prerequisite rule.
    private void validatePrerequisites(StudentEntity student, SubjectEntity subject) {
        if (!hasApprovedPrerequisites(subject, id -> hasApprovedSubject(student.getId(), id))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El estudiante no cumple con los prerequisitos de la asignatura");
        }
    }


    // Checks if the student passed a subject
    private boolean hasApprovedSubject(Long studentId, Long subjectId) {
        return gradeRepository.existsByEnrollment_Student_IdAndEnrollment_Section_Subject_IdAndResult(
                        studentId, subjectId, GradeResult.APROBADO);
    }


    // Validates that the subject has not been passed
    private void validateSubjectNotApproved(StudentEntity student, SubjectEntity subject) {

        boolean approved = gradeRepository.existsByEnrollment_Student_IdAndEnrollment_Section_Subject_IdAndResult(
                        student.getId(), subject.getId(), GradeResult.APROBADO);

        if (approved) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante ya aprobo esta asignatura");
        }
    }


    // Validates the student schedule to prevent conflicts
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


    // Validates the available capacity 
    private void validateCapacity(SectionEntity section) {
        validateCapacity(section, false);
    }

    private void validateCapacity(SectionEntity section, boolean alreadyEnrolled) {
        Long enrolled = enrollmentRepository.countBySectionIdAndStatus(section.getId(), EnrollmentStatus.ACTIVO);

        if (enrolled + (alreadyEnrolled ? 0 : 1) > section.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La seccion no tiene cupos disponibles");
        }
    }


    // Validates the maximum number of subjects per period
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


    // Validates the minimum number of subjects per period
    private void validateMinimumSubjects(StudentEntity student, Long periodId) {

        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudentIdAndAcademicPeriodId(student.getId(), periodId);

        Long total = enrollments.stream().filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVO).count();

        List<SubjectEntity> subjects = subjectRepository.findByStudyPlanId(student.getStudyPlan().getId());

        Set<Long> pendingSubjectIds = subjects.stream()
                .filter(subject -> !hasApprovedSubject(student.getId(), subject.getId()))
                .map(SubjectEntity::getId).collect(Collectors.toSet());
        long pending = pendingSubjectIds.size();

        if (pending == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante no tiene asignaturas pendientes para inscribir");
        }

        Long minimum = Math.min(3, pending);

        // With fewer than three pending subjects, require only those available to enroll.
        if (pending < 3) {
            Set<Long> availableSubjectIds = enrollments.stream()
                    .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVO)
                    .map(enrollment -> enrollment.getSection().getSubject().getId()).collect(Collectors.toSet());
            Set<String> occupiedBlocks = enrollments.stream()
                    .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVO)
                    .flatMap(enrollment -> enrollment.getSection().getBlocks().stream()).collect(Collectors.toSet());

            for (SectionEntity section : sectionRepository.findByAcademicPeriodId(periodId)) {
                if (pendingSubjectIds.contains(section.getSubject().getId()) && section.getStatus() == Status.ACTIVA &&
                    hasApprovedPrerequisites(section.getSubject(), id -> hasApprovedSubject(student.getId(), id)) &&
                    section.getBlocks().stream().noneMatch(occupiedBlocks::contains) &&
                    enrollmentRepository.countBySectionIdAndStatus(section.getId(), EnrollmentStatus.ACTIVO) < section.getCapacity()) {
                    availableSubjectIds.add(section.getSubject().getId());
                }
            }

            minimum = pendingSubjectIds.stream().filter(availableSubjectIds::contains).count();
        }

        if (total < minimum) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Debe inscribir al menos " + minimum + " asignaturas para verificar la seleccion; actualmente tiene " + total);
        }
    }


    // Earliest pending semester that is actually available under the existing priority rule.
    // Candidate capacity, candidate timetable and the six-subject limit are not catalogue filters.
    private int findPrioritySemester(List<SubjectEntity> subjects, List<EnrollmentEntity> enrollments,
                                     List<SectionEntity> sections, Predicate<Long> approvedSubject,
                                     ToLongFunction<SectionEntity> enrolledCount) {
        Set<Long> enrolledSubjectIds = enrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVO)
                .map(enrollment -> enrollment.getSection().getSubject().getId())
                .collect(Collectors.toSet());
        Set<String> occupiedBlocks = enrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVO)
                .flatMap(enrollment -> enrollment.getSection().getBlocks().stream())
                .collect(Collectors.toSet());
        return subjects.stream()
                .filter(subject -> !approvedSubject.test(subject.getId()))
                .filter(subject -> !enrolledSubjectIds.contains(subject.getId()))
                .filter(subject -> hasApprovedPrerequisites(subject, approvedSubject))
                .filter(subject -> sections.stream().anyMatch(section ->
                        section.getSubject().getId().equals(subject.getId()) &&
                        section.getStatus() == Status.ACTIVA &&
                        enrolledCount.applyAsLong(section) < section.getCapacity() &&
                        section.getBlocks().stream().noneMatch(occupiedBlocks::contains)))
                .mapToInt(SubjectEntity::getSemester).min().orElse(Integer.MAX_VALUE);
    }

    // Keep the definitive priority validation at enrollment/verification time.
    private void validateSemesterPriority(StudentEntity student, SectionEntity section) {
        Long studentId = student.getId();
        Long periodId = section.getAcademicPeriod().getId();
        int prioritySemester = findPrioritySemester(
                subjectRepository.findByStudyPlanId(student.getStudyPlan().getId()).stream()
                        .filter(subject -> subject.getSemester() < section.getSubject().getSemester()).toList(),
                enrollmentRepository.findByStudentIdAndAcademicPeriodId(studentId, periodId),
                sectionRepository.findByAcademicPeriodId(periodId),
                id -> hasApprovedSubject(studentId, id),
                current -> enrollmentRepository.countBySectionIdAndStatus(current.getId(), EnrollmentStatus.ACTIVO));
        if (section.getSubject().getSemester() > prioritySemester) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Debe priorizar las asignaturas pendientes de semestres inferiores");
        }
    }

    @Transactional(readOnly = true)
    public List<SectionResponseDTO> getCurrentStudentEligibleSections(String keycloakSubject, Long periodId) {
        StudentEntity student = studentRepository.findByKeycloakSubject(keycloakSubject).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un estudiante asociado a esta cuenta"));
        AcademicPeriodEntity period = entityManager.find(AcademicPeriodEntity.class, periodId);
        if (period == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo academico no encontrado");
        }
        if (period.getStatus() != AcademicPeriodStatus.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El periodo academico debe estar abierto");
        }

        findMatriculation(student.getId(), periodId);

        Long planId = student.getStudyPlan().getId();
        List<SectionEntity> sections = sectionRepository.findByAcademicPeriodId(periodId);
        List<EnrollmentEntity> enrollments = enrollmentRepository
                .findByStudentIdAndAcademicPeriodId(student.getId(), periodId);
        Set<Long> enrolledSectionIds = enrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVO)
                .map(enrollment -> enrollment.getSection().getId()).collect(Collectors.toSet());
        Set<Long> approvedSubjectIds = new HashSet<>(gradeRepository
                .findSubjectIdsByStudentAndResult(student.getId(), GradeResult.APROBADO));
        Map<Long, Long> enrolledCounts = enrollmentRepository
                .countEnrollmentsBySectionInPeriod(periodId, EnrollmentStatus.ACTIVO).stream()
                .collect(Collectors.toMap(EnrollmentRepository.SectionEnrollmentCount::getSectionId,
                        EnrollmentRepository.SectionEnrollmentCount::getTotal));
        boolean enabled = student.getStatus() == StudentStatus.REGULAR;
        // Explains a blocked catalogue without hiding existing active selections.
        if (!enabled && enrolledSectionIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El estudiante se encuentra en estado " + student.getStatus() + "; debe estar REGULAR para inscribir asignaturas en " + period.getCode());
        }
        Set<Long> gradedSectionIds = new HashSet<>(gradeRepository
                .findGradedSectionIdsByStudentAndPeriod(student.getId(), periodId));

        return sections.stream()
                // Existing active enrollments remain visible for identification/cancellation,
                // even if their section is inactive or academic eligibility has since changed.
                .filter(section -> enrolledSectionIds.contains(section.getId()) ||
                        (enabled && section.getStatus() == Status.ACTIVA &&
                         section.getSubject().getStudyPlan().getId().equals(planId) &&
                         !approvedSubjectIds.contains(section.getSubject().getId()) &&
                         !gradedSectionIds.contains(section.getId()) &&
                         hasApprovedPrerequisites(section.getSubject(), approvedSubjectIds::contains)))
                .map(section -> {
                    SectionResponseDTO response = new SectionResponseDTO(section);
                    response.setEnrolledStudents(Math.toIntExact(enrolledCounts.getOrDefault(section.getId(), 0L)));
                    return response;
                }).toList();
    }


    // create enrollment
    @Transactional 
    public EnrollmentResponseDTO createEnrollment (EnrollmentRequestDTO enrollment) {

        // Checks that the enrollment information is complete
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

        findMatriculation(student.getId(), periodId);
        validateSelectionChange(student.getId(), periodId);

        SectionEntity section = findLockedSectionById(enrollment.getSectionId());

        if (!section.getAcademicPeriod().getId().equals(periodId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La seccion cambio de periodo");
        }

        // Checks that the subject is not enrolled in another section
        List<EnrollmentEntity> studentEnrollments = enrollmentRepository.findByStudentIdAndAcademicPeriodId(student.getId(), periodId);

        for (EnrollmentEntity current : studentEnrollments) {

            if (current.getStatus() == EnrollmentStatus.ACTIVO &&
                current.getSection().getSubject().getId().equals(section.getSubject().getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante ya tiene inscrita esta asignatura en el periodo");
            }
        }

        // Validates the enrollment
        validateEnrollment(student, section);

        // Checks that the student is not already enrolled 
        EnrollmentEntity existing = enrollmentRepository.findByStudentIdAndSectionId(student.getId(), section.getId()).orElse(null);

        if (existing != null && existing.getStatus() == EnrollmentStatus.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante ya se encuentra inscrito en la seccion");
        }

        // Prevents reactivating enrollments that already have a grade
        if (existing != null && gradeRepository.existsByEnrollmentId(existing.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede reactivar una inscripcion con calificacion");
        }

        // Validates that the subject has not been passed
        validateSubjectNotApproved(student, section.getSubject());

        // Validates prerequisites, schedule conflicts, capacity, subject count and priority
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

        // Updates the enrolled student count
        enrollmentRepository.flush();
        Long enrolled = enrollmentRepository.countBySectionIdAndStatus(section.getId(), EnrollmentStatus.ACTIVO);
        section.setEnrolledStudents(Math.toIntExact(enrolled));
        sectionRepository.save(section);

        // Requires verification again after the selection changes
        invalidateSelectionVerification(student.getId(), periodId);

        return enrollmentResponse(saved);


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

        validateSelectionChange(studentId, periodId);

        SectionEntity section = findLockedSectionById(sectionId);

        // reload enrollment after acquiring locks
        entityManager.refresh(enrollment);

        // Validates that the period is open 
        if (section.getAcademicPeriod().getStatus() != AcademicPeriodStatus.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cambiar, el periodo se encuentra cerrado");
        }

        // Checks that the enrollment is active
        if (enrollment.getStatus() == EnrollmentStatus.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La inscripcion ya se encuentra cancelada");
        }

        // Prevents changes when a grade already exists
        if (gradeRepository.existsByEnrollmentId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cancelar ya que cuenta con calificacion");
        }

        // Cancels the enrollment
        enrollment.setStatus(EnrollmentStatus.CANCELADO);
        EnrollmentEntity saved =  enrollmentRepository.save(enrollment);

        // Updates the enrolled student count 
        enrollmentRepository.flush();
        Long enrolled = enrollmentRepository.countBySectionIdAndStatus(section.getId(), EnrollmentStatus.ACTIVO);
        section.setEnrolledStudents(Math.toIntExact(enrolled));
        sectionRepository.save(section);

        // Requires verification again after the selection changes
        invalidateSelectionVerification(studentId, periodId);

        return enrollmentResponse(saved);
    }


    // Cancels the provisional selection in one transaction using the existing rules.
    @Transactional
    public void discardCurrentStudentSelection(String keycloakSubject, Long periodId) {
        StudentEntity student = studentRepository.findByKeycloakSubject(keycloakSubject).orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un estudiante asociado a esta cuenta"));

        lockOpenPeriod(periodId);
        lockStudent(student.getId());
        PeriodMatriculationEntity matriculation = findMatriculation(student.getId(), periodId);

        if (Boolean.TRUE.equals(matriculation.getEnrollmentConfirmed())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede descartar una inscripcion confirmada");
        }

        validateSelectionChange(student.getId(), periodId);

        List<EnrollmentEntity> enrollments = enrollmentRepository
                .findByStudentIdAndAcademicPeriodId(student.getId(), periodId);
        for (EnrollmentEntity enrollment : enrollments) {
            if (enrollment.getStatus() == EnrollmentStatus.ACTIVO) {
                cancelEnrollment(enrollment.getId());
            }
        }
    }


    // confirm subject selection
    @Transactional
    public void confirmEnrollmentSelection(Long studentId, Long periodId) {
        verifyEnrollmentSelection(studentId, periodId);
    }
    
    
    // Shared selection verification.
    private void verifyEnrollmentSelection(Long studentId, Long periodId) {

        // Lock academic period and student
        lockOpenPeriod(periodId);
        StudentEntity student = lockStudent(studentId);

        // Validate student status
        validateStudentStatus(student);

        // require a prior administrative matriculation before confirming the selection
        findMatriculation(studentId, periodId);

        // Get student enrollments
        List<EnrollmentEntity> enrollments =
                enrollmentRepository.findByStudentIdAndAcademicPeriodId(
                        studentId, periodId
                );

        List<EnrollmentEntity> activeEnrollments = enrollments.stream()
                .filter(enrollment ->
                        enrollment.getStatus() == EnrollmentStatus.ACTIVO)
                .toList();

        // Prevent verifying an empty selection
        if (activeEnrollments.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El estudiante no tiene asignaturas inscritas"
            );
        }

        Set<Long> selectedSubjects = new HashSet<>();

        // Validate selected subjects
        for (EnrollmentEntity enrollment : activeEnrollments) {

            SectionEntity section =
                    findLockedSectionById(enrollment.getSection().getId());

            if (!section.getAcademicPeriod().getId().equals(periodId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La seccion no pertenece al periodo de la inscripcion");
            }
            if (!selectedSubjects.add(section.getSubject().getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La seleccion contiene asignaturas duplicadas");
            }
            validateEnrollment(student, section);
            validateSubjectNotApproved(student, section.getSubject());
            validatePrerequisites(student, section.getSubject());
            validateStudentSchedule(student, section);
            validateCapacity(section, true);
            validateMaximumSubjects(student, section, true);
            validateSemesterPriority(student, section);
        }

        // Validate required minimum
        validateMinimumSubjects(student, periodId);

        // Saves the selection as verified
        PeriodMatriculationEntity matriculation =
                findMatriculation(studentId, periodId);

        matriculation.setEnrollmentConfirmed(true);
        matriculationRepository.save(matriculation);
    }



    // Verify all academic rules, including the minimum, for the authenticated student
    @Transactional
    public void verifyCurrentStudentSelection(String keycloakSubject, Long periodId) {
        StudentEntity student = studentRepository.findByKeycloakSubject(keycloakSubject).orElseThrow(() -> 
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un estudiante asociado a esta cuenta"));

        verifyEnrollmentSelection(student.getId(), periodId);
    }


    // get enrollments of authenticated student
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getCurrentStudentEnrollments(String keycloakSubject) {

        StudentEntity student = studentRepository.findByKeycloakSubject(keycloakSubject).orElseThrow(() -> 
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un estudiante asociado a esta cuenta"));

        return getEnrollmentsByStudent(student.getId()).stream()
                .filter(EnrollmentResponseDTO::isConfirmed).toList();
    }

    // The enrollment form also needs provisional reservations for its schedule.
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getCurrentStudentSelection(String keycloakSubject) {
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


    @Transactional(readOnly = true)
    public boolean isCurrentStudentSelectionVerified(
            String keycloakSubject,
            Long periodId) {

        StudentEntity student = studentRepository
                .findByKeycloakSubject(keycloakSubject)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un estudiante asociado a esta cuenta"
                ));

        if (entityManager.find(AcademicPeriodEntity.class, periodId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo academico no encontrado");
        }

        return matriculationRepository
                .findByStudentIdAndAcademicPeriodId(student.getId(), periodId)
                .map(matriculation ->
                        Boolean.TRUE.equals(matriculation.getEnrollmentConfirmed()))
                .orElse(false);
    }


}
