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
