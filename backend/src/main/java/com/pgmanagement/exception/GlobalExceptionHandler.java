package com.pgmanagement.exception;

import com.pgmanagement.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 400: client-side input errors ──────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        // e.g. malformed JSON, wrong types — useful message instead of bare 400
        String msg = "Invalid request body — please check the format.";
        if (e.getMostSpecificCause() != null && e.getMostSpecificCause().getMessage() != null) {
            String cause = e.getMostSpecificCause().getMessage();
            // Hide internal class names but keep the human-readable part
            if (cause.length() < 200) msg = "Invalid request body: " + cause;
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(msg));
    }

    /** Bean-validation (@NotNull / @Email / @NotBlank etc) failures from @Valid. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.error(msg.isBlank() ? "Validation failed" : msg));
    }

    /** A required @RequestParam was missing. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Missing required parameter: " + e.getParameterName()));
    }

    /** A path variable couldn't be converted, e.g. /api/owner/rooms/abc when Long expected. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(
                "Invalid value '" + e.getValue() + "' for parameter '" + e.getName() + "'"));
    }

    /** Numeric parsing failure (e.g. Integer.parseInt of a bad string in body). */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ApiResponse<Void>> handleNumberFormat(NumberFormatException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(
                "Expected a number but got: " + e.getMessage()));
    }

    // ── 401 ───────────────────────────────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid email or password"));
    }

    // ── 403 ───────────────────────────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied"));
    }

    // ── 405 ───────────────────────────────────────────────────────────
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(e.getMethod() + " is not allowed on this endpoint"));
    }

    // ── 409 / 400: constraint violations (unique key, FK, etc.) ───────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        String root = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        String friendly;
        if (root != null && root.toLowerCase().contains("unique")) {
            friendly = "Duplicate entry — this value already exists.";
        } else if (root != null && root.toLowerCase().contains("foreign key")) {
            friendly = "Cannot complete the operation — referenced record is in use elsewhere.";
        } else {
            friendly = "Database constraint violated.";
        }
        log.warn("DataIntegrityViolation: {}", root);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(friendly));
    }

    // ── Business-logic RuntimeException (404 / 403 / 500 based on message) ──
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException e) {
        String msg = e.getMessage();
        if (msg == null) msg = "Unexpected error";

        // Heuristics: most service-layer "not found" / "not authorized" use plain RuntimeException
        String lower = msg.toLowerCase();
        if (lower.contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(msg));
        }
        if (lower.contains("not authorized") || lower.contains("unauthorized")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(msg));
        }
        // For any other RuntimeException-with-message thrown by our service code,
        // treat it as a bad-request (the business rule rejected the input).
        log.info("Business rule rejected: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(msg));
    }

    // ── 500 catch-all (last resort — log and surface a useful hint) ──
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception e) {
        log.error("Unhandled exception", e);
        String simpleName = e.getClass().getSimpleName();
        String msg = e.getMessage() != null && e.getMessage().length() < 200
                ? simpleName + ": " + e.getMessage()
                : simpleName + " — see server logs for details";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(msg));
    }
}
