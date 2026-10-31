package SIGA.backend.services;

import SIGA.backend.entities.*;
import SIGA.backend.repositories.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnrollmentAuthorizationTests {
    private EnrollmentService service;
    private EnrollmentRepository enrollments;
    private SubjectRepository subjects;
    private StudentRepository students;
    private PeriodMatriculationRepository matriculations;
    private GradeRepository grades;
    private EntityManager entityManager;
    private StudentEntity student;
    private AcademicPeriodEntity period;
    private PeriodMatriculationEntity matriculation;
    private SectionEntity section;
    private EnrollmentEntity enrollment;

    @BeforeEach
    void setUp() {
        enrollments = mock(EnrollmentRepository.class);
        students = mock(StudentRepository.class);
        matriculations = mock(PeriodMatriculationRepository.class);
        grades = mock(GradeRepository.class);
        entityManager = mock(EntityManager.class);
        subjects = mock(SubjectRepository.class);
        SectionRepository sections = mock(SectionRepository.class);
        service = new EnrollmentService(enrollments, students, sections, matriculations, grades,
                entityManager, subjects);
        StudyPlanEntity plan = new StudyPlanEntity();
        plan.setId(2L);
        student = new StudentEntity();
        student.setId(5L);
        student.setStatus(StudentStatus.REGULAR);
        student.setStudyPlan(plan);
        period = new AcademicPeriodEntity();
        period.setId(3L);
        period.setStatus(AcademicPeriodStatus.ABIERTO);
        matriculation = new PeriodMatriculationEntity();
        matriculation.setStudent(student);
        matriculation.setAcademicPeriod(period);
        SubjectEntity subject = new SubjectEntity();
        subject.setId(8L);
        subject.setStudyPlan(plan);
        subject.setSemester(1);
        section = new SectionEntity();
        section.setId(10L);
        section.setAcademicPeriod(period);
        section.setStatus(Status.ACTIVA);
        section.setSubject(subject);
        section.setCapacity(1);
        section.setBlocks(Set.of("L1"));
        enrollment = new EnrollmentEntity();
        enrollment.setId(20L);
        enrollment.setStudent(student);
        enrollment.setSection(section);
        enrollment.setStatus(EnrollmentStatus.ACTIVO);
        enrollment.setAcademicPeriod(period);
        when(students.findByKeycloakSubject("student-sub")).thenReturn(Optional.of(student));
        when(entityManager.find(AcademicPeriodEntity.class, 3L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(period);
        when(entityManager.find(StudentEntity.class, 5L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(student);
        when(entityManager.find(SectionEntity.class, 10L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(section);
        when(matriculations.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(Optional.of(matriculation));
        when(matriculations.existsByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(true);
        when(enrollments.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(List.of(enrollment));
        when(enrollments.countBySectionIdAndStatus(10L, EnrollmentStatus.ACTIVO)).thenReturn(1L);
        SubjectEntity pending1 = new SubjectEntity(); pending1.setId(9L); pending1.setSemester(1);
        SubjectEntity pending2 = new SubjectEntity(); pending2.setId(11L); pending2.setSemester(1);
        when(subjects.findByStudyPlanId(2L)).thenReturn(List.of(subject, pending1, pending2));
    }

    @Test
    void personalVerificationAllowsLastPendingSubjectAndAnAlreadyOccupiedSeat() {
        when(subjects.findByStudyPlanId(2L)).thenReturn(List.of(section.getSubject()));
        service.verifyCurrentStudentSelection("student-sub", 3L);
        assertTrue(matriculation.getEnrollmentConfirmed());
        verify(matriculations).save(matriculation);
        var order = inOrder(entityManager);
        order.verify(entityManager).find(AcademicPeriodEntity.class, 3L, LockModeType.PESSIMISTIC_WRITE);
        order.verify(entityManager).find(StudentEntity.class, 5L, LockModeType.PESSIMISTIC_WRITE);
        order.verify(entityManager).find(SectionEntity.class, 10L, LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void administrativeConfirmationKeepsMinimumRequirement() {
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.confirmEnrollmentSelection(5L, 3L)).getStatusCode());
        assertFalse(matriculation.getEnrollmentConfirmed());
        verify(matriculations, never()).save(any());
    }

    @Test
    void closedPeriodIsRejectedAndOpenSelectionCanBeVerifiedAgain() {
        period.setStatus(AcademicPeriodStatus.CERRADO);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.verifyCurrentStudentSelection("student-sub", 3L)).getStatusCode());
        period.setStatus(AcademicPeriodStatus.ABIERTO);
        matriculation.setEnrollmentConfirmed(true);
        when(subjects.findByStudyPlanId(2L)).thenReturn(List.of(section.getSubject()));
        service.verifyCurrentStudentSelection("student-sub", 3L);
        assertTrue(matriculation.getEnrollmentConfirmed());
        verify(matriculations).save(matriculation);
    }

    @Test
    void missingStudentOrMatriculationCannotConfirm() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.verifyCurrentStudentSelection("unknown", 3L)).getStatusCode());
        when(matriculations.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.verifyCurrentStudentSelection("student-sub", 3L)).getStatusCode());
    }

    @Test
    void inactiveStudentAndOverCapacityCannotConfirm() {
        student.setStatus(StudentStatus.ELIMINADO);
        assertThrows(ResponseStatusException.class, () -> service.verifyCurrentStudentSelection("student-sub", 3L));
        student.setStatus(StudentStatus.REGULAR);
        when(enrollments.countBySectionIdAndStatus(10L, EnrollmentStatus.ACTIVO)).thenReturn(2L);
        assertThrows(ResponseStatusException.class, () -> service.verifyCurrentStudentSelection("student-sub", 3L));
        verify(matriculations, never()).save(any());
    }

    @Test
    void unmetPrerequisiteCannotConfirm() {
        SubjectEntity prerequisite = new SubjectEntity(); prerequisite.setId(30L);
        section.getSubject().setPrerequisites(Set.of(prerequisite));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.verifyCurrentStudentSelection("student-sub", 3L)).getStatusCode());
        verify(matriculations, never()).save(any());
    }

    @Test
    void studentCannotCancelAnotherStudentsEnrollment() {
        StudentEntity another = new StudentEntity(); another.setId(99L);
        enrollment.setStudent(another);
        when(enrollments.findById(20L)).thenReturn(Optional.of(enrollment));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.cancelCurrentStudentEnrollment("student-sub", 20L)).getStatusCode());
        verifyNoInteractions(entityManager);
        verify(enrollments, never()).save(any());
    }
    @Test
    void scheduleConflictsCannotBeConfirmed() {
        SectionEntity conflicting = new SectionEntity();
        conflicting.setId(11L);
        conflicting.setBlocks(Set.of("L1"));
        EnrollmentEntity another = new EnrollmentEntity();
        another.setStatus(EnrollmentStatus.ACTIVO);
        another.setSection(conflicting);
        when(enrollments.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(List.of(enrollment, another));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.verifyCurrentStudentSelection("student-sub", 3L)).getStatusCode());
        verify(matriculations, never()).save(any());
    }

    @Test
    void maximumOfSixStillAppliesToPersonalConfirmation() {
        when(enrollments.findByStudentIdAndAcademicPeriodId(5L, 3L))
                .thenReturn(java.util.Collections.nCopies(7, enrollment));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.verifyCurrentStudentSelection("student-sub", 3L)).getStatusCode());
        verify(matriculations, never()).save(any());
    }
}
