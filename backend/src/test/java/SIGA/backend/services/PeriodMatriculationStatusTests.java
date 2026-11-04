package SIGA.backend.services;

import static SIGA.backend.services.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import SIGA.backend.entities.*;
import SIGA.backend.repositories.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PeriodMatriculationStatusTests {
  PeriodMatriculationRepository repo;
  EnrollmentRepository enrollments;
  EntityManager em;
  PeriodMatriculationService service;
  AcademicPeriodEntity period;
  StudentEntity student;
  PeriodMatriculationEntity record;

  @BeforeEach
  void setup() {
    repo = mock(PeriodMatriculationRepository.class);
    enrollments = mock(EnrollmentRepository.class);
    em = mock(EntityManager.class);
    service = new PeriodMatriculationService(repo, em, enrollments);
    period = period();
    student = student();
    record = new PeriodMatriculationEntity(1L, student, period, false);
    when(em.find(AcademicPeriodEntity.class, 3L, LockModeType.PESSIMISTIC_WRITE))
        .thenReturn(period);
    when(em.find(StudentEntity.class, 5L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(student);
  }

  @Test
  void yesCreatesExternalRecordWithoutConfirmingSelection() {
    service.updateMatriculationStatus(5L, 3L, true);
    var saved = ArgumentCaptor.forClass(PeriodMatriculationEntity.class);
    verify(repo).save(saved.capture());
    assertSame(student, saved.getValue().getStudent());
    assertSame(period, saved.getValue().getAcademicPeriod());
    assertFalse(saved.getValue().getEnrollmentConfirmed());
  }

  @Test
  void yesPreservesAnExistingConfirmedSelection() {
    record.setEnrollmentConfirmed(true);
    when(repo.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(Optional.of(record));
    service.updateMatriculationStatus(5L, 3L, true);
    verify(repo, never()).save(any());
    verify(repo, never()).delete(any());
    assertTrue(record.getEnrollmentConfirmed());
  }

  @Test
  void noRemovesOnlyAnUnusedRecord() {
    when(repo.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(Optional.of(record));
    service.updateMatriculationStatus(5L, 3L, false);
    verify(repo).delete(record);
  }

  @Test
  void noWithoutARecordIsIdempotent() {
    service.updateMatriculationStatus(5L, 3L, false);
    verify(repo, never()).delete(any());
    verify(repo, never()).save(any());
  }

  @Test
  void noRejectsActiveSubjects() {
    when(repo.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(Optional.of(record));
    when(enrollments.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(List.of(enrollment()));
    rejects(409, () -> service.updateMatriculationStatus(5L, 3L, false));
    verify(repo, never()).delete(any());
  }

  @Test
  void noRejectsConfirmedSelectionEvenWithoutActiveSubjects() {
    record.setEnrollmentConfirmed(true);
    when(repo.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(Optional.of(record));
    rejects(409, () -> service.updateMatriculationStatus(5L, 3L, false));
    verify(repo, never()).delete(any());
  }

  @Test
  void cancelledSubjectDoesNotPreventCorrection() {
    var cancelled = enrollment();
    cancelled.setStatus(EnrollmentStatus.CANCELADO);
    when(enrollments.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(List.of(cancelled));
    when(repo.findByStudentIdAndAcademicPeriodId(5L, 3L)).thenReturn(Optional.of(record));
    service.updateMatriculationStatus(5L, 3L, false);
    verify(repo).delete(record);
  }

  @Test
  void closedPeriodCannotBeChanged() {
    period.setStatus(AcademicPeriodStatus.CERRADO);
    rejects(409, () -> service.updateMatriculationStatus(5L, 3L, true));
    verifyNoInteractions(repo);
  }

  @Test
  void onlyRegularStudentsCanGainMatriculation() {
    student.setStatus(StudentStatus.ELIMINADO);
    rejects(409, () -> service.updateMatriculationStatus(5L, 3L, true));
    verify(repo, never()).save(any());
  }

  @Test
  void missingReferencesAndStatusAreRejected() {
    rejects(400, () -> service.updateMatriculationStatus(null, 3L, true));
    rejects(400, () -> service.updateMatriculationStatus(5L, null, false));
    rejects(400, () -> service.updateMatriculationStatus(5L, 3L, null));
    rejects(404, () -> service.updateMatriculationStatus(5L, 99L, true));
    rejects(404, () -> service.updateMatriculationStatus(99L, 3L, true));
    verify(repo, never()).save(any());
  }
}
