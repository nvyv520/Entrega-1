package SIGA.backend.controllers;

import SIGA.backend.dtos.GradeRequestDTO;
import SIGA.backend.dtos.GradeResponseDTO;
import SIGA.backend.entities.GradeResult;
import SIGA.backend.services.GradeService;

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
@RequestMapping("/api/grades")
@RequiredArgsConstructor
@Tag(name = "Grades", description = "Grade endpoints")
public class GradeController {

    private final GradeService gradeService;


    // get all grades
    @GetMapping
    @Operation(summary = "Get all grades")
    @ApiResponse(responseCode = "200", description = "Grades retrieved successfully")
    public ResponseEntity<List<GradeResponseDTO>> getAllGrades() {
        List<GradeResponseDTO> grades = gradeService.getAllGrades();
        return ResponseEntity.ok(grades);
    }


    // get grade by id
    @GetMapping("/{id}")
    @Operation(summary = "Get grade by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grade found"),
            @ApiResponse(responseCode = "404", description = "Grade not found")
    })
    public ResponseEntity<GradeResponseDTO> getGradeById(@PathVariable Long id) {
        GradeResponseDTO grade = gradeService.getGradeById(id);
        return ResponseEntity.ok(grade);
    }


    // get grade by enrollment
    @GetMapping("/enrollment/{enrollmentId}")
    @Operation(summary = "Get grade by enrollment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grade found"),
            @ApiResponse(responseCode = "404", description = "Grade not found")
    })
    public ResponseEntity<GradeResponseDTO> getGradeByEnrollment(@PathVariable Long enrollmentId) {
        GradeResponseDTO grade = gradeService.getGradeByEnrollment(enrollmentId);
        return ResponseEntity.ok(grade);
    }


    // get grades by result
    @GetMapping("/result/{result}")
    @Operation(summary = "Get grades by result")
    @ApiResponse(responseCode = "200", description = "Grades retrieved successfully")
    public ResponseEntity<List<GradeResponseDTO>> getGradesByResult(@PathVariable GradeResult result) {
        List<GradeResponseDTO> grades = gradeService.getGradesByResult(result);
        return ResponseEntity.ok(grades);
    }


    // get grades by student
    @GetMapping("/student/{studentId}")
    @Operation(summary = "Get grades by student")
    @ApiResponse(responseCode = "200", description = "Grades retrieved successfully")
    public ResponseEntity<List<GradeResponseDTO>> getGradesByStudent(@PathVariable Long studentId) {
        List<GradeResponseDTO> grades = gradeService.getGradesByStudent(studentId);
        return ResponseEntity.ok(grades);
    }


    // get grades by academic period
    @GetMapping("/academic-period/{periodId}")
    @Operation(summary = "Get grades by academic period")
    @ApiResponse(responseCode = "200", description = "Grades retrieved successfully")
    public ResponseEntity<List<GradeResponseDTO>> getGradesByAcademicPeriod(@PathVariable Long periodId) {
        List<GradeResponseDTO> grades = gradeService.getGradesByAcademicPeriod(periodId);
        return ResponseEntity.ok(grades);
    }


    // create grade
    @PostMapping
    @Operation(summary = "Create a new grade")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Grade created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid grade data"),
            @ApiResponse(responseCode = "404", description = "Enrollment not found"),
            @ApiResponse(responseCode = "409", description = "Grade already exists or enrollment is not valid")
    })
    public ResponseEntity<GradeResponseDTO> createGrade(@Valid @RequestBody GradeRequestDTO grade) {
        GradeResponseDTO newGrade = gradeService.createGrade(grade);
        URI location = URI.create("/api/grades/" + newGrade.getId())
        return ResponseEntity.created(location).body(newGrade);
    }


    // update grade
    @PutMapping("/{id}")
    @Operation(summary = "Update grade by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grade updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid grade data"),
            @ApiResponse(responseCode = "404", description = "Grade not found"),
            @ApiResponse(responseCode = "409", description = "Grade cannot be updated")
    })
    public ResponseEntity<GradeResponseDTO> updateGrade(@PathVariable Long id, @Valid @RequestBody GradeRequestDTO grade) {
        GradeResponseDTO updatedGrade = gradeService.updateGrade(id, grade);
        return ResponseEntity.ok(updatedGrade);
    }
}
