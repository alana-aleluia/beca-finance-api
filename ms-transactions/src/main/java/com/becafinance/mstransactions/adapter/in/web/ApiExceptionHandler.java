package com.becafinance.mstransactions.adapter.in.web;
import com.becafinance.mstransactions.application.exception.*;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(TransactionNotFoundException.class)
    ResponseEntity<ProblemDetail> missing(TransactionNotFoundException exception) { return problem(404, "Transação não encontrada"); }
    @ExceptionHandler(DependencyUnavailableException.class)
    ResponseEntity<ProblemDetail> dependency(DependencyUnavailableException exception) { return problem(503, exception.getMessage()); }
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> validation(ConstraintViolationException exception) { return problem(400, "Dados inválidos"); }
    @ExceptionHandler(InvalidOperationException.class)
    ResponseEntity<ProblemDetail> invalid(InvalidOperationException exception) { return problem(400, exception.getMessage()); }
    @ExceptionHandler(FinancialConflictException.class)
    ResponseEntity<ProblemDetail> conflict(FinancialConflictException exception) { return problem(409, exception.getMessage()); }
    @ExceptionHandler({DataAccessException.class, org.springframework.transaction.TransactionException.class, jakarta.persistence.PersistenceException.class})
    ResponseEntity<ProblemDetail> persistence(RuntimeException exception) { return problem(503, "Falha de persistência; operação não confirmada"); }
    private ResponseEntity<ProblemDetail> problem(int status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail));
    }
}
