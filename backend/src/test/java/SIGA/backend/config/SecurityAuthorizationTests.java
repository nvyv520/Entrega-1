package SIGA.backend.config;

import SIGA.backend.controllers.*;
import SIGA.backend.services.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Map;
import org.springframework.core.env.MapPropertySource;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class SecurityAuthorizationTests {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "spring.security.oauth2.resourceserver.jwt.issuer-uri", "https://issuer.test/realms/SIGA",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri", "https://issuer.test/certs",
                "siga.security.jwt.audience", "siga-backend",
                "siga.security.jwt.validate-audience", "true")));
        context.register(TestConfig.class);
        context.refresh();
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    private RequestPostProcessor account(String role) {
        return jwt().jwt(token -> token.subject("account-subject"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void authenticationIsRequired() throws Exception {
        mvc.perform(get("/api/grades")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/students/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void teachersCannotReadUnrestrictedGradesOrOtherProfiles() throws Exception {
        for (String path : new String[]{"/api/grades", "/api/grades/1", "/api/grades/student/1",
                "/api/students", "/api/students/1", "/api/enrollments", "/api/teachers/1"}) {
            mvc.perform(get(path).with(account("TEACHER"))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/grades/me/sections/7").with(account("TEACHER"))).andExpect(status().isOk());
        verify(context.getBean(GradeService.class)).getCurrentTeacherSectionGrades("account-subject", 7L);
    }

    @Test
    void studentsUsePersonalRoutesAndCannotAccessAdministration() throws Exception {
        for (String path : new String[]{"/api/students/me", "/api/students/me/academic-history",
                "/api/enrollments/me", "/api/grades/me"}) {
            mvc.perform(get(path).with(account("STUDENT"))).andExpect(status().isOk());
        }
        for (String path : new String[]{"/api/students/1", "/api/grades/student/1",
                "/api/enrollments/student/1", "/api/sections/me", "/api/grades/me/sections/7"}) {
            mvc.perform(get(path).with(account("STUDENT"))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/enrollments/me/academic-period/4/verify").with(account("STUDENT")))
                .andExpect(status().isNoContent());
        verify(context.getBean(EnrollmentService.class)).verifyCurrentStudentSelection("account-subject", 4L);
        mvc.perform(patch("/api/enrollments/student/8/academic-period/4/confirm").with(account("STUDENT")))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/academic-periods/4/close").with(account("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administrativeReadsAndCloseRemainAvailable() throws Exception {
        for (String path : new String[]{"/api/students", "/api/teachers", "/api/grades",
                "/api/enrollments", "/api/period-matriculations"}) {
            mvc.perform(get(path).with(account("ADMIN"))).andExpect(status().isOk());
        }
        mvc.perform(patch("/api/academic-periods/4/close").with(account("ADMIN"))).andExpect(status().isOk());
        mvc.perform(patch("/api/enrollments/student/8/academic-period/4/confirm").with(account("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/enrollments").with(account("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/enrollments/1/cancel").with(account("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/grades").with(account("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(put("/api/grades/1").with(account("ADMIN"))).andExpect(status().isForbidden());
    }

    @Test
    void unconfiguredEndpointsAndAccountsWithoutAppRolesAreDenied() throws Exception {
        mvc.perform(get("/api/unconfigured").with(account("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get("/unconfigured").with(account("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/careers").with(account("OTHER"))).andExpect(status().isForbidden());
        mvc.perform(options("/api/grades/me").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, GradeController.class, EnrollmentController.class, SectionController.class,
            StudentController.class, TeacherController.class, AcademicPeriodController.class,
            PeriodMatriculationController.class, CareerController.class})
    static class TestConfig {
        @Bean GradeService gradeService() { return mock(GradeService.class); }
        @Bean EnrollmentService enrollmentService() { return mock(EnrollmentService.class); }
        @Bean SectionService sectionService() { return mock(SectionService.class); }
        @Bean StudentService studentService() { return mock(StudentService.class); }
        @Bean TeacherService teacherService() { return mock(TeacherService.class); }
        @Bean AcademicPeriodService academicPeriodService() { return mock(AcademicPeriodService.class); }
        @Bean PeriodMatriculationService periodMatriculationService() { return mock(PeriodMatriculationService.class); }
        @Bean CareerService careerService() { return mock(CareerService.class); }
    }
}
