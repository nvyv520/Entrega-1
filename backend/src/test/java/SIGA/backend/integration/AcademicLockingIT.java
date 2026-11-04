package SIGA.backend.integration;

import static SIGA.backend.services.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import SIGA.backend.dtos.*;
import SIGA.backend.entities.*;
import SIGA.backend.services.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class AcademicLockingIT {
  @Autowired EntityManager em;
  @Autowired PlatformTransactionManager transactions;
  @Autowired EnrollmentService enrollments;
  @Autowired SectionService sections;
  @Autowired GradeService grades;
  @Autowired StudyPlanService plans;
  Long alternatePlanId;
  Long careerId, planId, periodId, studentId, teacherId, subjectId, sectionId, enrollmentId;

  TransactionTemplate transaction(boolean separate) {
    var template = new TransactionTemplate(transactions);
    if (separate) template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return template;
  }

  @BeforeEach
  void seed() {
    transaction(false)
        .executeWithoutResult(
            status -> {
              var career = career();
              career.setId(null);
              career.setCode("LOCK");
              em.persist(career);
              careerId = career.getId();
              var plan = plan();
              plan.setId(null);
              plan.setCareer(career);
              em.persist(plan);
              planId = plan.getId();
              var period = period();
              period.setId(null);
              em.persist(period);
              periodId = period.getId();
              var student = student();
              student.setId(null);
              student.setCareer(career);
              student.setStudyPlan(plan);
              em.persist(student);
              studentId = student.getId();
              var teacher = teacher();
              teacher.setId(null);
              teacher.setKeycloakSubject("locking-teacher");
              em.persist(teacher);
              teacherId = teacher.getId();
              var subject = subject();
              subject.setId(null);
              subject.setStudyPlan(plan);
              em.persist(subject);
              subjectId = subject.getId();
              var section = section();
              section.setId(null);
              section.setVersion(null);
              section.setSubject(subject);
              section.setAcademicPeriod(period);
              section.setTeacher(teacher);
              em.persist(section);
              sectionId = section.getId();
              em.persist(new PeriodMatriculationEntity(null, student, period, false));
            });
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
    transaction(false)
        .executeWithoutResult(
            status -> {
              em.createQuery("delete from GradeEntity g where g.enrollment.student.id = :id")
                  .setParameter("id", studentId)
                  .executeUpdate();
              em.createQuery("delete from EnrollmentEntity e where e.student.id = :id")
                  .setParameter("id", studentId)
                  .executeUpdate();
              em.createQuery("delete from PeriodMatriculationEntity m where m.student.id = :id")
                  .setParameter("id", studentId)
                  .executeUpdate();
              em.remove(em.find(SectionEntity.class, sectionId));
              em.remove(em.find(StudentEntity.class, studentId));
              em.remove(em.find(SubjectEntity.class, subjectId));
              em.remove(em.find(TeacherEntity.class, teacherId));
              em.remove(em.find(AcademicPeriodEntity.class, periodId));
              em.remove(em.find(StudyPlanEntity.class, planId));
              if (alternatePlanId != null)
                em.remove(em.find(StudyPlanEntity.class, alternatePlanId));
              em.remove(em.find(CareerEntity.class, careerId));
            });
  }

  EnrollmentRequestDTO enrollmentRequest() {
    var request = new EnrollmentRequestDTO();
    request.setStudentId(studentId);
    request.setSectionId(sectionId);
    return request;
  }

  void closeInAnotherTransaction() {
    transaction(true)
        .executeWithoutResult(
            status ->
                em.find(AcademicPeriodEntity.class, periodId)
                    .setStatus(AcademicPeriodStatus.CERRADO));
  }

  @Test
  void enrollmentRejectsPeriodClosedAfterAnEarlierRead() {
    transaction(false)
        .executeWithoutResult(
            status -> {
              try {
                assertEquals(
                    AcademicPeriodStatus.ABIERTO,
                    em.find(AcademicPeriodEntity.class, periodId).getStatus());
                closeInAnotherTransaction();
                rejects(409, () -> enrollments.createEnrollment(enrollmentRequest()));
              } finally {
                status.setRollbackOnly();
              }
            });
  }

  @Test
  void sectionUpdateRejectsPeriodClosedAfterAnEarlierRead() {
    transaction(false)
        .executeWithoutResult(
            status -> {
              try {
                em.find(SectionEntity.class, sectionId).getAcademicPeriod().getStatus();
                closeInAnotherTransaction();
                var request = sectionRequest();
                request.setSubjectId(subjectId);
                request.setAcademicPeriodId(periodId);
                request.setTeacherId(teacherId);
                rejects(409, () -> sections.updateSection(sectionId, request));
              } finally {
                status.setRollbackOnly();
              }
            });
  }

  @Test
  void gradeRejectsPeriodClosedAfterAnEarlierRead() {
    transaction(false)
        .executeWithoutResult(
            status -> {
              var enrollment = enrollment();
              enrollment.setId(null);
              enrollment.setStudent(em.find(StudentEntity.class, studentId));
              enrollment.setSection(em.find(SectionEntity.class, sectionId));
              enrollment.setAcademicPeriod(em.find(AcademicPeriodEntity.class, periodId));
              em.persist(enrollment);
              enrollmentId = enrollment.getId();
              em.createQuery(
                      "update PeriodMatriculationEntity m set m.enrollmentConfirmed = true where"
                          + " m.student.id = :id")
                  .setParameter("id", studentId)
                  .executeUpdate();
            });
    var jwt = Jwt.withTokenValue("test").header("alg", "RS256").subject("locking-teacher").build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
    transaction(false)
        .executeWithoutResult(
            status -> {
              try {
                em.find(AcademicPeriodEntity.class, periodId);
                closeInAnotherTransaction();
                var request = new GradeRequestDTO();
                request.setEnrollmentId(enrollmentId);
                request.setFinalGrade(new BigDecimal("5.0"));
                rejects(409, () -> grades.createGrade(request));
              } finally {
                status.setRollbackOnly();
              }
            });
  }

  @Test
  void enrollmentRejectsStudentStatusChangedAfterAnEarlierRead() {
    transaction(false)
        .executeWithoutResult(
            status -> {
              try {
                assertEquals(
                    StudentStatus.REGULAR, em.find(StudentEntity.class, studentId).getStatus());
                transaction(true)
                    .executeWithoutResult(
                        other ->
                            em.find(StudentEntity.class, studentId)
                                .setStatus(StudentStatus.POSTERGACION));
                rejects(409, () -> enrollments.createEnrollment(enrollmentRequest()));
              } finally {
                status.setRollbackOnly();
              }
            });
  }

  @Test
  void enrollmentRejectsSectionInactivatedAfterAnEarlierRead() {
    transaction(false)
        .executeWithoutResult(
            status -> {
              try {
                assertEquals(Status.ACTIVA, em.find(SectionEntity.class, sectionId).getStatus());
                transaction(true)
                    .executeWithoutResult(
                        other ->
                            em.find(SectionEntity.class, sectionId).setStatus(Status.INACTIVA));
                rejects(409, () -> enrollments.createEnrollment(enrollmentRequest()));
              } finally {
                status.setRollbackOnly();
              }
            });
  }

  @ParameterizedTest
  @EnumSource(StudyPlanStatus.class)
  void planStatusUsesTheLatestStateAfterAnotherTransactionChangesIt(StudyPlanStatus requested) {
    transaction(false)
        .executeWithoutResult(
            status -> em.find(StudyPlanEntity.class, planId).setStatus(requested));
    transaction(false)
        .executeWithoutResult(
            status -> {
              assertEquals(requested, em.find(StudyPlanEntity.class, planId).getStatus());
              transaction(true)
                  .executeWithoutResult(
                      other -> {
                        var changed =
                            requested == StudyPlanStatus.VIGENTE
                                ? StudyPlanStatus.NO_VIGENTE
                                : StudyPlanStatus.VIGENTE;
                        em.find(StudyPlanEntity.class, planId).setStatus(changed);
                        var alternate =
                            new StudyPlanEntity(
                                null,
                                "alternate",
                                requested,
                                em.find(CareerEntity.class, careerId));
                        em.persist(alternate);
                        alternatePlanId = alternate.getId();
                      });
              plans.updateStudyPlanStatus(planId, requested);
            });
    transaction(false)
        .executeWithoutResult(
            status -> {
              assertEquals(requested, em.find(StudyPlanEntity.class, planId).getStatus());
              assertEquals(
                  StudyPlanStatus.NO_VIGENTE,
                  em.find(StudyPlanEntity.class, alternatePlanId).getStatus());
            });
  }
}
