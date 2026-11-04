package SIGA.backend.services;

import static SIGA.backend.services.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import SIGA.backend.entities.*;
import SIGA.backend.repositories.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.*;

class TeacherServiceTests {
  TeacherRepository repo;
  CareerRepository careers;
  StudyPlanRepository plans;
  EnrollmentRepository enrollments;
  PeriodMatriculationRepository matriculations;
  GradeRepository grades;
  TeacherRepository teachers;
  StudentRepository students;
  SectionRepository sections;
  KeycloakAdminService keycloak;
  TeacherService s;
  TeacherEntity entity;

  @BeforeEach
  void setup() {
    repo = mock(TeacherRepository.class);
    careers = mock(CareerRepository.class);
    plans = mock(StudyPlanRepository.class);
    enrollments = mock(EnrollmentRepository.class);
    matriculations = mock(PeriodMatriculationRepository.class);
    grades = mock(GradeRepository.class);
    teachers = mock(TeacherRepository.class);
    students = mock(StudentRepository.class);
    sections = mock(SectionRepository.class);
    keycloak = mock(KeycloakAdminService.class);
    s = new TeacherService(repo, sections, students, keycloak);
    entity = teacher();
    when(repo.findById(7L)).thenReturn(Optional.of(entity));
    when(repo.findByIdForUpdate(7L)).thenReturn(Optional.of(entity));
    when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    when(repo.findByKeycloakSubject("account")).thenReturn(Optional.of(entity));
    when(keycloak.createUser(anyString(), anyString(), anyString(), eq("TEACHER")))
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
    assertEquals(1, s.getAllTeachers().size());
    assertEquals(7L, s.getTeacherById(7L).getId());
    assertEquals(7L, s.getTeacherByRut(" " + entity.getRut() + " ").getId());
    assertEquals(7L, s.getCurrentTeacher("account").getId());
    when(repo.findByFirstNameContainsIgnoreCase("Luis")).thenReturn(List.of(entity));
    when(repo.findByLastNameContainsIgnoreCase("Soto")).thenReturn(List.of(entity));
    assertEquals(1, s.getTeachersByFirstName(" Luis ").size());
    assertEquals(1, s.getTeachersByLastName(" Soto ").size());
    when(repo.findByStatus(Status.ACTIVA)).thenReturn(List.of(entity));
    assertEquals(1, s.getTeachersByStatus(TeacherStatus.ACTIVO).size());
    when(repo.findByNormalizedRut("111111111")).thenReturn(List.of(entity));
    when(repo.findByEmail(entity.getEmail())).thenReturn(Optional.of(entity));
    assertEquals(entity.getEmail(), s.updateTeacher(7L, teacherRequest()).getEmail());
    rejects(404, () -> s.getTeacherById(99L));
    rejects(404, () -> s.getTeacherByRut("unknown"));
    rejects(404, () -> s.getCurrentTeacher("unknown"));
    rejects(404, () -> s.updateTeacher(99L, teacherRequest()));
  }

  @Test
  void registrationCreatesAccountAndCompensatesRollback() {
    TransactionSynchronizationManager.initSynchronization();
    var result = s.createTeacher(teacherRequest());
    assertEquals("login", result.getUsername());
    assertEquals("temporary", result.getTemporaryPassword());
    assertEquals("ACTIVO", result.getTeacher().getStatus().name());
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
    var other = teacher();
    other.setId(99L);
    when(repo.findByNormalizedRut("111111111")).thenReturn(List.of(other));
    rejects(409, () -> s.createTeacher(teacherRequest()));
    rejects(409, () -> s.updateTeacher(7L, teacherRequest()));
    when(repo.findByNormalizedRut("111111111")).thenReturn(List.of());
    when(repo.existsByEmail(entity.getEmail())).thenReturn(true);
    rejects(409, () -> s.createTeacher(teacherRequest()));
    when(repo.findByEmail(entity.getEmail())).thenReturn(Optional.of(other));
    rejects(409, () -> s.updateTeacher(7L, teacherRequest()));
    verify(keycloak, never()).createUser(anyString(), anyString(), anyString(), anyString());
    verify(repo, never()).save(any());
  }

  @Test
  void statusAndDeletionRules() {
    rejects(400, () -> s.updateTeacherStatus(7L, null));
    assertEquals(
        TeacherStatus.INACTIVO, s.updateTeacherStatus(7L, TeacherStatus.INACTIVO).getStatus());
    when(sections.existsByTeacherIdAndAcademicPeriod_Status(7L, AcademicPeriodStatus.ABIERTO))
        .thenReturn(true);
    rejects(409, () -> s.updateTeacherStatus(7L, TeacherStatus.INACTIVO));
    s.deleteTeacher(7L);
    verify(repo).delete(entity);
    when(sections.existsByTeacherId(7L)).thenReturn(true);
    rejects(409, () -> s.deleteTeacher(7L));
  }

  @Test
  void accountLinkValidatesFormatOwnershipAndRole() {
    String id = "00000000-0000-0000-0000-000000000001";
    rejects(400, () -> s.linkTeacherKeycloak(7L, null));
    rejects(400, () -> s.linkTeacherKeycloak(7L, " "));
    rejects(400, () -> s.linkTeacherKeycloak(7L, "bad"));
    var other = teacher();
    other.setId(99L);
    when(repo.findByKeycloakSubject(id)).thenReturn(Optional.of(other));
    rejects(409, () -> s.linkTeacherKeycloak(7L, id));
    when(repo.findByKeycloakSubject(id)).thenReturn(Optional.of(entity));
    entity.setKeycloakSubject("other");
    rejects(409, () -> s.linkTeacherKeycloak(7L, id));
    entity.setKeycloakSubject(null);
    when(students.findByKeycloakSubject(id)).thenReturn(Optional.of(student()));
    rejects(409, () -> s.linkTeacherKeycloak(7L, id));
    when(students.findByKeycloakSubject(id)).thenReturn(Optional.empty());
    s.linkTeacherKeycloak(7L, id);
    assertEquals(id, entity.getKeycloakSubject());
    verify(keycloak).validateUserRole(id, "TEACHER");
  }
}
