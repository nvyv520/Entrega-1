package SIGA.backend.services;

import static SIGA.backend.services.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import SIGA.backend.dtos.*;
import SIGA.backend.entities.*;
import SIGA.backend.repositories.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.junit.jupiter.api.Test;

class CatalogServiceTests {
  @Test
  void careerLifecycleAndQueries() {
    var repo = mock(CareerRepository.class);
    var students = mock(StudentRepository.class);
    var plans = mock(StudyPlanRepository.class);
    var s = new CareerService(repo, students, plans);
    var entity = career();
    var r = new CareerRequestDTO();
    r.setCode(" INF ");
    r.setName(" Informática ");
    r.setDescription(" Carrera ");
    when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    when(repo.findById(1L)).thenReturn(Optional.of(entity));
    when(repo.findAll()).thenReturn(List.of(entity));
    assertEquals("INF", s.createCareer(r).getCode());
    assertEquals(4, s.createCareer(r).getDuration());
    assertEquals(1, s.getAllCareers().size());
    assertEquals("INF", s.getCareerById(1L).getCode());
    when(repo.findByCode("INF")).thenReturn(Optional.of(entity));
    assertEquals("Informática", s.updateCareer(1L, r).getName());
    assertEquals("INACTIVA", s.updateCareerStatus(1L, Status.INACTIVA).getStatus());
    s.deleteCareer(1L);
    verify(repo).delete(entity);
    rejects(400, () -> s.updateCareerStatus(1L, null));
    rejects(404, () -> s.getCareerById(99L));
    when(repo.existsByCode("INF")).thenReturn(true);
    rejects(409, () -> s.createCareer(r));
    var other = career();
    other.setId(99L);
    when(repo.findByCode("INF")).thenReturn(Optional.of(other));
    rejects(409, () -> s.updateCareer(1L, r));
    when(students.existsByCareerId(1L)).thenReturn(true);
    rejects(409, () -> s.deleteCareer(1L));
    when(students.existsByCareerId(1L)).thenReturn(false);
    when(plans.existsByCareerId(1L)).thenReturn(true);
    rejects(409, () -> s.deleteCareer(1L));
  }

  @Test
  void studyPlanLifecycleAndSingleCurrentPlan() {
    var repo = mock(StudyPlanRepository.class);
    var careers = mock(CareerRepository.class);
    var students = mock(StudentRepository.class);
    var subjects = mock(SubjectRepository.class);
    var em = mock(EntityManager.class);
    var s = new StudyPlanService(repo, careers, students, subjects, em);
    var entity = plan();
    var r = new StudyPlanRequestDTO();
    r.setCareerId(1L);
    r.setIdentifier(" 2026 ");
    when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    when(repo.findById(2L)).thenReturn(Optional.of(entity));
    when(careers.findById(1L)).thenReturn(Optional.of(career()));
    when(careers.existsById(1L)).thenReturn(true);
    when(repo.findAll()).thenReturn(List.of(entity));
    when(repo.findByCareerId(1L)).thenReturn(List.of(entity));
    assertEquals(StudyPlanStatus.NO_VIGENTE, s.createStudyPlan(r).getStatus());
    assertEquals(1, s.getAllStudyPlans().size());
    assertEquals(1, s.getStudyPlansByCareerId(1L).size());
    assertEquals("2026", s.getStudyPlanById(2L).getIdentifier());
    when(repo.findByCareerIdAndIdentifier(1L, "2026")).thenReturn(Optional.of(entity));
    assertEquals("2026", s.updateStudyPlan(2L, r).getIdentifier());
    var old = plan();
    old.setId(90L);
    when(repo.findByCareerIdAndStatus(1L, StudyPlanStatus.VIGENTE)).thenReturn(Optional.of(old));
    assertEquals(
        StudyPlanStatus.VIGENTE, s.updateStudyPlanStatus(2L, StudyPlanStatus.VIGENTE).getStatus());
    assertEquals(StudyPlanStatus.NO_VIGENTE, old.getStatus());
    verify(repo).saveAndFlush(old);
    verify(em).find(CareerEntity.class, 1L, LockModeType.PESSIMISTIC_WRITE);
    s.updateStudyPlanStatus(2L, StudyPlanStatus.NO_VIGENTE);
    s.deleteStudyPlan(2L);
    verify(repo).delete(entity);
    rejects(400, () -> s.updateStudyPlanStatus(2L, null));
    rejects(404, () -> s.getStudyPlanById(99L));
    rejects(404, () -> s.getStudyPlansByCareerId(99L));
    r.setCareerId(99L);
    rejects(404, () -> s.createStudyPlan(r));
    rejects(400, () -> s.updateStudyPlan(2L, r));
    r.setCareerId(1L);
    when(repo.existsByCareerIdAndIdentifier(1L, "2026")).thenReturn(true);
    rejects(409, () -> s.createStudyPlan(r));
    when(repo.findByCareerIdAndIdentifier(1L, "2026")).thenReturn(Optional.of(old));
    rejects(409, () -> s.updateStudyPlan(2L, r));
    when(students.existsByStudyPlanId(2L)).thenReturn(true);
    rejects(409, () -> s.deleteStudyPlan(2L));
    when(students.existsByStudyPlanId(2L)).thenReturn(false);
    when(subjects.existsByStudyPlanId(2L)).thenReturn(true);
    rejects(409, () -> s.deleteStudyPlan(2L));
  }

  @Test
  void matriculationLifecycleAndRejections() {
    var repo = mock(PeriodMatriculationRepository.class);
    var em = mock(EntityManager.class);
    var s = new PeriodMatriculationService(repo, em, mock(EnrollmentRepository.class));
    var p = period();
    var st = student();
    var e = new PeriodMatriculationEntity(1L, st, p, false);
    var r = new PeriodMatriculationRequestDTO();
    r.setStudentId(5L);
    r.setAcademicPeriodId(3L);
    when(em.find(AcademicPeriodEntity.class, 3L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(p);
    when(em.find(StudentEntity.class, 5L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(st);
    when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    assertFalse(s.createMatriculation(r).getEnrollmentConfirmed());
    when(repo.findAll()).thenReturn(List.of(e));
    when(repo.findById(1L)).thenReturn(Optional.of(e));
    when(repo.findByStudentId(5L)).thenReturn(List.of(e));
    when(repo.findByAcademicPeriodId(3L)).thenReturn(List.of(e));
    assertEquals(1, s.getAllMatriculations().size());
    assertEquals(1L, s.getMatriculationById(1L).getId());
    assertEquals(1, s.getMatriculationsByStudent(5L).size());
    assertEquals(1, s.getMatriculationsByAcademicPeriod(3L).size());
    rejects(404, () -> s.getMatriculationById(99L));
    rejects(400, () -> s.createMatriculation(null));
    rejects(400, () -> s.createMatriculation(new PeriodMatriculationRequestDTO()));
    when(repo.existsByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(true);
    rejects(409, () -> s.createMatriculation(r));
    p.setStatus(AcademicPeriodStatus.CERRADO);
    rejects(409, () -> s.createMatriculation(r));
    p.setStatus(AcademicPeriodStatus.ABIERTO);
    st.setStatus(StudentStatus.EGRESADO);
    rejects(409, () -> s.createMatriculation(r));
    r.setStudentId(99L);
    rejects(404, () -> s.createMatriculation(r));
    r.setAcademicPeriodId(99L);
    rejects(404, () -> s.createMatriculation(r));
  }
}
