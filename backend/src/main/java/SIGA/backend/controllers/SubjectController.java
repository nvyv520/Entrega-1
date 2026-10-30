package SIGA.backend.controllers;

import SIGA.backend.dtos.SubjectRequestDTO;
import SIGA.backend.dtos.SubjectResponseDTO;

import SIGA.backend.services.SubjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.net.URI;


@RestController 
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@Tag(name = "Subjects", description = "Subjects endpoints")

public class SubjectController {

    private final SubjectService subjectService;

    // get all subjects
    @GetMapping 
    @Operation (summary = "Get all subject")
    public ResponseEntity<List<SubjectResponseDTO>> getAllSubjects() {
        List<SubjectResponseDTO> subjects = subjectService.getAllSubjects();
        return ResponseEntity.ok(subjects);
    }


    // get subject by id 
    @GetMapping ("/{id}")
    @Operation (summary = "Get subject by id")
    @ApiResponse (responseCode = "404", description = "Subject not found")
    public ResponseEntity<SubjectResponseDTO> getSubjectById(@PathVariable Long id) {
        SubjectResponseDTO subject = subjectService.getSubjectById(id);
        return ResponseEntity.ok(subject);
    }


    // get subjects by study plan id
    @GetMapping ("/study-plan/{studyPlanId}")
    @Operation (summary = "Get subjects by study plan id")
    @ApiResponse (responseCode = "404", description = "Study plan not found")
    public ResponseEntity<List<SubjectResponseDTO>> getSubjectsByStudyPlanId(@PathVariable Long studyPlanId) {
        List<SubjectResponseDTO> subjects = subjectService.getSubjectByStudyPlanId(studyPlanId);
        return ResponseEntity.ok(subjects);
    }


    // create a subject
    @PostMapping 
    @Operation (summary = "Create a new subject")
    @ApiResponses ({
        @ApiResponse (responseCode = "201", description = "Subject created successfully"),
        @ApiResponse (responseCode = "400", description = "Invalid subject data or prerequisites"),
        @ApiResponse (responseCode = "404", description = "Study plan or prerequisite not found"),
        @ApiResponse (responseCode = "409", description = "Subject code already exists")
    })
    public ResponseEntity<SubjectResponseDTO> createSubject(@Valid @RequestBody SubjectRequestDTO subject) {
        SubjectResponseDTO newSubject = subjectService.createSubject(subject);
        URI location = URI.create("/api/subjects/"+ newSubject.getId());
        return ResponseEntity.created(location).body(newSubject);
    }


    // update subject by id
    @PutMapping ("/{id}")
    @Operation (summary = "Update subject by id")
    @ApiResponses ({
        @ApiResponse (responseCode = "400", description = "Invalid subject data"),
        @ApiResponse (responseCode = "404", description = "Study plan or prerequisite not found"),
        @ApiResponse (responseCode = "409", description = "Duplicate code")
    })
    public ResponseEntity<SubjectResponseDTO> updateSubject(@PathVariable Long id, @Valid @RequestBody SubjectRequestDTO subject) {
        SubjectResponseDTO updated = subjectService.updateSubject(id, subject);
        return ResponseEntity.ok(updated);
    }


    // delete a subject
    @DeleteMapping ("/{id}")
    @Operation (summary = "Delete a subject")
    @ApiResponses ({
        @ApiResponse (responseCode = "204", description = "Subject deleted successfully"),
        @ApiResponse (responseCode = "404", description = "Subject not found"),
        @ApiResponse (responseCode = "409", description = "Subject has associated sections or prerequisites")
    })
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }
}