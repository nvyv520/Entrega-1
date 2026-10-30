package SIGA.backend.controllers;

import SIGA.backend.entities.StudentStatus;

import SIGA.backend.dtos.StudentRequestDTO;
import SIGA.backend.dtos.StudentResponseDTO;
import SIGA.backend.dtos.StudentAcademicHistoryDTO;

import SIGA.backend.services.StudentService;

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

import java.util.List;
import java.net.URI;


@RestController
@RequestMapping ("/api/students")
@RequiredArgsConstructor 
@Tag (name = "Students", description = "Students endpoints")
public class StudentController {

    private final StudentService studentService;


    // get authenticated student
    @GetMapping("/me")
    @Operation(summary = "Get authenticated student")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student found"),
        @ApiResponse(responseCode = "404", description = "Student not linked to Keycloak")
    })
    public ResponseEntity<StudentResponseDTO> getCurrentStudent(@AuthenticationPrincipal Jwt jwt) {
        StudentResponseDTO student = studentService.getCurrentStudent(jwt.getSubject());
        return ResponseEntity.ok(student);
    }

    
    // get authenticated student academic history
    @GetMapping("/me/academic-history")
    @Operation(summary = "Get authenticated student academic history")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Academic history retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Student not linked to Keycloak")
    })
    public ResponseEntity<List<StudentAcademicHistoryDTO>> getCurrentStudentAcademicHistory(@AuthenticationPrincipal Jwt jwt) {
        List<StudentAcademicHistoryDTO> history = studentService.getCurrentStudentAcademicHistory(jwt.getSubject());
        return ResponseEntity.ok(history);
    }


    // get all students
    @GetMapping 
    @Operation (summary = "Get all students")
    @ApiResponse (responseCode = "200", description = "Students retrieved successfully")
    public ResponseEntity<List<StudentResponseDTO>> getAllStudents() {
        List<StudentResponseDTO> students =  studentService.getAllStudents();
        return ResponseEntity.ok(students);
    }


    // get student by id 
    @GetMapping ("/{id}")
    @Operation (summary = "Get student by id")
    @ApiResponses ({
        @ApiResponse (responseCode = "200", description = "Student found"),
        @ApiResponse (responseCode = "404", description = "Student not found")
    })
    public ResponseEntity<StudentResponseDTO> getStudentById(@PathVariable Long id) {
        StudentResponseDTO student =studentService.getStudentById(id);
        return ResponseEntity.ok(student);
    }


    // get student by rut 
    @GetMapping ("/rut/{rut}")
    @Operation (summary = "Get student by rut")
    @ApiResponses ({
        @ApiResponse (responseCode = "200", description = "Student found"),
        @ApiResponse (responseCode = "404", description = "Student not found")
    })
    public ResponseEntity<StudentResponseDTO> getStudentByRut(@PathVariable String rut) {
        StudentResponseDTO student = studentService.getStudentByRut(rut);
        return ResponseEntity.ok(student);
    }


    // get students by career id
    @GetMapping ("/careers/{careerId}")
    @Operation (summary = "Get students by career")
    @ApiResponses ({
        @ApiResponse (responseCode = "200", description = "Student retrieved successfully"),
        @ApiResponse (responseCode = "404", description = "Career not found")
    })
    public ResponseEntity<List<StudentResponseDTO>> getStudentsByCareerId(@PathVariable Long careerId) {
        List<StudentResponseDTO> students = studentService.getStudentsByCareerId(careerId);
        return ResponseEntity.ok(students);
    }


    // get students by status
    @GetMapping ("/status/{status}")
    @Operation (summary = "Get students by status")
    @ApiResponse (responseCode = "200", description = "Student retrieved successfully")
    public ResponseEntity<List<StudentResponseDTO>> getStudentsByStatus(@PathVariable StudentStatus status) {
        List<StudentResponseDTO> students = studentService.getStudentsByStatus(status);
        return ResponseEntity.ok(students);
    }


    // get student academic history
    @GetMapping("/{id}/academic-history")
    @Operation(summary = "Get student academic history")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Academic history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    public ResponseEntity<List<StudentAcademicHistoryDTO>> getStudentAcademicHistory(@PathVariable Long id) {
        List<StudentAcademicHistoryDTO> history = studentService.getStudentAcademicHistory(id);
        return ResponseEntity.ok(history);
    }


    // create a student
    @PostMapping 
    @Operation (summary = "Create a new student")
    @ApiResponses ({
        @ApiResponse (responseCode = "201", description = "Student created successfully"),
        @ApiResponse (responseCode = "400", description = "Invalid student data"),
        @ApiResponse (responseCode = "404", description = "Career not found"),
        @ApiResponse (responseCode = "409", description = "Student already exist or career unavailable")
    })
    public ResponseEntity<StudentResponseDTO> createStudent(@Valid @RequestBody StudentRequestDTO student) {
        StudentResponseDTO newStudent = studentService.createStudent(student);
        URI location = URI.create("/api/students/" + newStudent.getId());
        return ResponseEntity.created(location).body(newStudent);
    }


    // update a student
    @PutMapping ("/{id}")
    @Operation (summary = "Update student by id")
    @ApiResponses ({
        @ApiResponse (responseCode = "200", description = "Student updated successfully"),
        @ApiResponse (responseCode = "400", description = "Invalid student data"),
        @ApiResponse (responseCode = "404", description = "Student, career or study plan not found"),
        @ApiResponse (responseCode = "409", description = "Duplicate data or restricted academic changes")
    })
    public ResponseEntity<StudentResponseDTO> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentRequestDTO student) {
        StudentResponseDTO updated =studentService.updateStudent(id, student);
        return ResponseEntity.ok(updated);
    }


    // update a student status
    @PatchMapping ("/{id}/status")
    @Operation (summary = "Update student status")
    @ApiResponses ({
        @ApiResponse (responseCode = "200", description = "Student status updated successfully"),
        @ApiResponse (responseCode = "400", description = "Invalid student status"),
        @ApiResponse (responseCode = "404", description = "Student not found"),
        @ApiResponse (responseCode = "409", description = "Student status cant be changed")
    })
    public ResponseEntity<StudentResponseDTO> updateStudentStatus(@PathVariable Long id, @RequestParam StudentStatus status) {
        StudentResponseDTO updated = studentService.updateStudentStatus(id, status);
        return ResponseEntity.ok(updated);
    }


    // delete a student
    @DeleteMapping ("/{id}")
    @Operation (summary = "Delete student by id")
    @ApiResponses ({
        @ApiResponse (responseCode = "204", description = "Student deleted successfully"),
        @ApiResponse (responseCode = "404", description = "Student not found"),
        @ApiResponse (responseCode = "409", description = "Student has academic information")
    })
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    // link student with Keycloak
    @PatchMapping("/{id}/keycloak")
    @Operation(summary = "Link student with Keycloak user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student linked successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid Keycloak user ID"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Student or Keycloak user not found"),
            @ApiResponse(responseCode = "409", description = "Invalid or duplicate Keycloak association"),
            @ApiResponse(responseCode = "502", description = "Keycloak communication error")
    })
    public ResponseEntity<StudentResponseDTO> linkStudentKeycloak(
            @PathVariable Long id, @RequestParam String keycloakSubject) {
        StudentResponseDTO updated = studentService.linkStudentKeycloak(id, keycloakSubject);
        return ResponseEntity.ok(updated);
    }
}