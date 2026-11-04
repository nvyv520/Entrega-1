package SIGA.backend.services;

import static SIGA.backend.services.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import SIGA.backend.dtos.*;
import SIGA.backend.entities.*;
import SIGA.backend.repositories.*;
import jakarta.persistence.*;
import java.util.*;
import org.junit.jupiter.api.*;

class EnrollmentServiceTests {
  EnrollmentRepository repo;
  StudentRepository students;
  SectionRepository sections;
  PeriodMatriculationRepository matriculations;
  GradeRepository grades;
  SubjectRepository subjects;
  EntityManager em;
  EnrollmentService s;
  EnrollmentEntity entity;
  PeriodMatriculationEntity matriculation;
  EnrollmentRequestDTO r;

  @BeforeEach
  void setup() {
    repo = mock(EnrollmentRepository.class);
    students = mock(StudentRepository.class);
    sections = mock(SectionRepository.class);
    matriculations = mock(PeriodMatriculationRepository.class);
    grades = mock(GradeRepository.class);
    subjects = mock(SubjectRepository.class);
    em = mock(EntityManager.class);
    s = new EnrollmentService(repo, students, sections, matriculations, grades, em, subjects);
    entity = enrollment();
    matriculation =
        new PeriodMatriculationEntity(1L, entity.getStudent(), entity.getAcademicPeriod(), false);
    r = new EnrollmentRequestDTO();
    r.setStudentId(5L);
    r.setSectionId(10L);
    when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    when(repo.findById(20L)).thenReturn(Optional.of(entity));
    when(sections.findById(10L)).thenReturn(Optional.of(entity.getSection()));
    when(students.findByKeycloakSubject("student")).thenReturn(Optional.of(entity.getStudent()));
    when(em.find(AcademicPeriodEntity.class, 3L, LockModeType.PESSIMISTIC_WRITE))
        .thenReturn(entity.getAcademicPeriod());
    when(em.find(AcademicPeriodEntity.class, 3L)).thenReturn(entity.getAcademicPeriod());
    when(em.find(StudentEntity.class, 5L, LockModeType.PESSIMISTIC_WRITE))
        .thenReturn(entity.getStudent());
    when(em.find(SectionEntity.class, 10L, LockModeType.PESSIMISTIC_WRITE))
        .thenReturn(entity.getSection());
    when(matriculations.findByStudentIdAndAcademicPeriodId(5L, 3L))
        .thenReturn(Optional.of(matriculation));
    when(subjects.findByStudyPlanId(2L)).thenReturn(List.of(entity.getSection().getSubject()));
  }

  @Test
  void readsExposeConfirmationCorrectly() {
    when(repo.findAll()).thenReturn(List.of(entity));
    when(repo.findByStudentId(5L)).thenReturn(List.of(entity));
    when(repo.findBySectionId(10L)).thenReturn(List.of(entity));
    when(repo.findByAcademicPeriodId(3L)).thenReturn(List.of(entity));
    when(repo.findByStatus(EnrollmentStatus.ACTIVO)).thenReturn(List.of(entity));
    assertEquals(1, s.getAllEnrollments().size());
    assertFalse(s.getEnrollmentById(20L).isConfirmed());
    assertEquals(1, s.getEnrollmentsByStudent(5L).size());
    assertEquals(1, s.getEnrollmentsBySection(10L).size());
    assertEquals(1, s.getEnrollmentsByAcademicPeriod(3L).size());
    assertEquals(1, s.getEnrollmentsByStatus(EnrollmentStatus.ACTIVO).size());
    assertTrue(s.getCurrentStudentEnrollments("student").isEmpty());
    assertEquals(1, s.getCurrentStudentSelection("student").size());
    when(repo.isSelectionConfirmed(5L, 3L)).thenReturn(true);
    assertEquals(1, s.getCurrentStudentEnrollments("student").size());
    assertFalse(s.isCurrentStudentSelectionVerified("student", 3L));
    matriculation.setEnrollmentConfirmed(true);
    assertTrue(s.isCurrentStudentSelectionVerified("student", 3L));
    rejects(404, () -> s.getEnrollmentById(99L));
    rejects(404, () -> s.isCurrentStudentSelectionVerified("student", 99L));
  }

  @Test
  void newReservationUsesExistingMatriculationAndResetsConfirmation() {
    assertEquals(EnrollmentStatus.ACTIVO, s.createEnrollment(r).getStatus());
    verify(matriculations).save(matriculation);
    verify(repo).flush();
    verify(sections).save(entity.getSection());
    assertFalse(matriculation.getEnrollmentConfirmed());
  }

