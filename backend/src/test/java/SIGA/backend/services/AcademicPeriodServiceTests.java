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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AcademicPeriodServiceTests {
  AcademicPeriodRepository repo;
  SectionRepository sections;
  EnrollmentRepository enrollments;
  PeriodMatriculationRepository matriculations;
  StudentRepository students;
  SubjectRepository subjects;
  GradeRepository grades;
  EntityManager em;
  AcademicPeriodService s;
  AcademicPeriodEntity entity;
  AcademicPeriodRequestDTO r;

  @BeforeEach
  void setup() {
    repo = mock(AcademicPeriodRepository.class);
    sections = mock(SectionRepository.class);
    enrollments = mock(EnrollmentRepository.class);
    matriculations = mock(PeriodMatriculationRepository.class);
    students = mock(StudentRepository.class);
    subjects = mock(SubjectRepository.class);
    grades = mock(GradeRepository.class);
    em = mock(EntityManager.class);
    s =
        new AcademicPeriodService(
            repo, sections, enrollments, matriculations, students, subjects, grades, em);
    entity = period();
    r = new AcademicPeriodRequestDTO();
    r.setYear(2026);
    r.setSemester(1);
    when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    when(repo.findById(3L)).thenReturn(Optional.of(entity));
    when(em.find(AcademicPeriodEntity.class, 3L, LockModeType.PESSIMISTIC_WRITE))
        .thenReturn(entity);
  }

  @Test
  void lifecycleAndQueries() {
    assertEquals("2026-1", s.createAcademicPeriod(r).getCode());
    when(repo.findAll()).thenReturn(List.of(entity));
    when(repo.findByCode("2026-1")).thenReturn(Optional.of(entity));
    when(repo.findByYear(2026)).thenReturn(List.of(entity));
    when(repo.findByStatus(AcademicPeriodStatus.ABIERTO)).thenReturn(List.of(entity));
    assertEquals(1, s.getAllAcademicPeriods().size());
    assertEquals(3L, s.getAcademicPeriodById(3L).getId());
    assertEquals(3L, s.getAcademicPeriodByCode("2026-1").getId());
    assertEquals(1, s.getAcademicPeriodsByYear(2026).size());
    assertEquals(1, s.getAcademicPeriodsByStatus(AcademicPeriodStatus.ABIERTO).size());
    assertEquals("2026-1", s.updateAcademicPeriod(3L, r).getCode());
    s.deleteAcademicPeriod(3L);
    verify(repo).delete(entity);
    rejects(404, () -> s.getAcademicPeriodById(99L));
    rejects(404, () -> s.getAcademicPeriodByCode("unknown"));
    rejects(404, () -> s.closeAcademicPeriod(99L));
  }

  @ParameterizedTest
  @CsvSource({"1999,1", "10000,1", "2026,0", "2026,3", ",1", "2026,"})
  void invalidYearOrSemester(Integer year, Integer semester) {
    r.setYear(year);
    r.setSemester(semester);
    rejects(400, () -> s.createAcademicPeriod(r));
    verify(repo, never()).save(any());
  }

  @Test
  void rejectsDuplicatesChangesWithHistoryAndClosedPeriods() {
    when(repo.findByYearAndSemester(2026, 1)).thenReturn(Optional.of(entity));
    rejects(409, () -> s.createAcademicPeriod(r));
    when(repo.findByYearAndSemester(2026, 1)).thenReturn(Optional.empty());
    var other = period();
    other.setId(99L);
    when(repo.findByCode("2026-1")).thenReturn(Optional.of(other));
    rejects(409, () -> s.createAcademicPeriod(r));
    rejects(409, () -> s.updateAcademicPeriod(3L, r));
    r.setYear(2027);
    when(sections.existsByAcademicPeriodId(3L)).thenReturn(true);
    rejects(409, () -> s.updateAcademicPeriod(3L, r));
    rejects(409, () -> s.deleteAcademicPeriod(3L));
    when(sections.existsByAcademicPeriodId(3L)).thenReturn(false);
    when(enrollments.existsByAcademicPeriodId(3L)).thenReturn(true);
    rejects(409, () -> s.deleteAcademicPeriod(3L));
    when(enrollments.existsByAcademicPeriodId(3L)).thenReturn(false);
    when(matriculations.existsByAcademicPeriodId(3L)).thenReturn(true);
    rejects(409, () -> s.deleteAcademicPeriod(3L));
    entity.setStatus(AcademicPeriodStatus.CERRADO);
    rejects(409, () -> s.updateAcademicPeriod(3L, r));
    rejects(409, () -> s.deleteAcademicPeriod(3L));
    rejects(409, () -> s.closeAcademicPeriod(3L));
  }

  @Test
  void cannotClosePendingSelectionsOrMissingGrades() {
    var e = enrollment();
    when(enrollments.findByAcademicPeriodId(3L)).thenReturn(List.of(e));
    rejects(409, () -> s.closeAcademicPeriod(3L));
    when(enrollments.isSelectionConfirmed(5L, 3L)).thenReturn(true);
    rejects(409, () -> s.closeAcademicPeriod(3L));
    verify(repo, never()).save(any());
    e.setStatus(EnrollmentStatus.CANCELADO);
    assertEquals(AcademicPeriodStatus.CERRADO, s.closeAcademicPeriod(3L).getStatus());
  }

  @Test
  void closingGraduatesStudentAndInactivatesSections() {
    var st = student();
    var sec = section();
    var g = grade();
    when(matriculations.findByAcademicPeriodId(3L))
        .thenReturn(List.of(new PeriodMatriculationEntity(1L, st, entity, true)));
    when(grades.findByEnrollment_Student_Id(5L)).thenReturn(List.of(g));
    when(enrollments.isSelectionConfirmed(5L, 3L)).thenReturn(true);
    when(subjects.findByStudyPlanId(2L)).thenReturn(List.of(subject()));
    when(sections.findByAcademicPeriodId(3L)).thenReturn(List.of(sec));
    assertEquals(AcademicPeriodStatus.CERRADO, s.closeAcademicPeriod(3L).getStatus());
    assertEquals(StudentStatus.EGRESADO, st.getStatus());
    assertEquals(Status.INACTIVA, sec.getStatus());
    verify(students).save(st);
    verify(sections).saveAll(List.of(sec));
  }

  @Test
  void repeatedFailuresEliminateStudent() {
    var st = student();
    var g = grade();
    g.setResult(GradeResult.REPROBADO);
    var previous = grade();
    previous.setResult(GradeResult.REPROBADO);
    previous.getEnrollment().getAcademicPeriod().setId(99L);
    previous.getEnrollment().getAcademicPeriod().setStatus(AcademicPeriodStatus.CERRADO);
    when(matriculations.findByAcademicPeriodId(3L))
        .thenReturn(List.of(new PeriodMatriculationEntity(1L, st, entity, true)));
    when(grades.findByEnrollment_Student_Id(5L)).thenReturn(List.of(g, previous));
    when(enrollments.isSelectionConfirmed(eq(5L), anyLong())).thenReturn(true);
    s.closeAcademicPeriod(3L);
    assertEquals(StudentStatus.ELIMINADO, st.getStatus());
    verify(students).save(st);
  }

  @Test
  void ignoresUnconfirmedCancelledAndOtherOpenGrades() {
    var st = student();
    var g = grade();
    g.getEnrollment().getAcademicPeriod().setId(99L);
    var cancelled = grade();
    cancelled.getEnrollment().setStatus(EnrollmentStatus.CANCELADO);
    when(matriculations.findByAcademicPeriodId(3L))
        .thenReturn(List.of(new PeriodMatriculationEntity(1L, st, entity, true)));
    when(grades.findByEnrollment_Student_Id(5L)).thenReturn(List.of(g, cancelled));
    when(subjects.findByStudyPlanId(2L)).thenReturn(List.of(subject()));
    s.closeAcademicPeriod(3L);
    assertEquals(StudentStatus.REGULAR, st.getStatus());
    entity.setStatus(AcademicPeriodStatus.ABIERTO);
    when(enrollments.isSelectionConfirmed(eq(5L), anyLong())).thenReturn(true);
    s.closeAcademicPeriod(3L);
    verify(students, never()).save(any());
  }

  @Test
  void preservesFinalStudentStatusesAndEmptyPlans() {
    var st = student();
    when(matriculations.findByAcademicPeriodId(3L))
        .thenReturn(List.of(new PeriodMatriculationEntity(1L, st, entity, true)));
    for (var status :
        List.of(StudentStatus.EGRESADO, StudentStatus.ELIMINADO, StudentStatus.REGULAR)) {
      entity.setStatus(AcademicPeriodStatus.ABIERTO);
      st.setStatus(status);
      s.closeAcademicPeriod(3L);
      assertEquals(status, st.getStatus());
    }
    verify(students, never()).save(any());
  }
}
