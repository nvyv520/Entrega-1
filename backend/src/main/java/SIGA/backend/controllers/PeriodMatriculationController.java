package SIGA.backend.controllers;

import SIGA.backend.dtos.PeriodMatriculationRequestDTO;
import SIGA.backend.dtos.PeriodMatriculationResponseDTO;

import SIGA.backend.services.PeriodMatriculationService;

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
@RequestMapping("/api/period-matriculations")
@RequiredArgsConstructor
@Tag(name = "Period Matriculations", description = "Period matriculation endpoints")
public class PeriodMatriculationController {

    private final PeriodMatriculationService matriculationService;


    // get all matriculations
    @GetMapping
    @Operation(summary = "Get all matriculations")
    @ApiResponse(responseCode = "200", description = "Matriculations retrieved successfully")
    public ResponseEntity<List<PeriodMatriculationResponseDTO>> getAllMatriculations() {
        List<PeriodMatriculationResponseDTO> matriculations = matriculationService.getAllMatriculations();
        return ResponseEntity.ok(matriculations);
    }


    // get matriculation by id
    @GetMapping("/{id}")
    @Operation(summary = "Get matriculation by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matriculation found"),
            @ApiResponse(responseCode = "404", description = "Matriculation not found")
    })
    public ResponseEntity<PeriodMatriculationResponseDTO> getMatriculationById(@PathVariable Long id) {
        PeriodMatriculationResponseDTO matriculation = matriculationService.getMatriculationById(id);
        return ResponseEntity.ok(matriculation);
    }


    // get matriculations by student
    @GetMapping("/student/{studentId}")
    @Operation(summary = "Get matriculations by student")
    @ApiResponse(responseCode = "200", description = "Matriculations retrieved successfully")
    public ResponseEntity<List<PeriodMatriculationResponseDTO>> getMatriculationsByStudent(@PathVariable Long studentId) {
        List<PeriodMatriculationResponseDTO> matriculations = matriculationService.getMatriculationsByStudent(studentId);
        return ResponseEntity.ok(matriculations);
    }


    // get matriculations by academic period
    @GetMapping("/academic-period/{periodId}")
    @Operation(summary = "Get matriculations by academic period")
    @ApiResponse(responseCode = "200", description = "Matriculations retrieved successfully")
    public ResponseEntity<List<PeriodMatriculationResponseDTO>> getMatriculationsByAcademicPeriod(@PathVariable Long periodId) {
        List<PeriodMatriculationResponseDTO> matriculations = matriculationService.getMatriculationsByAcademicPeriod(periodId);
        return ResponseEntity.ok(matriculations);
    }


    // create matriculation
    @PostMapping
    @Operation(summary = "Create a new matriculation")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Matriculation created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid matriculation data"),
            @ApiResponse(responseCode = "404", description = "Student or academic period not found"),
            @ApiResponse(responseCode = "409", description = "Matriculation already exists or is not allowed")
    })
    public ResponseEntity<PeriodMatriculationResponseDTO> createMatriculation(@Valid @RequestBody PeriodMatriculationRequestDTO matriculation) {
        PeriodMatriculationResponseDTO newMatriculation = matriculationService.createMatriculation(matriculation);
        URI location = URI.create("/api/period-matriculations/" + newMatriculation.getId());
        return ResponseEntity.created(location).body(newMatriculation);
    }
}
