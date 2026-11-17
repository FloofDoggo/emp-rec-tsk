package com.empik.coupons.exception;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CouponException.class)
    ProblemDetail handleBusinessFailure(CouponException ex) {
        return problem(ex.errorCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error");
        problem.setTitle("Internal error");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = problem(ErrorCode.INVALID_INPUT, "Request validation failed");
        problem.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    private static ProblemDetail problem(ErrorCode code, String detail) {
        HttpStatus status = statusOf(code);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code.name());
        return problem;
    }

    private static HttpStatus statusOf(ErrorCode code) {
        return switch (code) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case COUPON_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case COUNTRY_NOT_ALLOWED -> HttpStatus.FORBIDDEN;
            case COUPON_ALREADY_EXISTS, COUPON_EXHAUSTED, COUPON_ALREADY_REDEEMED -> HttpStatus.CONFLICT;
            case GEOLOCATION_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }
}