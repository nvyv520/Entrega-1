package SIGA.backend.services;

import static SIGA.backend.services.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import SIGA.backend.entities.*;
import SIGA.backend.repositories.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.*;

class StudentServiceTests {
  StudentRepository repo;
  CareerRepository careers;
  StudyPlanRepository plans;
  EnrollmentRepository enrollments;
  PeriodMatriculationRepository matriculations;
  GradeRepository grades;
  TeacherRepository teachers;
  StudentRepository students;
  SectionRepository sections;
  KeycloakAdminService keycloak;
  StudentService s;
  StudentEntity entity;

  @BeforeEach
  void setup() {
    repo = mock(StudentRepository.class);
    careers = mock(CareerRepository.class);
    plans = mock(StudyPlanRepository.class);
    enrollments = mock(EnrollmentRepository.class);
    matriculations = mock(PeriodMatriculationRepository.class);
    grades = mock(GradeRepository.class);
    teachers = mock(TeacherRepository.class);
    students = mock(StudentRepository.class);
    sections = mock(SectionRepository.class);
    keycloak = mock(KeycloakAdminService.class);
    s =
        new StudentService(
            careers, plans, repo, enrollments, matriculations, grades, teachers, keycloak);
    entity = student();
    when(repo.findById(5L)).thenReturn(Optional.of(entity));
    when(repo.findByIdForUpdate(5L)).thenReturn(Optional.of(entity));
    when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    when(repo.findByKeycloakSubject("account")).thenReturn(Optional.of(entity));
    when(careers.findById(1L)).thenReturn(Optional.of(career()));
    when(careers.existsById(1L)).thenReturn(true);
    when(plans.findByCareerIdAndStatus(1L, StudyPlanStatus.VIGENTE))
        .thenReturn(Optional.of(plan()));
    when(keycloak.createUser(anyString(), anyString(), anyString(), eq("STUDENT")))
        .thenReturn(
            new KeycloakAdminService.CreatedUser(
                "00000000-0000-0000-0000-000000000001", "login", "temporary", "registration"));
  }

