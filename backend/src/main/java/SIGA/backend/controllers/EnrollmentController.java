package SIGA.backend.controllers;

import SIGA.backend.dtos.EnrollmentRequestDTO;
import SIGA.backend.dtos.EnrollmentResponseDTO;
import SIGA.backend.entities.EnrollmentStatus;
import SIGA.backend.services.EnrollmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Enrollment management endpoints")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;


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
}
