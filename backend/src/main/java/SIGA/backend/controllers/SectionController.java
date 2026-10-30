package SIGA.backend.controllers;

import SIGA.backend.entities.Status;

import SIGA.backend.dtos.SectionRequestDTO;
import SIGA.backend.dtos.SectionResponseDTO;
import SIGA.backend.dtos.SectionStudentResponseDTO;

import SIGA.backend.services.SectionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
@Tag(name = "Sections", description = "Section  endpoints")
public class SectionController {

    private final SectionService sectionService;


    // get sections assigned to authenticated teacher
    @GetMapping("/me")
    @Operation(summary = "Get authenticated teacher sections")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Teacher sections retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Teacher not linked to Keycloak")
    })
    public ResponseEntity<List<SectionResponseDTO>> getCurrentTeacherSections(
            @AuthenticationPrincipal Jwt jwt) {

        List<SectionResponseDTO> sections = sectionService.getCurrentTeacherSections(jwt.getSubject());
        return ResponseEntity.ok(sections);
    }


    // get students enrolled in authenticated teacher section
    @GetMapping("/me/{sectionId}/students")
    @Operation(summary = "Get students enrolled in authenticated teacher section")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Students retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Teacher not assigned to section"),
            @ApiResponse(responseCode = "404", description = "Teacher or section not found")
    })
    public ResponseEntity<List<SectionStudentResponseDTO>> getCurrentTeacherSectionStudents(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long sectionId) {

        List<SectionStudentResponseDTO> students = sectionService.getCurrentTeacherSectionStudents(jwt.getSubject(), sectionId);
        return ResponseEntity.ok(students);
    }


    // get all sections
    @GetMapping 
    @Operation (summary = "Get all sections")
    @ApiResponse (responseCode = "200", description = "Sections retrieved successfully")
    public ResponseEntity<List<SectionResponseDTO>> getAllSections() {
        List<SectionResponseDTO> sections = sectionService.getAllSections();
        return ResponseEntity.ok(sections);
    }


    // get section by id
    @GetMapping ("/{id}")
    @Operation (summary = "Get section by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Section found"),
            @ApiResponse(responseCode = "404", description = "Section not found")
    })
    public ResponseEntity<SectionResponseDTO> getSectionById(@PathVariable Long id) {
        SectionResponseDTO section = sectionService.getSectionById(id);
        return ResponseEntity.ok(section);
    }


    // get sections by subject
    @GetMapping ("/subject/{subjectId}")
    @Operation (summary = "Get sections by subject")
    @ApiResponse (responseCode = "200", description = "Sections retrieved successfully")
    public ResponseEntity<List<SectionResponseDTO>> getSectionsBySubject(@PathVariable Long subjectId) {
        List<SectionResponseDTO> sections = sectionService.getSectionsBySubject(subjectId);
        return ResponseEntity.ok(sections);
    }


    // get sections by academic period
    @GetMapping("/academic-period/{academicPeriodId}")
    @Operation(summary = "Get sections by academic period")
    @ApiResponse(responseCode = "200", description = "Sections retrieved successfully")
    public ResponseEntity<List<SectionResponseDTO>> getSectionsByAcademicPeriod(@PathVariable Long academicPeriodId) {
        List<SectionResponseDTO> sections = sectionService.getSectionsByAcademicPeriod(academicPeriodId);
        return ResponseEntity.ok(sections);
    }


    // get sections by teacher
    @GetMapping("/teacher/{teacherId}")
    @Operation(summary = "Get sections by teacher")
    @ApiResponse(responseCode = "200", description = "Sections retrieved successfully")
    public ResponseEntity<List<SectionResponseDTO>> getSectionsByTeacher(@PathVariable Long teacherId) {
        List<SectionResponseDTO> sections = sectionService.getSectionsByTeacher(teacherId);
        return ResponseEntity.ok(sections);
    }


    // get sections by status
    @GetMapping("/status/{status}")
    @Operation(summary = "Get sections by status")
    @ApiResponse(responseCode = "200", description = "Sections retrieved successfully")
    public ResponseEntity<List<SectionResponseDTO>> getSectionsByStatus(@PathVariable Status status) {
        List<SectionResponseDTO> sections = sectionService.getSectionsByStatus(status);
        return ResponseEntity.ok(sections);
    }


    // create section
    @PostMapping
    @Operation(summary = "Create a new section")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Section created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid section data"),
            @ApiResponse(responseCode = "404", description = "Subject, academic period or teacher not found"),
            @ApiResponse(responseCode = "409", description = "Section already exists or has a schedule conflict")
    })
    public ResponseEntity<SectionResponseDTO> createSection(@Valid @RequestBody SectionRequestDTO section) {
        SectionResponseDTO newSection = sectionService.createSection(section);
        URI location = URI.create("/api/sections/" + newSection.getId());
        return ResponseEntity.created(location).body(newSection);
    }


    // update section
    @PutMapping("/{id}")
    @Operation(summary = "Update section by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Section updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid section data"),
            @ApiResponse(responseCode = "404", description = "Section or teacher not found"),
            @ApiResponse(responseCode = "409", description = "Section cannot be updated")
    })
    public ResponseEntity<SectionResponseDTO> updateSection(@PathVariable Long id, @Valid @RequestBody SectionRequestDTO section) {
        SectionResponseDTO updatedSection = sectionService.updateSection(id, section);
        return ResponseEntity.ok(updatedSection);
    }


    // delete section
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete section by id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Section deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Section not found"),
            @ApiResponse(responseCode = "409", description = "Section has enrollments or belongs to a closed period")
    })
    public ResponseEntity<Void> deleteSection(@PathVariable Long id) {
        sectionService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }

}