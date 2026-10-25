package SIGA.backend.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import SIGA.backend.dtos.TeacherRequestDTO;
import SIGA.backend.dtos.TeacherResponseDTO;
import SIGA.backend.services.TeacherService;

import SIGA.backend.entities.Status;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;


@RestController 
@RequestMapping ("/api/teachers")
@RequiredArgsConstructor 
@Tag (name = "Teachers", description = "Teacher endpoints")

public class TeacherController {

    private final TeacherService teacherService;

    // get all teachers
    @GetMapping 
    @Operation (summary = "Get all teachers")
    @ApiResponse (responseCode = "200", description = "Teachers retrived successfully")
    public ResponseEntity<List<TeacherResponseDTO>> getAllTeachers() {
        List<TeacherResponseDTO> teachers = teacherService.getAllTeachers();
        return ResponseEntity.ok(teachers);
    }


    // get teacher by id 
    @GetMapping ("/{id}")
    @Operation (summary = "Get teacher by id")
    @ApiResponses ({
        @ApiResponse (responseCode = "200", description = "Teacher found"),
        @ApiResponse (responseCode = "404", description = "Teacher not found")
    })
    public ResponseEntity<TeacherResponseDTO> getTeacherById(@PathVariable Long id) {
        TeacherResponseDTO teacher = teacherService.getTeacherById(id);
        return ResponseEntity.ok(teacher);
    }


    // get teacher by rut 
    @GetMapping ("/rut/{rut}")
    @Operation (summary = "Get teacher by rut")
    @ApiResponses ({
        @ApiResponse (responseCode = "200", description = "Teacher found"),
        @ApiResponse (responseCode = "400", description = "Teacher not found")
    })
    public ResponseEntity<TeacherResponseDTO> getTeacherByRut(@PathVariable String rut) {
        TeacherResponseDTO teacher = teacherService.getTeacherByRut(rut);
        return ResponseEntity.ok(teacher);
    }


    // get teachers by status
    @GetMapping ("/status/{status}")
    @Operation (summary = "Get teachers by status")
    @ApiResponse (responseCode = "200", description = "Teachers retrieved successfully")
    public ResponseEntity<List<TeacherResponseDTO>> getTeachersByStatus(@PathVariable Status status) {
        List<TeacherResponseDTO> teachers = teacherService.getTeachersByStatus(status);
        return ResponseEntity.ok(teachers);
    }


    // get teachers by first name
    @GetMapping ("/first-name")
    @Operation (summary = "Get teachers by first name")
    @ApiResponse (responseCode = "200", description = "Teachers retrieved successfully")
    public ResponseEntity<List<TeacherResponseDTO>> getTeachersByFirstName(@PathVariable String firstName) {
        List<TeacherResponseDTO> teachers = teacherService.getTeachersByFirstName(firstName);
        return ResponseEntity.ok(teachers);
    }


    // get teachers by last name
    @GetMapping ("/last-name")
    @Operation (summary = "Get teachers by last name")
    @ApiResponse (responseCode = "200", description = "Teachers retrieved successfully")
    public ResponseEntity<List<TeacherResponseDTO>> getTeachersByLastName(@PathVariable String lastName) {
        List<TeacherResponseDTO> teachers = teacherService.getTeachersByLastName(lastName);
        return ResponseEntity.ok(teachers);
    }


    // create teacher
    @PostMapping 
    @Operation (summary = "create a new teacher")
    @ApiResponses ({
        @ApiResponse (responseCode = "201", description = "Teacher created successfully"),
        @ApiResponse (responseCode = "404", description = "Invalid teacher data"),
        @ApiResponse (responseCode = "409", description = "Teacher already exist")
    })
    public ResponseEntity<TeacherResponseDTO> createTeacher(@Valid @RequestBody TeacherRequestDTO teacher) {
        TeacherResponseDTO newTeacher = teacherService.createTeacher(teacher);
        URI location = URI.create("/api/teachers/" + newTeacher.getId());
        return ResponseEntity.created(location).body(newTeacher);
    }


    // update teacher
    @PutMapping ("/{id}")
    @Operation (summary = "Update teacher")
    @ApiResponses ({
        @ApiResponse (responseCode = "200", description = "Teacher updated successfully"),
        @ApiResponse (responseCode = "400", description = "Invalid teacher data"),
        @ApiResponse (responseCode = "404", description = "Teacher not found"),
        @ApiResponse (responseCode = "409", description = "Teacher rut or email already exist")
    })
    public ResponseEntity<TeacherResponseDTO> updateTeacher(@PathVariable Long id, @Valid @RequestBody TeacherRequestDTO teacher) {
        TeacherResponseDTO updated = teacherService.updateTeacher(id, teacher);
        return ResponseEntity.ok(updated);
    }


    // update teacher status
    @PutMapping ("/{id}/status")
    @Operation (summary = "Update teacher status")
    @ApiResponses ({
        @ApiResponse (responseCode = "200", description = "Teacher status updated successfully"),
        @ApiResponse (responseCode = "400", description = "Invalid teacher status"),
        @ApiResponse (responseCode = "404", description = "Teacher not found"),
        @ApiResponse (responseCode = "409", description = "Teacher has active sections")
    })
    public ResponseEntity<TeacherResponseDTO> updateTeacherStatus(@PathVariable Long id, @RequestParam Status status) {
        TeacherResponseDTO updated = teacherService.updateTeacherStatus(id, status);
        return ResponseEntity.ok(updated);
    }


    // delete teacher
    @DeleteMapping ("/{id}")
    @Operation (summary = "delete teacher")
    @ApiResponses ({
        @ApiResponse (responseCode = "204", description = "Teacher deleted successfully"),
        @ApiResponse (responseCode = "404", description = "Teacher not found"),
        @ApiResponse (responseCode = "409", description = "Teacher has active sections")
    })
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }
}