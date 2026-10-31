package SIGA.backend.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import SIGA.backend.dtos.EnrollmentRequestDTO;
import SIGA.backend.dtos.EnrollmentResponseDTO;
import SIGA.backend.dtos.SectionResponseDTO;
import SIGA.backend.entities.EnrollmentStatus;
import SIGA.backend.services.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Enrollment management endpoints")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // Return academic validation reasons without exposing internal exceptions.
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleEnrollmentError(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
    }


    // get enrollments of authenticated student
    @GetMapping("/me")
    @Operation(summary = "Get authenticated student enrollments")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enrollments retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Student not linked to Keycloak")
    })
    public ResponseEntity<List<EnrollmentResponseDTO>> getCurrentStudentEnrollments( @AuthenticationPrincipal Jwt jwt) {
        List<EnrollmentResponseDTO> enrollments = enrollmentService.getCurrentStudentEnrollments(jwt.getSubject());
        return ResponseEntity.ok(enrollments);
    }


    // Academic catalogue for the authenticated student
    @GetMapping("/me/academic-period/{periodId}/eligible-sections")
    @Operation(summary = "Get academically eligible sections and existing enrollments for authenticated student")
    public ResponseEntity<List<SectionResponseDTO>> getCurrentStudentEligibleSections(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long periodId) {
        return ResponseEntity.ok(enrollmentService.getCurrentStudentEligibleSections(jwt.getSubject(), periodId));
    }


    // create enrollment for authenticated student
    @PostMapping("/me")
    @Operation(summary = "Create enrollment for authenticated student")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Enrollment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid section"),
            @ApiResponse(responseCode = "404", description = "Student or section not found"),
            @ApiResponse(responseCode = "409", description = "Enrollment is not allowed")
    })
    public ResponseEntity<EnrollmentResponseDTO> createCurrentStudentEnrollment(
            @AuthenticationPrincipal Jwt jwt, @RequestParam Long sectionId) {

        EnrollmentResponseDTO created = enrollmentService.createCurrentStudentEnrollment(jwt.getSubject(), sectionId);
        URI location = URI.create("/api/enrollments/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }


    // cancel enrollment for authenticated student
    @PatchMapping("/me/{enrollmentId}/cancel")
    @Operation(summary = "Cancel enrollment for authenticated student")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enrollment cancelled successfully"),
            @ApiResponse(responseCode = "403", description = "Enrollment belongs to another student"),
            @ApiResponse(responseCode = "404", description = "Enrollment not found"),
            @ApiResponse(responseCode = "409", description = "Enrollment cannot be cancelled")
    })
    public ResponseEntity<EnrollmentResponseDTO> cancelCurrentStudentEnrollment(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long enrollmentId) {

        EnrollmentResponseDTO cancelled = enrollmentService.cancelCurrentStudentEnrollment(jwt.getSubject(), enrollmentId);
        return ResponseEntity.ok(cancelled);
    }


    // Verify current student's subject selection
    @PostMapping("/me/academic-period/{periodId}/verify")
    @Operation(summary = "Verify authenticated student's subject selection")
    public ResponseEntity<Void> verifyCurrentStudentSelection(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long periodId) {

        enrollmentService.verifyCurrentStudentSelection(
                jwt.getSubject(),
                periodId
        );

        return ResponseEntity.noContent().build();
    }


    @GetMapping("/me/academic-period/{periodId}/verification-status")
    @Operation(summary = "Get authenticated student selection verification status")
    public ResponseEntity<Boolean> getCurrentStudentVerificationStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long periodId) {

        boolean verified =
                enrollmentService.isCurrentStudentSelectionVerified(
                        jwt.getSubject(),
                        periodId
                );

        return ResponseEntity.ok(verified);
    }


    // get all enrollments
    @GetMapping
    @Operation(summary = "Get all enrollments")
    @ApiResponse(responseCode = "200", description = "Enrollments retrieved successfully")
    public ResponseEntity<List<EnrollmentResponseDTO>> getAllEnrollments() {
        List<EnrollmentResponseDTO> enrollments = enrollmentService.getAllEnrollments();
        return ResponseEntity.ok(enrollments);
    }


    // get enrollment by id
    @GetMapping("/{id}")
    @Operation(summary = "Get enrollment by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enrollment found"),
            @ApiResponse(responseCode = "404", description = "Enrollment not found")
    })
    public ResponseEntity<EnrollmentResponseDTO> getEnrollmentById(@PathVariable Long id) {
        EnrollmentResponseDTO enrollment = enrollmentService.getEnrollmentById(id);
        return ResponseEntity.ok(enrollment);
    }


    // get enrollments by student
    @GetMapping("/student/{studentId}")
    @Operation(summary = "Get enrollments by student")
    @ApiResponse(responseCode = "200", description = "Enrollments retrieved successfully")
    public ResponseEntity<List<EnrollmentResponseDTO>> getEnrollmentsByStudent(@PathVariable Long studentId) {
        List<EnrollmentResponseDTO> enrollments = enrollmentService.getEnrollmentsByStudent(studentId);
        return ResponseEntity.ok(enrollments);
    }


    // get enrollments by section
    @GetMapping("/section/{sectionId}")
    @Operation(summary = "Get enrollments by section")
    @ApiResponse(responseCode = "200", description = "Enrollments retrieved successfully")
    public ResponseEntity<List<EnrollmentResponseDTO>> getEnrollmentsBySection(@PathVariable Long sectionId) {
        List<EnrollmentResponseDTO> enrollments = enrollmentService.getEnrollmentsBySection(sectionId);
        return ResponseEntity.ok(enrollments);
    }


    // get enrollments by academic period
    @GetMapping("/academic-period/{periodId}")
    @Operation(summary = "Get enrollments by academic period")
    @ApiResponse(responseCode = "200", description = "Enrollments retrieved successfully")
    public ResponseEntity<List<EnrollmentResponseDTO>> getEnrollmentsByAcademicPeriod(@PathVariable Long periodId) {
        List<EnrollmentResponseDTO> enrollments = enrollmentService.getEnrollmentsByAcademicPeriod(periodId);
        return ResponseEntity.ok(enrollments);
    }


    // get enrollments by status
    @GetMapping("/status/{status}")
    @Operation(summary = "Get enrollments by status")
    @ApiResponse(responseCode = "200", description = "Enrollments retrieved successfully")
    public ResponseEntity<List<EnrollmentResponseDTO>> getEnrollmentsByStatus(@PathVariable EnrollmentStatus status) {
        List<EnrollmentResponseDTO> enrollments = enrollmentService.getEnrollmentsByStatus(status);
        return ResponseEntity.ok(enrollments);
    }


    // create enrollment
    @PostMapping
    @Operation(summary = "Create a new enrollment")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Enrollment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid enrollment data"),
            @ApiResponse(responseCode = "404", description = "Student or section not found"),
            @ApiResponse(responseCode = "409", description = "Enrollment is not allowed")
    })
    public ResponseEntity<EnrollmentResponseDTO> createEnrollment(@Valid @RequestBody EnrollmentRequestDTO enrollment) {
        EnrollmentResponseDTO newEnrollment = enrollmentService.createEnrollment(enrollment);
        URI location = URI.create("/api/enrollments/" + newEnrollment.getId());
        return ResponseEntity.created(location).body(newEnrollment);
    }


    // cancel enrollment
    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel enrollment by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enrollment cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Enrollment not found"),
            @ApiResponse(responseCode = "409", description = "Enrollment cannot be cancelled")
    })
    public ResponseEntity<EnrollmentResponseDTO> cancelEnrollment(@PathVariable Long id) {
        EnrollmentResponseDTO cancelledEnrollment = enrollmentService.cancelEnrollment(id);
        return ResponseEntity.ok(cancelledEnrollment);
    }


    // confirm student subject selection
    @PatchMapping("/student/{studentId}/academic-period/{periodId}/confirm")
    @Operation(summary = "Confirm student subject selection")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Selection confirmed"),
            @ApiResponse(responseCode = "404", description = "Student or period not found"),
            @ApiResponse(responseCode = "409", description = "Selection cannot be confirmed")
    })
    public ResponseEntity<Void> confirmEnrollmentSelection(@PathVariable Long studentId, @PathVariable Long periodId) {
        enrollmentService.confirmEnrollmentSelection(studentId, periodId);
        return ResponseEntity.noContent().build();
    }

}
