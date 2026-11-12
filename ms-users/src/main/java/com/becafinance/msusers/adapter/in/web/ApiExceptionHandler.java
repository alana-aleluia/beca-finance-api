package com.becafinance.msusers.adapter.in.web;
import com.becafinance.msusers.domain.exception.*;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.List;
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(com.becafinance.msusers.application.exception.InvalidCsvException.class)
    public ResponseEntity<ProblemDetail> invalidCsv(RuntimeException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage(), List.of());
    }
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> notFound(UserNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage(), List.of());
    }
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ProblemDetail> duplicate(DuplicateEmailException e) {
        return problem(HttpStatus.CONFLICT, e.getMessage(), List.of());
    }
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> invalid(ConstraintViolationException e) {
        return problem(HttpStatus.BAD_REQUEST, "Dados invalidos", e.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().toList());
    }
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status,
        org.springframework.web.context.request.WebRequest request) {
        var body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados invalidos");
        body.setProperty("errors", e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage()).sorted().toList());
        return handleExceptionInternal(e, body, headers, status, request);
    }
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail, List<String> errors) {
        var body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setProperty("errors", errors);
        return ResponseEntity.status(status).body(body);
    }
}
