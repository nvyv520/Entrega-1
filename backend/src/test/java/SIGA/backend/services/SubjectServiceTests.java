package SIGA.backend.services;

import static SIGA.backend.services.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import SIGA.backend.dtos.*;
import SIGA.backend.entities.*;
import SIGA.backend.repositories.*;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SubjectServiceTests {
  SubjectRepository repo;
  StudyPlanRepository plans;
  SectionRepository sections;
  EnrollmentRepository enrollments;
  SubjectService s;
  SubjectEntity entity;

  @BeforeEach
  void setup() {
    repo = mock(SubjectRepository.class);
    plans = mock(StudyPlanRepository.class);
    sections = mock(SectionRepository.class);
    enrollments = mock(EnrollmentRepository.class);
    s = new SubjectService(repo, plans, sections, enrollments);
    entity = subject();
    when(repo.findById(8L)).thenReturn(Optional.of(entity));
    when(plans.findById(2L)).thenReturn(Optional.of(plan()));
    when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  void lifecycleAndQueries() {
    assertEquals("MAT", s.createSubject(subjectRequest()).getCode());
    when(repo.findAll()).thenReturn(List.of(entity));
    when(plans.existsById(2L)).thenReturn(true);
    when(repo.findByStudyPlanId(2L)).thenReturn(List.of(entity));
    assertEquals(1, s.getAllSubjects().size());
    assertEquals("MAT", s.getSubjectById(8L).getCode());
    assertEquals(1, s.getSubjectByStudyPlanId(2L).size());
    when(repo.findByStudyPlanIdAndCode(2L, "MAT")).thenReturn(Optional.of(entity));
    assertEquals("Matemáticas", s.updateSubject(8L, subjectRequest()).getName());
    s.deleteSubject(8L);
    verify(repo).delete(entity);
    rejects(404, () -> s.getSubjectById(99L));
    rejects(404, () -> s.getSubjectByStudyPlanId(99L));
    when(plans.findById(2L)).thenReturn(Optional.empty());
    rejects(404, () -> s.createSubject(subjectRequest()));
  }

  static List<Consumer<SubjectRequestDTO>> invalidData() {
    return List.of(
        r -> r.setCode(null),
        r -> r.setCode(" "),
        r -> r.setName(null),
        r -> r.setName(" "),
        r -> r.setStudyPlanId(null),
        r -> r.setCode("x".repeat(21)),
        r -> r.setName("x".repeat(101)),
        r -> r.setSemester(null),
        r -> r.setSemester(0),
        r -> r.setSemester(5),
        r -> r.setSct(null),
        r -> r.setSct(0),
        r -> r.setSct(8),
        r -> r.setTheoryHours(null),
        r -> r.setTheoryHours(-1),
        r -> r.setExerciseHours(null),
        r -> r.setExerciseHours(-1),
        r -> r.setLaboratoryHours(null),
        r -> r.setLaboratoryHours(-1),
        r -> r.setTheoryHours(9));
  }

  @ParameterizedTest
  @MethodSource("invalidData")
  void invalidDataNeverPersists(Consumer<SubjectRequestDTO> change) {
    var r = subjectRequest();
    change.accept(r);
    rejects(400, () -> s.createSubject(r));
    verify(repo, never()).save(any());
  }

  @Test
  void prerequisitesAreValidated() {
    var r = subjectRequest();
    r.setSemester(2);
    r.setPrerequisiteIds(Set.of(8L));
    assertEquals(1, s.createSubject(r).getPrerequisiteIds().size());
    r.setPrerequisiteIds(Set.of(1L, 2L, 3L, 4L));
    rejects(400, () -> s.createSubject(r));
    r.setPrerequisiteIds(Collections.singleton(null));
    rejects(400, () -> s.createSubject(r));
    r.setPrerequisiteIds(Set.of(99L));
    rejects(404, () -> s.createSubject(r));
    r.setPrerequisiteIds(Set.of(8L));
    entity.setSemester(2);
    rejects(400, () -> s.createSubject(r));
    entity.setSemester(1);
    entity.setStudyPlan(new StudyPlanEntity(99L, "Other", StudyPlanStatus.NO_VIGENTE, career()));
    rejects(400, () -> s.createSubject(r));
    entity.setStudyPlan(plan());
    r.setSemester(1);
    rejects(400, () -> s.updateSubject(8L, r));
  }

  @Test
  void protectsIdentityHoursHistoryAndDuplicates() {
    var r = subjectRequest();
    when(repo.existsByStudyPlanIdAndCode(2L, "MAT")).thenReturn(true);
    rejects(409, () -> s.createSubject(r));
    r.setStudyPlanId(99L);
    rejects(400, () -> s.updateSubject(8L, r));
    r.setStudyPlanId(2L);
    r.setSemester(2);
    rejects(409, () -> s.updateSubject(8L, r));
    r.setSemester(1);
    when(enrollments.existsBySection_Subject_Id(8L)).thenReturn(true);
    rejects(409, () -> s.updateSubject(8L, r));
    rejects(409, () -> s.deleteSubject(8L));
    when(enrollments.existsBySection_Subject_Id(8L)).thenReturn(false);
    when(sections.existsBySubjectId(8L)).thenReturn(true);
    r.setTheoryHours(4);
    rejects(409, () -> s.updateSubject(8L, r));
    rejects(409, () -> s.deleteSubject(8L));
    r.setTheoryHours(2);
    when(sections.existsBySubjectId(8L)).thenReturn(false);
    var other = subject();
    other.setId(99L);
    when(repo.findByStudyPlanIdAndCode(2L, "MAT")).thenReturn(Optional.of(other));
    rejects(409, () -> s.updateSubject(8L, r));
    when(repo.existsByPrerequisites_Id(8L)).thenReturn(true);
    rejects(409, () -> s.deleteSubject(8L));
  }
}
