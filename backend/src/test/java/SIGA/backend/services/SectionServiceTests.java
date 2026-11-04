package SIGA.backend.services;

import static SIGA.backend.services.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import SIGA.backend.dtos.*;
import SIGA.backend.entities.*;
import SIGA.backend.repositories.*;
import jakarta.persistence.*;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SectionServiceTests {
  SectionRepository repo;
  SubjectRepository subjects;
  TeacherRepository teachers;
  EnrollmentRepository enrollments;
  EntityManager em;
  SectionService s;
  SectionEntity entity;

  @BeforeEach
  void setup() {
    repo = mock(SectionRepository.class);
    subjects = mock(SubjectRepository.class);
    teachers = mock(TeacherRepository.class);
    enrollments = mock(EnrollmentRepository.class);
    em = mock(EntityManager.class);
    s =
        new SectionService(
            repo, subjects, teachers, mock(AcademicPeriodRepository.class), enrollments, em);
    entity = section();
    when(repo.findById(10L)).thenReturn(Optional.of(entity));
    when(subjects.findById(8L)).thenReturn(Optional.of(entity.getSubject()));
    when(teachers.findById(7L)).thenReturn(Optional.of(entity.getTeacher()));
    when(em.find(AcademicPeriodEntity.class, 3L, LockModeType.PESSIMISTIC_WRITE))
        .thenReturn(entity.getAcademicPeriod());
    when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  void lifecycleAndQueries() {
    assertEquals(Status.ACTIVA, s.createSection(sectionRequest()).getStatus());
    when(repo.findAll()).thenReturn(List.of(entity));
    when(repo.findBySubjectId(8L)).thenReturn(List.of(entity));
    when(repo.findByAcademicPeriodId(3L)).thenReturn(List.of(entity));
    when(repo.findByTeacherId(7L)).thenReturn(List.of(entity));
    when(repo.findByStatus(Status.ACTIVA)).thenReturn(List.of(entity));
    assertEquals(1, s.getAllSections().size());
    assertEquals(10L, s.getSectionById(10L).getId());
    assertEquals(1, s.getSectionsBySubject(8L).size());
    assertEquals(1, s.getSectionsByAcademicPeriod(3L).size());
    assertEquals(1, s.getSectionsByTeacher(7L).size());
    assertEquals(1, s.getSectionsByStatus(Status.ACTIVA).size());
    assertEquals(30, s.updateSection(10L, sectionRequest()).getCapacity());
    s.deleteSection(10L);
    verify(repo).delete(entity);
    rejects(404, () -> s.getSectionById(99L));
  }

  static List<Consumer<SectionRequestDTO>> invalidData() {
    return List.of(
        r -> r.setSubjectId(null),
        r -> r.setTeacherId(null),
        r -> r.setAcademicPeriodId(null),
        r -> r.setCapacity(null),
        r -> r.setCapacity(0),
        r -> r.setCapacity(61),
        r -> r.setBlocks(null),
        r -> r.setBlocks(List.of()),
        r -> r.setBlocks(List.of("L1", "L1")),
        r -> r.setBlocks(Collections.singletonList(null)),
        r -> r.setBlocks(List.of("X1")));
  }

  @ParameterizedTest
  @MethodSource("invalidData")
  void rejectsInvalidData(Consumer<SectionRequestDTO> change) {
    var r = sectionRequest();
    change.accept(r);
    rejects(400, () -> s.createSection(r));
    verify(repo, never()).save(any());
  }

  @Test
  void rejectsNullMissingReferencesAndClosedPeriod() {
    rejects(400, () -> s.createSection(null));
    var r = sectionRequest();
    r.setSubjectId(99L);
    rejects(404, () -> s.createSection(r));
    r.setSubjectId(8L);
    r.setAcademicPeriodId(99L);
    rejects(404, () -> s.createSection(r));
    r.setAcademicPeriodId(3L);
    r.setTeacherId(99L);
    rejects(404, () -> s.createSection(r));
    entity.getAcademicPeriod().setStatus(AcademicPeriodStatus.CERRADO);
    rejects(409, () -> s.createSection(sectionRequest()));
  }

  @Test
  void validatesHoursTeacherAndSchedule() {
    var r = sectionRequest();
    entity.getTeacher().setStatus(Status.INACTIVA);
    rejects(409, () -> s.createSection(r));
    entity.getTeacher().setStatus(Status.ACTIVA);
    when(repo.existsBySubjectIdAndAcademicPeriodId(8L, 3L)).thenReturn(true);
    rejects(409, () -> s.createSection(r));
    when(repo.existsBySubjectIdAndAcademicPeriodId(8L, 3L)).thenReturn(false);
    entity.getSubject().setTheoryHours(3);
    rejects(400, () -> s.createSection(r));
    entity.getSubject().setTheoryHours(4);
    rejects(400, () -> s.createSection(r));
    entity.getSubject().setTheoryHours(2);
    when(repo.findByTeacherId(7L)).thenReturn(List.of(entity));
    rejects(409, () -> s.createSection(r));
    var other = section();
    other.getAcademicPeriod().setId(99L);
    when(repo.findByTeacherId(7L)).thenReturn(List.of(other));
    assertEquals(30, s.createSection(r).getCapacity());
  }

  @Test
  void enrolledSectionsCannotChangeOrBeDeleted() {
    when(enrollments.findBySectionId(10L)).thenReturn(List.of(enrollment()));
    rejects(409, () -> s.updateSection(10L, sectionRequest()));
    rejects(409, () -> s.deleteSection(10L));
    var r = sectionRequest();
    r.setSubjectId(99L);
    rejects(409, () -> s.updateSection(10L, r));
  }

  @Test
  void teacherOnlySeesAssignedConfirmedStudents() {
    when(teachers.findByKeycloakSubject("teacher")).thenReturn(Optional.of(entity.getTeacher()));
    when(repo.findByTeacherId(7L)).thenReturn(List.of(entity));
    assertEquals(1, s.getCurrentTeacherSections("teacher").size());
    rejects(404, () -> s.getCurrentTeacherSections("unknown"));
    when(enrollments.findBySectionIdAndStatus(10L, EnrollmentStatus.ACTIVO))
        .thenReturn(List.of(enrollment()));
    assertTrue(s.getCurrentTeacherSectionStudents("teacher", 10L).isEmpty());
    when(enrollments.isSelectionConfirmed(5L, 3L)).thenReturn(true);
    assertEquals(1, s.getCurrentTeacherSectionStudents("teacher", 10L).size());
    entity.setTeacher(teacher());
    entity.getTeacher().setId(99L);
    rejects(403, () -> s.findCurrentTeacherSection("teacher", 10L));
  }
}
