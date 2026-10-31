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


    private void invalidateSelectionVerification(Long studentId, Long periodId) {

        PeriodMatriculationEntity matriculation =
                findMatriculation(studentId, periodId);

        matriculation.setEnrollmentConfirmed(false);
        matriculationRepository.save(matriculation);
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

        Long planId = student.getStudyPlan().getId();
        List<SubjectEntity> subjects = subjectRepository.findByStudyPlanId(planId);
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
        int prioritySemester = findPrioritySemester(subjects, enrollments, sections,
                approvedSubjectIds::contains, current -> enrolledCounts.getOrDefault(current.getId(), 0L));
        boolean enabled = student.getStatus() == StudentStatus.REGULAR &&
                matriculationRepository.existsByStudentIdAndAcademicPeriodId(student.getId(), periodId);
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
                         hasApprovedPrerequisites(section.getSubject(), approvedSubjectIds::contains) &&
                         section.getSubject().getSemester() <= prioritySemester))
                .map(section -> {
                    SectionResponseDTO response = new SectionResponseDTO(section);
                    response.setEnrolledStudents(Math.toIntExact(enrolledCounts.getOrDefault(section.getId(), 0L)));
                    return response;
                }).toList();
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede reactivar una inscripcion con calificacion");
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

        // La seleccion cambio, debe verificarse nuevamente
        invalidateSelectionVerification(student.getId(), periodId);

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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cancelar ya que cuenta con calificacion");
        }

        // cancelar la inscripcion
        enrollment.setStatus(EnrollmentStatus.CANCELADO);
        EnrollmentEntity saved =  enrollmentRepository.save(enrollment);

        // actualizar la cantidad de alumnos inscritos 
        enrollmentRepository.flush();
        Long enrolled = enrollmentRepository.countBySectionIdAndStatus(section.getId(), EnrollmentStatus.ACTIVO);
        section.setEnrolledStudents(Math.toIntExact(enrolled));
        sectionRepository.save(section);

        // La seleccion cambio, debe verificarse nuevamente
        invalidateSelectionVerification(studentId, periodId);

        return new EnrollmentResponseDTO(saved);
    }


    // confirm subject selection
    @Transactional
    public void confirmEnrollmentSelection(Long studentId, Long periodId) {
        verifyEnrollmentSelection(studentId, periodId);
    }
    
    
    // Shared verification for the administrative and personal flows.
    private void verifyEnrollmentSelection(Long studentId, Long periodId) {

        // Lock academic period and student
        lockOpenPeriod(periodId);
        StudentEntity student = lockStudent(studentId);

        // Validate student status
        validateStudentStatus(student);

        // Verify matriculation in the academic period
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

        // Guardar la seleccion como verificada
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
