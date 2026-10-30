package SIGA.backend.controllers;

import SIGA.backend.entities.AcademicPeriodStatus;


import SIGA.backend.dtos.AcademicPeriodRequestDTO;
import SIGA.backend.dtos.AcademicPeriodResponseDTO;

import SIGA.backend.services.AcademicPeriodService;

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
@RequestMapping("/api/academic-periods")
@RequiredArgsConstructor 
@Tag (name = "Academic periods", description = "Academic periods endpoints")

public class AcademicPeriodController {

    private final AcademicPeriodService academicPeriodService;

    // get all academic periods
    @GetMapping 
    @Operation (summary = "Get all acadmeic periods")
    @ApiResponse (responseCode = "200", description = "Academic periods retrieved successfully")
    public ResponseEntity<List<AcademicPeriodResponseDTO>> getAllAcademicPeriods() {
        List<AcademicPeriodResponseDTO> periods = academicPeriodService.getAllAcademicPeriods();
        return ResponseEntity.ok(periods);
    }


    // get academic period by id
    @GetMapping ("/{id}")
    @Operation (summary = "Get academic period by id")
    @ApiResponses ({
        @ApiResponse (responseCode = "200", description = "Academic period found"),
        @ApiResponse (responseCode = "404", description = "Academic period not found")
    })
    public ResponseEntity<AcademicPeriodResponseDTO> getAcademicPeriodById(@PathVariable Long id) {
        AcademicPeriodResponseDTO period = academicPeriodService.getAcademicPeriodById(id);
        return ResponseEntity.ok(period);
    }


    // get academic period by code
    @GetMapping ("/code/{code}")
    @Operation (summary = "Get academic period by code")
    @ApiResponses ({
        @ApiResponse (responseCode = "200", description = "Academic period found"),
        @ApiResponse (responseCode = "404", description = "Academic period not found")
    })
    public ResponseEntity<AcademicPeriodResponseDTO> getAcademicPeriodByCode(@PathVariable String code) {
        AcademicPeriodResponseDTO period = academicPeriodService.getAcademicPeriodByCode(code);
        return ResponseEntity.ok(period);
    }


    // get academic periods by year
    @GetMapping ("/year/{year}")
    @Operation (summary = "Get academic periods by year")
    @ApiResponse (responseCode = "200", description = "Academic periods retrieved successfully")
    public ResponseEntity<List<AcademicPeriodResponseDTO>> getAcademicPeriodsByYear(@PathVariable Integer year) {
        List<AcademicPeriodResponseDTO> periods = academicPeriodService.getAcademicPeriodsByYear(year);
        return ResponseEntity.ok(periods);
    }


    // get academic periods by status
    @GetMapping ("/status/{status}")
    @Operation (summary = "Get academic periods by status")
    @ApiResponse (responseCode = "200", description = "Academic periods retrieved successfully")
    public ResponseEntity<List<AcademicPeriodResponseDTO>> getAcademicPeriodsByStatus(@PathVariable AcademicPeriodStatus status) {
        List<AcademicPeriodResponseDTO> periods = academicPeriodService.getAcademicPeriodsByStatus(status);
        return ResponseEntity.ok(periods);
    }


    // create academic period
    @PostMapping 
    @Operation (summary = "Create a new academic period")
    @ApiResponses ({
        @ApiResponse (responseCode = "201", description = "Academic period created successfully"),
        @ApiResponse (responseCode = "400", description = "Invalid acadmeic period data"),
        @ApiResponse (responseCode = "409", description = "Academic period already exists") 
    })
    public ResponseEntity<AcademicPeriodResponseDTO> createAcademicPeriod(@Valid @RequestBody AcademicPeriodRequestDTO period) {
        AcademicPeriodResponseDTO newPeriod = academicPeriodService.createAcademicPeriod(period);
        URI location = URI.create("/api/academic-periods/" + newPeriod.getId());
        return ResponseEntity.created(location).body(newPeriod);
    }


    // update academic period
    @PutMapping ("/{id}")
    @Operation (summary = "Update academic period")
    @ApiResponses ({
        @ApiResponse (responseCode = "200", description = "Academic period updated successfully"),
        @ApiResponse (responseCode = "400", description = "Invalid academic period data"),
        @ApiResponse (responseCode = "404", description = "Academic period not found"),
        @ApiResponse (responseCode = "409", description = "Academic period cant be updated") 
    })
    public ResponseEntity<AcademicPeriodResponseDTO> updateAcademicPeriod(@PathVariable Long id, @Valid @RequestBody AcademicPeriodRequestDTO period) {
        AcademicPeriodResponseDTO updated = academicPeriodService.updateAcademicPeriod(id, period);
        return ResponseEntity.ok(updated);
    }


    // delete academic period
    @DeleteMapping ("/{id}")
    @Operation (summary = "Delete academic period")
    @ApiResponses ({
        @ApiResponse (responseCode = "204", description = "Academic period deleted successfully"),
        @ApiResponse (responseCode = "404", description = "Academic period not found"),
        @ApiResponse (responseCode = "409", description = "Academic period has associated information or is closed") 
    })
    public ResponseEntity<Void> deleteAcademicPeriod(@PathVariable Long id) {
        academicPeriodService.deleteAcademicPeriod(id);
        return ResponseEntity.noContent().build();
    }



    // close academic period
    @PatchMapping("/{id}/close")
    @Operation(summary = "Close academic period")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Academic period closed successfully"),
            @ApiResponse(responseCode = "404", description = "Academic period not found"),
            @ApiResponse(responseCode = "409", description = "Academic period cannot be closed")
    })
    public ResponseEntity<AcademicPeriodResponseDTO> closeAcademicPeriod(@PathVariable Long id) {
        AcademicPeriodResponseDTO closedPeriod = academicPeriodService.closeAcademicPeriod(id);
        return ResponseEntity.ok(closedPeriod);
    }

}