  @AfterEach
  void cleanup() {
    if (TransactionSynchronizationManager.isSynchronizationActive())
      TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  void queriesAndUpdates() {
    when(repo.findAll()).thenReturn(List.of(entity));
    when(repo.findByRut(entity.getRut())).thenReturn(Optional.of(entity));
    assertEquals(1, s.getAllStudents().size());
    assertEquals(5L, s.getStudentById(5L).getId());
    assertEquals(5L, s.getStudentByRut(" " + entity.getRut() + " ").getId());
    assertEquals(5L, s.getCurrentStudent("account").getId());
    when(repo.findByCareerId(1L)).thenReturn(List.of(entity));
    assertEquals(1, s.getStudentsByCareerId(1L).size());
    rejects(404, () -> s.getStudentsByCareerId(99L));
    when(repo.findByStatus(StudentStatus.REGULAR)).thenReturn(List.of(entity));
    assertEquals(1, s.getStudentsByStatus(StudentStatus.REGULAR).size());
    when(repo.findByNormalizedRut("123456789")).thenReturn(List.of(entity));
    when(repo.findByEmail(entity.getEmail())).thenReturn(Optional.of(entity));
    assertEquals(entity.getEmail(), s.updateStudent(5L, studentRequest()).getEmail());
    rejects(404, () -> s.getStudentById(99L));
    rejects(404, () -> s.getStudentByRut("unknown"));
    rejects(404, () -> s.getCurrentStudent("unknown"));
    rejects(404, () -> s.updateStudent(99L, studentRequest()));
  }

  @Test
  void registrationCreatesAccountAndCompensatesRollback() {
    TransactionSynchronizationManager.initSynchronization();
    var result = s.createStudent(studentRequest());
    assertEquals("login", result.getUsername());
    assertEquals("temporary", result.getTemporaryPassword());
    assertEquals("REGULAR", result.getStudent().getStatus().name());
    var sync = TransactionSynchronizationManager.getSynchronizations().get(0);
    sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    verify(keycloak, never()).deleteCreatedUser(any());
    sync.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
    sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
    verify(keycloak).deleteCreatedUser(any());
    doThrow(new IllegalStateException("simulated cleanup failure"))
        .when(keycloak)
        .deleteCreatedUser(any());
    assertDoesNotThrow(() -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
  }

  @Test
  void duplicateRutAndEmailCannotPersist() {
    var other = student();
    other.setId(99L);
    when(repo.findByNormalizedRut("123456789")).thenReturn(List.of(other));
    rejects(409, () -> s.createStudent(studentRequest()));
    rejects(409, () -> s.updateStudent(5L, studentRequest()));
    when(repo.findByNormalizedRut("123456789")).thenReturn(List.of());
    when(repo.existsByEmail(entity.getEmail())).thenReturn(true);
    rejects(409, () -> s.createStudent(studentRequest()));
    when(repo.findByEmail(entity.getEmail())).thenReturn(Optional.of(other));
    rejects(409, () -> s.updateStudent(5L, studentRequest()));
    verify(keycloak, never()).createUser(anyString(), anyString(), anyString(), anyString());
    verify(repo, never()).save(any());
  }

  @Test
  void statusAndDeletionRules() {
    rejects(400, () -> s.updateStudentStatus(5L, null));
    assertEquals(
        StudentStatus.POSTERGACION,
        s.updateStudentStatus(5L, StudentStatus.POSTERGACION).getStatus());
    rejects(400, () -> s.updateStudentStatus(5L, StudentStatus.EGRESADO));
    entity.setStatus(StudentStatus.EGRESADO);
    rejects(409, () -> s.updateStudentStatus(5L, StudentStatus.REGULAR));
    s.deleteStudent(5L);
    verify(repo).delete(entity);
    when(enrollments.existsByStudentId(5L)).thenReturn(true);
    rejects(409, () -> s.deleteStudent(5L));
    when(enrollments.existsByStudentId(5L)).thenReturn(false);
    when(matriculations.existsByStudentId(5L)).thenReturn(true);
    rejects(409, () -> s.deleteStudent(5L));
  }

  @Test
  void accountLinkValidatesFormatOwnershipAndRole() {
    String id = "00000000-0000-0000-0000-000000000001";
    rejects(400, () -> s.linkStudentKeycloak(5L, null));
    rejects(400, () -> s.linkStudentKeycloak(5L, " "));
    rejects(400, () -> s.linkStudentKeycloak(5L, "bad"));
    var other = student();
    other.setId(99L);
    when(repo.findByKeycloakSubject(id)).thenReturn(Optional.of(other));
    rejects(409, () -> s.linkStudentKeycloak(5L, id));
    when(repo.findByKeycloakSubject(id)).thenReturn(Optional.of(entity));
    entity.setKeycloakSubject("other");
    rejects(409, () -> s.linkStudentKeycloak(5L, id));
    entity.setKeycloakSubject(null);
    when(teachers.findByKeycloakSubject(id)).thenReturn(Optional.of(teacher()));
    rejects(409, () -> s.linkStudentKeycloak(5L, id));
    when(teachers.findByKeycloakSubject(id)).thenReturn(Optional.empty());
    s.linkStudentKeycloak(5L, id);
    assertEquals(id, entity.getKeycloakSubject());
    verify(keycloak).validateUserRole(id, "STUDENT");
  }

  @Test
  void historyOnlyIncludesConfirmedGradedClosedPeriods() {
    var e = enrollment();
    when(enrollments.findByStudentId(5L)).thenReturn(List.of(e));
    assertTrue(s.getStudentAcademicHistory(5L).isEmpty());
    e.getAcademicPeriod().setStatus(AcademicPeriodStatus.CERRADO);
    assertTrue(s.getStudentAcademicHistory(5L).isEmpty());
    when(enrollments.isSelectionConfirmed(5L, 3L)).thenReturn(true);
    assertTrue(s.getStudentAcademicHistory(5L).isEmpty());
    when(grades.findByEnrollmentId(20L)).thenReturn(Optional.of(grade()));
    assertEquals(GradeResult.APROBADO, s.getStudentAcademicHistory(5L).get(0).getResult());
    assertEquals(1, s.getCurrentStudentAcademicHistory("account").size());
    rejects(404, () -> s.getCurrentStudentAcademicHistory("unknown"));
  }

  @Test
  void validatesCareerPlanChangesAndAcademicHistory() {
    var r = studentRequest();
    when(careers.findById(1L)).thenReturn(Optional.empty());
    rejects(404, () -> s.createStudent(r));
    rejects(404, () -> s.updateStudent(5L, r));
    when(careers.findById(1L)).thenReturn(Optional.of(career()));
    var inactive = career();
    inactive.setStatus(Status.INACTIVA);
    when(careers.findById(1L)).thenReturn(Optional.of(inactive));
    rejects(409, () -> s.createStudent(r));
    when(careers.findById(1L)).thenReturn(Optional.of(career()));
    when(plans.findByCareerIdAndStatus(1L, StudyPlanStatus.VIGENTE)).thenReturn(Optional.empty());
    rejects(409, () -> s.createStudent(r));
    r.setStudyPlanId(99L);
    rejects(404, () -> s.updateStudent(5L, r));
    var other = plan();
    other.setId(99L);
    when(plans.findById(99L)).thenReturn(Optional.of(other));
    when(enrollments.existsByStudentId(5L)).thenReturn(true);
    rejects(409, () -> s.updateStudent(5L, r));
    when(enrollments.existsByStudentId(5L)).thenReturn(false);
    assertEquals(99L, s.updateStudent(5L, r).getStudyPlanId());
    other.setCareer(new CareerEntity(99L, "OTH", "Other", "Other", Status.ACTIVA, 4));
    rejects(400, () -> s.updateStudent(5L, r));
    r.setStudyPlanId(null);
    r.setCareerId(99L);
    var c = other.getCareer();
    c.setStatus(Status.INACTIVA);
    when(careers.findById(99L)).thenReturn(Optional.of(c));
    rejects(409, () -> s.updateStudent(5L, r));
    c.setStatus(Status.ACTIVA);
    when(plans.findByCareerIdAndStatus(99L, StudyPlanStatus.VIGENTE))
        .thenReturn(Optional.of(other));
    assertEquals(99L, s.updateStudent(5L, r).getCareerId());
  }
}
