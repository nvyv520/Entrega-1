package SIGA.backend.controllers;

import SIGA.backend.entities.Status;

import SIGA.backend.dtos.CareerRequestDTO;
import SIGA.backend.dtos.CareerResponseDTO;

import SIGA.backend.services.CareerService;

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
@RequestMapping("/api/careers")
@RequiredArgsConstructor
@Tag(name = "Careers", description = "Career management endpoints")

public class CareerController {
    private final CareerService careerService;


    // get all careers
    @GetMapping 
    @Operation(summary = "Get all careers")
    public ResponseEntity<List<CareerResponseDTO>> getAllCareers() {
        List<CareerResponseDTO> careers = careerService.getAllCareers();
        return ResponseEntity.ok(careers);
    }


    // get career by id 
    @GetMapping("/{id}")
    @Operation(summary = "Get career by id")
    @ApiResponse(responseCode = "404", description = "Career not found")
    public ResponseEntity<CareerResponseDTO> getCareerById(@PathVariable Long id) {
        CareerResponseDTO career = careerService.getCareerById(id);
        return ResponseEntity.ok(career);
    }


    // create a new career
    @PostMapping
    @Operation(summary = "Create a new career")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Career created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid career data"),
            @ApiResponse(responseCode = "409", description = "Career code already exists")
    })    
    public ResponseEntity<CareerResponseDTO> createCareer(@Valid @RequestBody CareerRequestDTO career) {
        CareerResponseDTO newCareer = careerService.createCareer(career);
        URI location = URI.create("/api/careers/" + newCareer.getId());
        return ResponseEntity.created(location).body(newCareer);
    }


    // update a career by id
    @PutMapping("/{id}")
    @Operation(summary = "Update career by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Career updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid career data"),
            @ApiResponse(responseCode = "404", description = "Career not found"),
            @ApiResponse(responseCode = "409", description = "Career code already exists")
        })
    public ResponseEntity<CareerResponseDTO> updateCareer(@PathVariable Long id, @Valid @RequestBody CareerRequestDTO career) {
        CareerResponseDTO updatedCareer = careerService.updateCareer(id, career);
        return ResponseEntity.ok(updatedCareer);
    }

    
    // update career status
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update career status")
    @ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid career status"),
        @ApiResponse(responseCode = "404", description = "Career not found")
    })
    public ResponseEntity<CareerResponseDTO> updateCareerStatus(@PathVariable Long id, @RequestParam Status status) {
        CareerResponseDTO updatedCareer = careerService.updateCareerStatus(id, status);
        return ResponseEntity.ok(updatedCareer);
    }


    // delete a career by id
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete career by id")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Career not found"),
            @ApiResponse(responseCode = "409", description = "Career has students or study plans"),
            @ApiResponse(responseCode = "204", description = "Career deleted successfully")
    })
    public ResponseEntity<Void> deleteCareer(@PathVariable Long id) {
        careerService.deleteCareer(id);
        return ResponseEntity.noContent().build();
    }
}
