package SIGA.backend.services;

import static org.junit.jupiter.api.Assertions.*;

import SIGA.backend.dtos.*;
import SIGA.backend.entities.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.function.Executable;
import org.springframework.web.server.ResponseStatusException;

public final class ServiceFixtures {
  private ServiceFixtures() {}

  public static CareerEntity career() {
    return new CareerEntity(1L, "INF", "Informática", "Carrera", Status.ACTIVA, 4);
  }

  public static StudyPlanEntity plan() {
    return new StudyPlanEntity(2L, "2026", StudyPlanStatus.VIGENTE, career());
  }

  public static AcademicPeriodEntity period() {
    return new AcademicPeriodEntity(3L, 2026, 1, "2026-1", AcademicPeriodStatus.ABIERTO);
  }

  public static StudentEntity student() {
    return new StudentEntity(
        5L,
        "12.345.678-9",
        "Ana",
        "Pérez",
        "ana@example.cl",
        StudentStatus.REGULAR,
        null,
        career(),
        plan());
  }

  public static TeacherEntity teacher() {
    return new TeacherEntity(
        7L,
        "11.111.111-1",
        "Luis",
        "Soto",
        "luis@example.cl",
        "Ingeniero",
        "Magíster",
        Status.ACTIVA,
        null);
  }

  public static SubjectEntity subject() {
    return new SubjectEntity(8L, "MAT", "Matemáticas", 1, 2, 0, 0, 4, plan(), Set.of());
  }

  public static SectionEntity section() {
    return new SectionEntity(
        10L, 0L, subject(), period(), teacher(), 30, 0, Status.ACTIVA, Set.of("L1"));
  }

  public static EnrollmentEntity enrollment() {
    return new EnrollmentEntity(
        20L, student(), section(), period(), LocalDate.of(2026, 3, 1), EnrollmentStatus.ACTIVO);
  }

  public static GradeEntity grade() {
    return new GradeEntity(30L, enrollment(), new BigDecimal("5.0"), GradeResult.APROBADO);
  }

  public static SubjectRequestDTO subjectRequest() {
    var r = new SubjectRequestDTO();
    r.setCode(" MAT ");
    r.setName(" Matemáticas ");
    r.setStudyPlanId(2L);
    r.setSemester(1);
    r.setSct(4);
    r.setTheoryHours(2);
    r.setExerciseHours(0);
    r.setLaboratoryHours(0);
    return r;
  }

  public static SectionRequestDTO sectionRequest() {
    var r = new SectionRequestDTO();
    r.setSubjectId(8L);
    r.setAcademicPeriodId(3L);
    r.setTeacherId(7L);
    r.setCapacity(30);
    r.setBlocks(java.util.List.of("L1"));
    return r;
  }

  public static StudentRequestDTO studentRequest() {
    var r = new StudentRequestDTO();
    r.setRut("12.345.678-9");
    r.setFirstName("Ana");
    r.setLastName("Pérez");
    r.setEmail("ana@example.cl");
    r.setCareerId(1L);
    return r;
  }

  public static TeacherRequestDTO teacherRequest() {
    var r = new TeacherRequestDTO();
    r.setRut("11.111.111-1");
    r.setFirstName("Luis");
    r.setLastName("Soto");
    r.setEmail("luis@example.cl");
    r.setProfessionalTitle("Ingeniero");
    r.setAcademicDegree("Magíster");
    return r;
  }

  public static void rejects(int status, Executable action) {
    assertEquals(
        status, assertThrows(ResponseStatusException.class, action).getStatusCode().value());
  }
}
