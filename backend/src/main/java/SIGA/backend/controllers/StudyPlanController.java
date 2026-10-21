package SIGA.backend.controllers;

import SIGA.backend.entities.StudyPlanStatus;
import SIGA.backend.dtos.StudyPlanRequestDTO;
import SIGA.backend.dtos.StudyPlanResponseDTO;
import SIGA.backend.services.StudyPlanService;

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
@RequestMapping("/api/study-plans")
@RequiredArgsConstructor 
@Tag(name = "Study Plans", description = "Studyplans endpoints") 

public class StudyPlanController {
    private final StudyPlanService studyPlanService;

    // get all study plans
    @GetMapping 
    @Operation (summary = "Get all study plans")
    public ResponseEntity<List<StudyPlanResponseDTO>> getAllStudyPlans() {
        List<StudyPlanResponseDTO> studyPlans = studyPlanService.getAllStudyPlans();
        return ResponseEntity.ok(studyPlans);
    }


    // get study plan by id
    @GetMapping("/{id}")
    @Operation(summary = "Get study plan by id")
    @ApiResponse(responseCode = "404", description = "Study plan not found")
    public ResponseEntity<StudyPlanResponseDTO> getStudyPlanById(@PathVariable Long id) {
        StudyPlanResponseDTO studyPlan = studyPlanService.getStudyPlanById(id);
        return ResponseEntity.ok(studyPlan);
    }


    // get study plans by career id
    @GetMapping("/career/{careerId}")
    @Operation(summary = "Get study plans by career id")
    @ApiResponse(responseCode = "404", description = "Career not found")
    public ResponseEntity<List<StudyPlanResponseDTO>> getStudyPlansByCareerId(@PathVariable Long careerId) {
        List<StudyPlanResponseDTO> studyPlans = studyPlanService.getStudyPlansByCareerId(careerId);
        return ResponseEntity.ok(studyPlans);
    }


    // create a new study plan
    @PostMapping 
    @Operation (summary = "Create a new study plan")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Study plan created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid study plan data"),
        @ApiResponse(responseCode = "404", description = "Career not found"),
        @ApiResponse(responseCode = "409", description = "Study plan identifier already exists")
    })
    public ResponseEntity<StudyPlanResponseDTO> createStudyPlan(@Valid @RequestBody StudyPlanRequestDTO studyPlan) {
        StudyPlanResponseDTO newStudyPlan = studyPlanService.createStudyPlan(studyPlan);
        URI location = URI.create("/api/study-plans/" + newStudyPlan.getId());
        return ResponseEntity.created(location).body(newStudyPlan);
    }


    // update a study plan by id
    @PutMapping("/{id}")
    @Operation(summary = "Update study plan by id")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid study plan data or career change"),
            @ApiResponse(responseCode = "404", description = "Study plan not found"),
            @ApiResponse(responseCode = "409", description = "Study plan identifier already exists")
    })
    public ResponseEntity<StudyPlanResponseDTO> updateStudyPlan(@PathVariable Long id, @Valid @RequestBody StudyPlanRequestDTO studyPlan) {
        StudyPlanResponseDTO updatedStudyPlan = studyPlanService.updateStudyPlan(id, studyPlan);
        return ResponseEntity.ok(updatedStudyPlan);
    }


    // update study plan status
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update study plan status")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid study plan status"),
            @ApiResponse(responseCode = "404", description = "Study plan not found")
    })
    public ResponseEntity<StudyPlanResponseDTO> updateStudyPlanStatus(@PathVariable Long id, @RequestParam StudyPlanStatus status) {
        StudyPlanResponseDTO updatedStudyPlan = studyPlanService.updateStudyPlanStatus(id, status);
        return ResponseEntity.ok(updatedStudyPlan);
    }


    // delete a study plan by id
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete study plan by id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Study plan deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Study plan not found"),
            @ApiResponse(responseCode = "409", description = "Study plan has students or subjects")
    })
    public ResponseEntity<Void> deleteStudyPlan(@PathVariable Long id) {
        studyPlanService.deleteStudyPlan(id);
        return ResponseEntity.noContent().build();
    }

}