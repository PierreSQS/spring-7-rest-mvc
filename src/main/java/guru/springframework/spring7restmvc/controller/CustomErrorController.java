package guru.springframework.spring7restmvc.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 31-10-2026
 */
@ControllerAdvice
public class CustomErrorController {

    @ExceptionHandler
    ResponseEntity<List<Map<String, String>>> handleJPAViolations(TransactionSystemException exception){
        Throwable rootCause = exception.getRootCause();

        if (rootCause instanceof ConstraintViolationException cve){
            List<Map<String, String>> errorList = cve.getConstraintViolations().stream()
                    .map(violation -> {
                        Map<String, String> errorMap = new HashMap<>();
                        errorMap.put(violation.getPropertyPath().toString(), violation.getMessage());
                        return errorMap;
                    }).toList();

            return ResponseEntity.badRequest().body(errorList);
        }
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<List<Map<String, String>>> handleBindErrors(MethodArgumentNotValidException exception){

        List<Map<String, String>> errorList = exception.getFieldErrors().stream()
                .map(fieldError -> {
                    Map<String, String > errorMap = new HashMap<>();
                    errorMap.put(fieldError.getField(), fieldError.getDefaultMessage());
                    return errorMap;
                }).toList();

        return ResponseEntity.badRequest().body(errorList);
    }
}
