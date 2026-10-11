package dev.ricciarelli.supportdesk.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> domain(DomainException ex, HttpServletRequest request) {
        return problem(ex.status(), ex.code(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> fields(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(
                                e ->
                                        Map.of(
                                                "field",
                                                e.getField(),
                                                "message",
                                                e.getDefaultMessage() == null
                                                        ? "Valor inválido."
                                                        : e.getDefaultMessage()))
                        .toList();
        return problem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Revise os campos da requisição.",
                request,
                errors);
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        HandlerMethodValidationException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class
    })
    ResponseEntity<ProblemDetail> invalid(Exception ex, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "Parâmetro, enumeração ou JSON inválido.",
                request,
                null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> constraint(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "DATA_CONFLICT",
                "O registro conflita com uma restrição de integridade.",
                request,
                null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse error) {
            var response =
                    problem(
                            HttpStatus.valueOf(error.getStatusCode().value()),
                            "HTTP_ERROR",
                            "A requisição não pôde ser atendida.",
                            request,
                            null);
            return ResponseEntity.status(error.getStatusCode())
                    .headers(error.getHeaders())
                    .headers(response.getHeaders())
                    .body(response.getBody());
        }
        LOG.error("Falha interna no processamento de {}", request.getRequestURI(), ex);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Não foi possível concluir a operação.",
                request,
                null);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String code,
            String detail,
            HttpServletRequest request,
            Object errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("requestId", request.getAttribute("requestId"));
        if (errors != null) problem.setProperty("errors", errors);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
