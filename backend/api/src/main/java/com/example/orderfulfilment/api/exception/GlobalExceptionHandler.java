package com.example.orderfulfilment.api.exception;

import com.example.orderfulfilment.api.dto.ErrorResponse;
import com.example.orderfulfilment.service.exception.InvalidStatusTransitionException;
import com.example.orderfulfilment.service.exception.OrderNotFoundException;
import com.example.orderfulfilment.service.exception.ServiceUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralised exception handling for all REST controllers.
 *
 * Design pattern: Chain of Responsibility
 *   Handlers are ordered from most-specific to most-generic.
 *   Each handler maps exactly one exception type to a consistent ErrorResponse.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // 400 Bad Request
    // -------------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex,
                                          HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .sorted()
                .collect(Collectors.joining("; "));
        log.warn("Validation failed for {}: {}", req.getRequestURI(), message);
        return ErrorResponse.of(400, "Bad Request", message, req.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex,
                                               HttpServletRequest req) {
        log.warn("Illegal argument at {}: {}", req.getRequestURI(), ex.getMessage());
        return ErrorResponse.of(400, "Bad Request", ex.getMessage(), req.getRequestURI());
    }

    // -------------------------------------------------------------------------
    // 404 Not Found
    // -------------------------------------------------------------------------

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleOrderNotFound(OrderNotFoundException ex,
                                             HttpServletRequest req) {
        log.warn("Order not found at {}: {}", req.getRequestURI(), ex.getMessage());
        return ErrorResponse.of(404, "Not Found", ex.getMessage(), req.getRequestURI());
    }

    // -------------------------------------------------------------------------
    // 409 Conflict
    // -------------------------------------------------------------------------

    @ExceptionHandler(InvalidStatusTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInvalidTransition(InvalidStatusTransitionException ex,
                                                 HttpServletRequest req) {
        log.warn("Invalid status transition at {}: {}", req.getRequestURI(), ex.getMessage());
        return ErrorResponse.of(409, "Conflict", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                              HttpServletRequest req) {
        log.warn("Optimistic lock conflict at {}: {}", req.getRequestURI(), ex.getMessage());
        return ErrorResponse.of(409, "Conflict",
                "Resource was modified concurrently, please retry.", req.getRequestURI());
    }

    // -------------------------------------------------------------------------
    // 503 Service Unavailable
    // -------------------------------------------------------------------------

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleServiceUnavailable(ServiceUnavailableException ex,
                                                  HttpServletRequest req) {
        log.error("Service unavailable at {}: {}", req.getRequestURI(), ex.getMessage());
        return ErrorResponse.of(503, "Service Unavailable", ex.getMessage(), req.getRequestURI());
    }

    // -------------------------------------------------------------------------
    // 500 Internal Server Error (catch-all)
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ErrorResponse.of(500, "Internal Server Error",
                "An unexpected error occurred.", req.getRequestURI());
    }
}