  @Test
  void missingMatriculationCannotCreateReservationOrAccessCatalogue() {
    when(matriculations.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(Optional.empty());
    rejects(409, () -> s.createEnrollment(r));
    rejects(409, () -> s.getCurrentStudentEligibleSections("student", 3L));
    verify(repo, never()).save(any());
    verify(matriculations, never()).save(any());
    verify(sections, never()).save(any());
  }

  @Test
  void reactivationAndPersonalCreation() {
    entity.setStatus(EnrollmentStatus.CANCELADO);
    when(repo.findByStudentIdAndSectionId(5L, 10L)).thenReturn(Optional.of(entity));
    when(matriculations.existsByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(true);
    assertEquals(20L, s.createCurrentStudentEnrollment("student", 10L).getId());
    assertEquals(EnrollmentStatus.ACTIVO, entity.getStatus());
    verify(matriculations).save(matriculation);
  }

  @Test
  void cancelAndDiscardResetVerification() {
    assertEquals(
        EnrollmentStatus.CANCELADO, s.cancelCurrentStudentEnrollment("student", 20L).getStatus());
    assertFalse(matriculation.getEnrollmentConfirmed());
    entity.setStatus(EnrollmentStatus.ACTIVO);
    when(repo.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(List.of(entity));
    s.discardCurrentStudentSelection("student", 3L);
    assertEquals(EnrollmentStatus.CANCELADO, entity.getStatus());
    matriculation.setEnrollmentConfirmed(true);
    rejects(409, () -> s.discardCurrentStudentSelection("student", 3L));
  }

  @Test
  void missingRequestsAndReferences() {
    rejects(400, () -> s.createEnrollment(null));
    rejects(400, () -> s.createEnrollment(new EnrollmentRequestDTO()));
    rejects(400, () -> s.createCurrentStudentEnrollment("student", null));
    r.setSectionId(99L);
    rejects(404, () -> s.createEnrollment(r));
    r.setSectionId(10L);
    r.setStudentId(99L);
    rejects(404, () -> s.createEnrollment(r));
    when(em.find(AcademicPeriodEntity.class, 3L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(null);
    rejects(404, () -> s.createEnrollment(r));
  }

  @Test
  void unknownAccountsCannotUsePersonalOperations() {
    rejects(404, () -> s.getCurrentStudentEnrollments("unknown"));
    rejects(404, () -> s.getCurrentStudentSelection("unknown"));
    rejects(404, () -> s.getCurrentStudentEligibleSections("unknown", 3L));
    rejects(404, () -> s.createCurrentStudentEnrollment("unknown", 10L));
    rejects(404, () -> s.cancelCurrentStudentEnrollment("unknown", 20L));
    rejects(404, () -> s.discardCurrentStudentSelection("unknown", 3L));
    rejects(404, () -> s.isCurrentStudentSelectionVerified("unknown", 3L));
  }

  @Test
  void rejectsDuplicatesGradedSelectionsAndApprovedSubjects() {
    when(repo.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(List.of(entity));
    rejects(409, () -> s.createEnrollment(r));
    when(repo.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(List.of());
    when(repo.findByStudentIdAndSectionId(5L, 10L)).thenReturn(Optional.of(entity));
    rejects(409, () -> s.createEnrollment(r));
    entity.setStatus(EnrollmentStatus.CANCELADO);
    when(grades.existsByEnrollmentId(20L)).thenReturn(true);
    rejects(409, () -> s.createEnrollment(r));
    when(grades.existsByEnrollmentId(20L)).thenReturn(false);
    when(grades.existsByEnrollment_Student_IdAndEnrollment_Section_Subject_IdAndResult(
            5L, 8L, GradeResult.APROBADO))
        .thenReturn(true);
    rejects(409, () -> s.createEnrollment(r));
    when(grades.findGradedSectionIdsByStudentAndPeriod(5L, 3L)).thenReturn(List.of(10L));
    rejects(409, () -> s.createEnrollment(r));
    rejects(409, () -> s.cancelEnrollment(20L));
  }

  @Test
  void rejectsInactiveSectionWrongPlanAndMissingLockedSection() {
    entity.getSection().setStatus(Status.INACTIVA);
    rejects(409, () -> s.createEnrollment(r));
    entity.getSection().setStatus(Status.ACTIVA);
    entity.getSection().getSubject().getStudyPlan().setId(99L);
    rejects(409, () -> s.createEnrollment(r));
    when(em.find(SectionEntity.class, 10L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(null);
    rejects(404, () -> s.createEnrollment(r));
  }

  @Test
  void rejectsFullCapacityMaximumAndSemesterPriority() {
    when(repo.countBySectionIdAndStatus(10L, EnrollmentStatus.ACTIVO)).thenReturn(30L);
    rejects(409, () -> s.createEnrollment(r));
    when(repo.countBySectionIdAndStatus(10L, EnrollmentStatus.ACTIVO)).thenReturn(0L);
    var other = enrollment();
    other.getSection().setId(99L);
    other.getSection().getSubject().setId(99L);
    other.getSection().setBlocks(Set.of("M1"));
    when(repo.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(Collections.nCopies(6, other));
    rejects(409, () -> s.createEnrollment(r));
    when(repo.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(List.of());
    var earlier = section();
    earlier.setId(99L);
    earlier.getSubject().setId(99L);
    entity.getSection().getSubject().setSemester(2);
    when(subjects.findByStudyPlanId(2L))
        .thenReturn(List.of(earlier.getSubject(), entity.getSection().getSubject()));
    when(sections.findByAcademicPeriodId(3L)).thenReturn(List.of(earlier));
    rejects(409, () -> s.createEnrollment(r));
  }

  @Test
  void cancelRejectsCancelledAndGradedEnrollment() {
    entity.setStatus(EnrollmentStatus.CANCELADO);
    rejects(409, () -> s.cancelEnrollment(20L));
    entity.setStatus(EnrollmentStatus.ACTIVO);
    when(grades.existsByEnrollmentId(20L)).thenReturn(true);
    rejects(409, () -> s.cancelEnrollment(20L));
    verify(repo, never()).save(any());
  }

  @Test
  void confirmationRejectsEmptyAndAlreadyCompletedPlan() {
    rejects(409, () -> s.confirmEnrollmentSelection(5L, 3L));
    when(repo.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(List.of(entity));
    when(subjects.findByStudyPlanId(2L)).thenReturn(List.of());
    rejects(409, () -> s.confirmEnrollmentSelection(5L, 3L));
  }

  @Test
  void confirmationAllowsAvailableRemainderAndIgnoresCancelledEnrollments() {
    when(repo.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(List.of(entity));
    var remaining = section();
    remaining.setId(99L);
    remaining.getSubject().setId(99L);
    remaining.setBlocks(Set.of("M1"));
    when(subjects.findByStudyPlanId(2L))
        .thenReturn(List.of(entity.getSection().getSubject(), remaining.getSubject()));
    when(sections.findByAcademicPeriodId(3L)).thenReturn(List.of(remaining));
    rejects(409, () -> s.confirmEnrollmentSelection(5L, 3L));
    remaining.setStatus(Status.INACTIVA);
    s.confirmEnrollmentSelection(5L, 3L);
    assertTrue(matriculation.getEnrollmentConfirmed());
  }

  @Test
  void catalogueFiltersPrerequisitesAndPreservesActiveSelections() {
    var eligible = entity.getSection();
    var blocked = section();
    blocked.setId(99L);
    blocked.getSubject().setId(99L);
    blocked.getSubject().setPrerequisites(Set.of(subject()));
    when(sections.findByAcademicPeriodId(3L)).thenReturn(List.of(eligible, blocked));
    var count = mock(EnrollmentRepository.SectionEnrollmentCount.class);
    when(count.getSectionId()).thenReturn(10L);
    when(count.getTotal()).thenReturn(2L);
    when(repo.countEnrollmentsBySectionInPeriod(3L, EnrollmentStatus.ACTIVO))
        .thenReturn(List.of(count));
    assertEquals(1, s.getCurrentStudentEligibleSections("student", 3L).size());
    assertEquals(
        2, s.getCurrentStudentEligibleSections("student", 3L).get(0).getEnrolledStudents());
    entity.getStudent().setStatus(StudentStatus.POSTERGACION);
    rejects(409, () -> s.getCurrentStudentEligibleSections("student", 3L));
    when(repo.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(List.of(entity));
    assertEquals(1, s.getCurrentStudentEligibleSections("student", 3L).size());
    rejects(404, () -> s.getCurrentStudentEligibleSections("student", 99L));
    entity.getAcademicPeriod().setStatus(AcademicPeriodStatus.CERRADO);
    rejects(409, () -> s.getCurrentStudentEligibleSections("student", 3L));
  }
}
