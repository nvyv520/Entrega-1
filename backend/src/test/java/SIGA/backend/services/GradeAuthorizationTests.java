package SIGA.backend.services;

import SIGA.backend.dtos.GradeRequestDTO;
import SIGA.backend.entities.*;
import SIGA.backend.repositories.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GradeAuthorizationTests {
    private GradeRepository grades;
    private EnrollmentRepository enrollments;
    private TeacherRepository teachers;
    private SectionRepository sections;
    private EntityManager entityManager;
    private GradeService service;
    private SectionService sectionService;
    private SectionEntity section;
    private EnrollmentEntity enrollment;
    private AcademicPeriodEntity period;
    private GradeRequestDTO request;

    @BeforeEach
    void setUp() {
        grades = mock(GradeRepository.class);
        enrollments = mock(EnrollmentRepository.class);
        teachers = mock(TeacherRepository.class);
        sections = mock(SectionRepository.class);
        entityManager = mock(EntityManager.class);
        sectionService = new SectionService(sections, mock(SubjectRepository.class), teachers,
                mock(AcademicPeriodRepository.class), enrollments, entityManager);
        service = new GradeService(grades, enrollments, entityManager, teachers, mock(StudentRepository.class), sectionService);
        TeacherEntity assigned = new TeacherEntity();
        assigned.setId(7L);
        when(teachers.findByKeycloakSubject("teacher-sub")).thenReturn(Optional.of(assigned));
        section = new SectionEntity();
        section.setId(10L);
        section.setTeacher(assigned);
        when(sections.findById(10L)).thenReturn(Optional.of(section));
        period = new AcademicPeriodEntity();
        period.setId(3L);
        period.setStatus(AcademicPeriodStatus.ABIERTO);
        enrollment = new EnrollmentEntity();
        enrollment.setId(20L);
        enrollment.setSection(section);
        enrollment.setAcademicPeriod(period);
        enrollment.setStatus(EnrollmentStatus.ACTIVO);
        when(enrollments.findById(20L)).thenReturn(Optional.of(enrollment));
        when(entityManager.find(AcademicPeriodEntity.class, 3L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(period);
        request = new GradeRequestDTO();
        request.setEnrollmentId(20L);
        request.setFinalGrade(new BigDecimal("5.0"));
        account("TEACHER");
    }

    private void account(String role) {
        var jwt = Jwt.withTokenValue("test").header("alg", "RS256").subject("teacher-sub").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    private void otherTeacher() {
        TeacherEntity other = new TeacherEntity();
        other.setId(8L);
        section.setTeacher(other);
    }

    @Test
    void teacherReadsOnlyAssignedSection() {
        assertTrue(service.getCurrentTeacherSectionGrades("teacher-sub", 10L).isEmpty());
        verify(grades).findByEnrollment_Section_IdAndEnrollment_Status(10L, EnrollmentStatus.ACTIVO);
        otherTeacher();
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.getCurrentTeacherSectionGrades("teacher-sub", 10L)).getStatusCode());
        verifyNoMoreInteractions(grades);
    }

    @Test
    void missingTeacherOrSectionReturnsNotFound() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.getCurrentTeacherSectionGrades("unknown", 10L)).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.getCurrentTeacherSectionGrades("teacher-sub", 99L)).getStatusCode());
        verifyNoInteractions(grades);
    }

    @Test
    void teacherCannotCreateOrUpdateAnotherSectionsGrade() {
        otherTeacher();
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.createGrade(request)).getStatusCode());
        GradeEntity grade = new GradeEntity();
        grade.setEnrollment(enrollment);
        when(grades.findById(1L)).thenReturn(Optional.of(grade));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.updateGrade(1L, request)).getStatusCode());
        verify(grades, never()).save(any());
    }

    @Test
    void assignedTeacherCanWriteAndClosedPeriodBlocksEvenAdmin() {
        when(grades.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(new BigDecimal("5.0"), service.createGrade(request).getFinalGrade());
        period.setStatus(AcademicPeriodStatus.CERRADO);
        GradeEntity current = new GradeEntity();
        current.setEnrollment(enrollment);
        when(grades.findById(1L)).thenReturn(Optional.of(current));
        for (String role : List.of("TEACHER", "ADMIN")) {
            account(role);
            assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                    () -> service.createGrade(request)).getStatusCode());
            assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                    () -> service.updateGrade(1L, request)).getStatusCode());
        }
    }

    @Test
    void studentCannotWriteAndAdminKeepsExistingGradePermission() {
        account("STUDENT");
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.createGrade(request)).getStatusCode());
        otherTeacher();
        account("ADMIN");
        when(grades.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(GradeResult.APROBADO, service.createGrade(request).getResult());
    }
}
