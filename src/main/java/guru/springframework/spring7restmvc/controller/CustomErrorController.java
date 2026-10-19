package guru.springframework.spring7restmvc.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 19-10-2026
 */
@ControllerAdvice
public class CustomErrorController {

    // the entity check fails at commit: the field and its message sit in the innermost cause.
    // Answer with the same {field: message} list as handleBindErrors, whichever check failed.
    @ExceptionHandler
    ResponseEntity<List<Map<String, String>>> handleJPAViolations(TransactionSystemException exception){
        if (exception.getRootCause() instanceof ConstraintViolationException violationException) {
            List<Map<String, String>> errorList = violationException.getConstraintViolations().stream()
                    .map(violation -> Map.of(violation.getPropertyPath().toString(), violation.getMessage()))
                    .toList();

            return ResponseEntity.badRequest().body(errorList);
        }

        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<List<Map<String, String>>> handleBindErrors(MethodArgumentNotValidException exception){

        List<Map<String, String>> errorList = exception.getFieldErrors().stream()
                // Map.of rejects null, and a field error may come without a message
                .map(fieldError -> Map.of(fieldError.getField(),
                        Objects.requireNonNullElse(fieldError.getDefaultMessage(), "invalid value")))
                .toList();

        return ResponseEntity.badRequest().body(errorList);
    }
}
