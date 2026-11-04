package SIGA.backend.integration;

import static SIGA.backend.services.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import SIGA.backend.dtos.*;
import SIGA.backend.entities.*;
import SIGA.backend.repositories.*;
import SIGA.backend.services.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AcademicProcessesIT {
  @Autowired EntityManager em;
  @Autowired EnrollmentService enrollmentService;
  @Autowired GradeService gradeService;
  @Autowired AcademicPeriodService periodService;
  @Autowired EnrollmentRepository enrollments;
  @Autowired GradeRepository grades;
  @Autowired SectionRepository sections;
  @Autowired StudentRepository students;
  @Autowired PeriodMatriculationRepository matriculations;
  StudentEntity student;
  AcademicPeriodEntity period;
  SectionEntity section;
  TeacherEntity teacher;

  @BeforeEach
  void seed() {
    var career = career();
    career.setId(null);
    em.persist(career);
    var plan = plan();
    plan.setId(null);
    plan.setCareer(career);
    em.persist(plan);
    student = student();
    student.setId(null);
    student.setCareer(career);
    student.setStudyPlan(plan);
    em.persist(student);
    teacher = teacher();
    teacher.setId(null);
    teacher.setKeycloakSubject("teacher");
    em.persist(teacher);
    period = period();
    period.setId(null);
    em.persist(period);
    var subject = subject();
    subject.setId(null);
    subject.setStudyPlan(plan);
    em.persist(subject);
    section = section();
    section.setId(null);
    section.setVersion(null);
    section.setSubject(subject);
    section.setTeacher(teacher);
    section.setAcademicPeriod(period);
    em.persist(section);
    em.flush();
    var jwt = Jwt.withTokenValue("test").header("alg", "RS256").subject("teacher").build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  EnrollmentRequestDTO request() {
    var r = new EnrollmentRequestDTO();
    r.setStudentId(student.getId());
    r.setSectionId(section.getId());
    return r;
  }

  EnrollmentEntity savedEnrollment(boolean confirmed) {
    var m = new PeriodMatriculationEntity(null, student, period, confirmed);
    em.persist(m);
    var e =
        new EnrollmentEntity(
            null, student, section, period, LocalDate.of(2026, 3, 1), EnrollmentStatus.ACTIVO);
    em.persist(e);
    em.flush();
    return e;
  }

  GradeRequestDTO gradeRequest(EnrollmentEntity e, String value) {
    var r = new GradeRequestDTO();
    r.setEnrollmentId(e.getId());
    r.setFinalGrade(new BigDecimal(value));
    return r;
  }

  void saveGrade(EnrollmentEntity e, String value, GradeResult result) {
    em.persist(new GradeEntity(null, e, new BigDecimal(value), result));
    em.flush();
  }

  void reload() {
    em.flush();
    em.clear();
  }

  @Test
  void enrollmentSuccessCreatesReservationAndInternalMatriculation() {
    var result = enrollmentService.createEnrollment(request());
    reload();
    assertEquals(
        EnrollmentStatus.ACTIVO, enrollments.findById(result.getId()).orElseThrow().getStatus());
    assertFalse(
        matriculations
            .findByStudentIdAndAcademicPeriodId(student.getId(), period.getId())
            .orElseThrow()
            .getEnrollmentConfirmed());
    assertEquals(1, sections.findById(section.getId()).orElseThrow().getEnrolledStudents());
  }

  @Test
  void enrollmentSuccessReactivatesCancelledReservation() {
    var e = savedEnrollment(false);
    e.setStatus(EnrollmentStatus.CANCELADO);
    em.flush();
    var result = enrollmentService.createEnrollment(request());
    reload();
    assertEquals(e.getId(), result.getId());
    assertEquals(1, enrollments.count());
    assertEquals(
        EnrollmentStatus.ACTIVO, enrollments.findById(e.getId()).orElseThrow().getStatus());
  }

  @Test
  void enrollmentSuccessCancelsAndReleasesSeat() {
    var e = savedEnrollment(true);
    section.setEnrolledStudents(1);
    em.flush();
    enrollmentService.cancelEnrollment(e.getId());
    reload();
    assertEquals(
        EnrollmentStatus.CANCELADO, enrollments.findById(e.getId()).orElseThrow().getStatus());
    assertEquals(0, sections.findById(section.getId()).orElseThrow().getEnrolledStudents());
    assertFalse(enrollments.isSelectionConfirmed(student.getId(), period.getId()));
  }

  @Test
  void enrollmentRejectionForNonRegularStudent() {
    student.setStatus(StudentStatus.POSTERGACION);
    em.flush();
    rejects(409, () -> enrollmentService.createEnrollment(request()));
    assertEquals(0, enrollments.count());
    assertEquals(0, matriculations.count());
  }

  @Test
  void enrollmentRejectionForClosedPeriod() {
    period.setStatus(AcademicPeriodStatus.CERRADO);
    em.flush();
    rejects(409, () -> enrollmentService.createEnrollment(request()));
    assertEquals(0, enrollments.count());
  }

  @Test
  void enrollmentRejectionWhenCapacityIsFull() {
    var other = student();
    other.setId(null);
    other.setRut("22.222.222-2");
    other.setEmail("other@example.cl");
    other.setCareer(student.getCareer());
    other.setStudyPlan(student.getStudyPlan());
    em.persist(other);
    section.setCapacity(1);
    em.persist(
        new EnrollmentEntity(
            null, other, section, period, LocalDate.of(2026, 3, 1), EnrollmentStatus.ACTIVO));
    em.flush();
    rejects(409, () -> enrollmentService.createEnrollment(request()));
    assertTrue(enrollments.findByStudentId(student.getId()).isEmpty());
    assertEquals(1, enrollments.count());
  }

  @Test
  void gradeSuccessPassesAtFour() {
    var e = savedEnrollment(true);
    var dto = gradeService.createGrade(gradeRequest(e, "4.0"));
    reload();
    assertEquals(GradeResult.APROBADO, grades.findById(dto.getId()).orElseThrow().getResult());
  }

  @Test
  void gradeSuccessFailsBelowFour() {
    var e = savedEnrollment(true);
    var dto = gradeService.createGrade(gradeRequest(e, "3.9"));
    reload();
    assertEquals(GradeResult.REPROBADO, grades.findById(dto.getId()).orElseThrow().getResult());
  }

  @Test
  void gradeSuccessUpdatesExistingGradeAndResult() {
    var e = savedEnrollment(true);
    saveGrade(e, "3.0", GradeResult.REPROBADO);
    var id = grades.findByEnrollmentId(e.getId()).orElseThrow().getId();
    gradeService.updateGrade(id, gradeRequest(e, "7.0"));
    reload();
    assertEquals(1, grades.count());
    assertEquals(GradeResult.APROBADO, grades.findById(id).orElseThrow().getResult());
    assertEquals(new BigDecimal("7.0"), grades.findById(id).orElseThrow().getFinalGrade());
  }

  @Test
  void gradeRejectionForUnconfirmedSelection() {
    var e = savedEnrollment(false);
    rejects(409, () -> gradeService.createGrade(gradeRequest(e, "5.0")));
    assertEquals(0, grades.count());
  }

  @Test
  void gradeRejectionForUnassignedTeacher() {
    var e = savedEnrollment(true);
    var jwt = Jwt.withTokenValue("test").header("alg", "RS256").subject("other").build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
    rejects(403, () -> gradeService.createGrade(gradeRequest(e, "5.0")));
    assertEquals(0, grades.count());
  }

  @Test
  void gradeRejectionForDuplicate() {
    var e = savedEnrollment(true);
    saveGrade(e, "4.5", GradeResult.APROBADO);
    rejects(409, () -> gradeService.createGrade(gradeRequest(e, "6.0")));
    assertEquals(1, grades.count());
    assertEquals(
        new BigDecimal("4.5"), grades.findByEnrollmentId(e.getId()).orElseThrow().getFinalGrade());
  }

  @Test
  void closingSuccessWithoutEnrollmentsInactivatesSections() {
    periodService.closeAcademicPeriod(period.getId());
    reload();
    assertEquals(
        AcademicPeriodStatus.CERRADO,
        em.find(AcademicPeriodEntity.class, period.getId()).getStatus());
    assertEquals(Status.INACTIVA, sections.findById(section.getId()).orElseThrow().getStatus());
  }

  @Test
  void closingSuccessGraduatesStudentWhoCompletedPlan() {
    var e = savedEnrollment(true);
    saveGrade(e, "5.0", GradeResult.APROBADO);
    periodService.closeAcademicPeriod(period.getId());
    reload();
    assertEquals(
        StudentStatus.EGRESADO, students.findById(student.getId()).orElseThrow().getStatus());
    assertEquals(Status.INACTIVA, sections.findById(section.getId()).orElseThrow().getStatus());
  }

  @Test
  void closingSuccessEliminatesStudentAfterSecondFailure() {
    var old = period();
    old.setId(null);
    old.setYear(2025);
    old.setCode("2025-1");
    old.setStatus(AcademicPeriodStatus.CERRADO);
    em.persist(old);
    var oldSection = section();
    oldSection.setId(null);
    oldSection.setVersion(null);
    oldSection.setSubject(section.getSubject());
    oldSection.setTeacher(teacher);
    oldSection.setAcademicPeriod(old);
    em.persist(oldSection);
    em.persist(new PeriodMatriculationEntity(null, student, old, true));
    var attempt =
        new EnrollmentEntity(
            null, student, oldSection, old, LocalDate.of(2025, 3, 1), EnrollmentStatus.ACTIVO);
    em.persist(attempt);
    saveGrade(attempt, "3.0", GradeResult.REPROBADO);
    var current = savedEnrollment(true);
    saveGrade(current, "3.9", GradeResult.REPROBADO);
    periodService.closeAcademicPeriod(period.getId());
    reload();
    assertEquals(
        StudentStatus.ELIMINADO, students.findById(student.getId()).orElseThrow().getStatus());
    assertEquals(2, grades.count());
  }

  @Test
  void closingRejectionForAlreadyClosedPeriod() {
    period.setStatus(AcademicPeriodStatus.CERRADO);
    em.flush();
    rejects(409, () -> periodService.closeAcademicPeriod(period.getId()));
    assertEquals(Status.ACTIVA, sections.findById(section.getId()).orElseThrow().getStatus());
  }

  @Test
  void closingRejectionForPendingConfirmation() {
    savedEnrollment(false);
    rejects(409, () -> periodService.closeAcademicPeriod(period.getId()));
    assertEquals(
        AcademicPeriodStatus.ABIERTO,
        em.find(AcademicPeriodEntity.class, period.getId()).getStatus());
    assertEquals(StudentStatus.REGULAR, student.getStatus());
  }

  @Test
  void closingRejectionForMissingGrades() {
    savedEnrollment(true);
    rejects(409, () -> periodService.closeAcademicPeriod(period.getId()));
    assertEquals(
        AcademicPeriodStatus.ABIERTO,
        em.find(AcademicPeriodEntity.class, period.getId()).getStatus());
    assertEquals(Status.ACTIVA, sections.findById(section.getId()).orElseThrow().getStatus());
  }
}
