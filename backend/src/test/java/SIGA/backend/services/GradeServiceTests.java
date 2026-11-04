package SIGA.backend.services;

import static SIGA.backend.services.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import SIGA.backend.dtos.*;
import SIGA.backend.entities.*;
import SIGA.backend.repositories.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class GradeServiceTests {
  GradeRepository repo;
  EnrollmentRepository enrollments;
  TeacherRepository teachers;
  StudentRepository students;
  EntityManager em;
  SectionService sections;
  GradeService s;
  GradeEntity entity;
  GradeRequestDTO r;

  @BeforeEach
  void setup() {
    repo = mock(GradeRepository.class);
    enrollments = mock(EnrollmentRepository.class);
    teachers = mock(TeacherRepository.class);
    students = mock(StudentRepository.class);
    em = mock(EntityManager.class);
    sections = mock(SectionService.class);
    s = new GradeService(repo, enrollments, em, teachers, students, sections);
    entity = grade();
    r = new GradeRequestDTO();
    r.setEnrollmentId(20L);
    r.setFinalGrade(new BigDecimal("5.0"));
    when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    when(repo.findById(30L)).thenReturn(Optional.of(entity));
    when(enrollments.findById(20L)).thenReturn(Optional.of(entity.getEnrollment()));
    when(em.find(AcademicPeriodEntity.class, 3L, LockModeType.PESSIMISTIC_WRITE))
        .thenReturn(entity.getEnrollment().getAcademicPeriod());
    when(enrollments.isSelectionConfirmed(5L, 3L)).thenReturn(true);
    when(teachers.findByKeycloakSubject("teacher")).thenReturn(Optional.of(teacher()));
    account();
  }

  static void account() {
    var jwt = Jwt.withTokenValue("test").header("alg", "RS256").subject("teacher").build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void readsAndUpdates() {
    when(repo.findAll()).thenReturn(List.of(entity));
    when(repo.findByEnrollmentId(20L)).thenReturn(Optional.of(entity));
    when(repo.findByResult(GradeResult.APROBADO)).thenReturn(List.of(entity));
    when(repo.findByEnrollment_Student_Id(5L)).thenReturn(List.of(entity));
    when(repo.findByEnrollment_AcademicPeriod_Id(3L)).thenReturn(List.of(entity));
    when(students.findByKeycloakSubject("student")).thenReturn(Optional.of(student()));
    when(repo.findByEnrollment_Section_IdAndEnrollment_Status(10L, EnrollmentStatus.ACTIVO))
        .thenReturn(List.of(entity));
    assertEquals(1, s.getAllGrades().size());
    assertEquals(30L, s.getGradeById(30L).getId());
    assertEquals(30L, s.getGradeByEnrollment(20L).getId());
    assertEquals(1, s.getGradesByResult(GradeResult.APROBADO).size());
    assertEquals(1, s.getGradesByStudent(5L).size());
    assertEquals(1, s.getGradesByAcademicPeriod(3L).size());
    assertEquals(1, s.getCurrentStudentGrades("student").size());
    assertEquals(1, s.getCurrentTeacherSectionGrades("teacher", 10L).size());
    when(enrollments.isSelectionConfirmed(5L, 3L)).thenReturn(false);
    assertTrue(s.getCurrentStudentGrades("student").isEmpty());
    when(enrollments.isSelectionConfirmed(5L, 3L)).thenReturn(true);
    r.setFinalGrade(new BigDecimal("3.9"));
    assertEquals(GradeResult.REPROBADO, s.updateGrade(30L, r).getResult());
    assertEquals(GradeResult.REPROBADO, s.createGrade(r).getResult());
    r.setFinalGrade(new BigDecimal("4.0"));
    assertEquals(GradeResult.APROBADO, s.createGrade(r).getResult());
    rejects(404, () -> s.getGradeById(99L));
    rejects(404, () -> s.getGradeByEnrollment(99L));
    rejects(404, () -> s.getCurrentStudentGrades("unknown"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.9", "7.1", "4.55"})
  void rejectsInvalidGrade(String value) {
    r.setFinalGrade(new BigDecimal(value));
    rejects(400, () -> s.createGrade(r));
    verify(repo, never()).save(any());
  }

  @Test
  void rejectsMissingDataReferencesAndDuplicate() {
    rejects(400, () -> s.createGrade(null));
    rejects(400, () -> s.createGrade(new GradeRequestDTO()));
    r.setFinalGrade(null);
    rejects(400, () -> s.createGrade(r));
    r.setFinalGrade(new BigDecimal("5"));
    r.setEnrollmentId(99L);
    rejects(404, () -> s.createGrade(r));
    rejects(409, () -> s.updateGrade(30L, r));
    r.setEnrollmentId(20L);
    when(repo.existsByEnrollmentId(20L)).thenReturn(true);
    rejects(409, () -> s.createGrade(r));
  }

  @Test
  void rejectsUnauthenticatedUnknownTeacherAndInactiveEnrollment() {
    SecurityContextHolder.clearContext();
    rejects(401, () -> s.createGrade(r));
    account();
    when(teachers.findByKeycloakSubject("teacher")).thenReturn(Optional.empty());
    rejects(403, () -> s.createGrade(r));
    when(teachers.findByKeycloakSubject("teacher")).thenReturn(Optional.of(teacher()));
    entity.getEnrollment().setStatus(EnrollmentStatus.CANCELADO);
    rejects(409, () -> s.createGrade(r));
    entity.getEnrollment().setStatus(EnrollmentStatus.ACTIVO);
    entity.getEnrollment().getSection().setStatus(Status.INACTIVA);
    rejects(409, () -> s.createGrade(r));
    when(em.find(AcademicPeriodEntity.class, 3L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(null);
    rejects(404, () -> s.createGrade(r));
  }
}
