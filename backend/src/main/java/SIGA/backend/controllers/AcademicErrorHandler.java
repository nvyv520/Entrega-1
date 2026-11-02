package SIGA.backend.controllers;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {
        StudentController.class, TeacherController.class, CareerController.class,
        StudyPlanController.class, SubjectController.class, SectionController.class,
        AcademicPeriodController.class
})
public class AcademicErrorHandler {

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(org.springframework.http.converter.HttpMessageNotReadableException exception) {
        String field = "";
        if (exception.getCause() instanceof tools.jackson.core.JacksonException jsonException) {
            java.util.Map<String, String> labels = java.util.Map.of(
                    "semester", "Semestre", "year", "Año", "theoryHours", "Horas de teoría",
                    "exerciseHours", "Horas de ejercicios", "laboratoryHours", "Horas de laboratorio",
                    "sct", "Créditos SCT", "capacity", "Capacidad");
            field = jsonException.getPath().stream().map(reference -> reference.getPropertyName())
                    .filter(java.util.Objects::nonNull).map(labels::get)
                    .filter(java.util.Objects::nonNull).findFirst().map(label -> label + ": ").orElse("");
        }
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                field + "Revisa el formato de los datos: los campos numéricos deben contener enteros dentro del rango permitido"));
    }

    // Expose the service's actual rejection, including sections with pending grades.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleStatus(ResponseStatusException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(exception.getStatusCode(),
                exception.getReason() == null ? "No se pudo completar la operación" : exception.getReason());
        return ResponseEntity.status(exception.getStatusCode()).headers(exception.getHeaders()).body(detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? "Revisa los datos ingresados" : error.getDefaultMessage())
                .distinct().collect(java.util.stream.Collectors.joining(". "));
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(
                org.springframework.http.HttpStatus.BAD_REQUEST, message));
    }
}
