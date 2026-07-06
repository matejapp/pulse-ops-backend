package com.mateja.pulseops.common.web;

import com.mateja.pulseops.auth.application.EmailAlreadyRegisteredException;
import com.mateja.pulseops.auth.application.InvalidCredentialsException;
import com.mateja.pulseops.httpmonitor.application.HttpMonitorNotFoundException;
import com.mateja.pulseops.incident.application.IncidentNotFoundException;
import com.mateja.pulseops.incident.domain.InvalidIncidentTransitionException;
import com.mateja.pulseops.monitoredservice.application.MonitoredServiceNotFoundException;
import com.mateja.pulseops.monitoredservice.application.ServiceAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.LinkedHashMap;
import java.util.Map;

// @RestControllerAdvice = one central place to turn exceptions thrown by ANY controller into
// HTTP responses. Keeps controllers/services free of try/catch; they just throw, we map here.
// ProblemDetail is the RFC 7807 standard error shape (type/title/status/detail + custom props),
// served as application/problem+json.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Domain exception from register() -> 409 Conflict: the request was valid but conflicts with
    // existing state (email taken).
    @ExceptionHandler(value = EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailAlreadyRegisteredException(EmailAlreadyRegisteredException ex) {

        ProblemDetail pb = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pb.setTitle("Email Already in Use");
        return pb;
    }

    // Thrown by Spring when a @Valid @RequestBody fails its Jakarta validation annotations -> 400.
    // We flatten the per-field errors into a {field: message} map so clients can show them inline.
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ProblemDetail pb = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation Failed");
        pb.setTitle("Bad Request");

        // LinkedHashMap preserves insertion order (stable output); putIfAbsent keeps the FIRST
        // message per field if a field has multiple violations, instead of overwriting.
        Map<String,String> fieldErrors = new LinkedHashMap<>();

        for(FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        // setProperty adds a NON-standard field to the JSON body alongside the RFC 7807 members.
        pb.setProperty("fieldErrors", fieldErrors);
        return pb;
    }

    // Bad login -> 401 Unauthorized. The message is intentionally generic (see AuthService.login)
    // to avoid leaking whether the email exists.
    @ExceptionHandler(value = InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentialsException(InvalidCredentialsException ex) {
        ProblemDetail pb = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,  ex.getMessage());
        pb.setTitle("Unauthorized");

        return pb;
    }

    @ExceptionHandler(value = ServiceAlreadyExistsException.class)
    public ProblemDetail handleServiceAlreadExistsException(ServiceAlreadyExistsException ex) {
        ProblemDetail pb = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pb.setTitle("Service Already Exists");
        return pb;
    }

    @ExceptionHandler(value = MonitoredServiceNotFoundException.class)
    public ProblemDetail handleMonitoredServiceNotFoundException(MonitoredServiceNotFoundException ex) {
        ProblemDetail pb = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pb.setTitle("Service Not Found");
        return pb;
    }

    @ExceptionHandler(value = HttpMonitorNotFoundException.class)
    public ProblemDetail handleHttpMonitorNotFoundException(HttpMonitorNotFoundException ex) {
        ProblemDetail pb = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pb.setTitle("Monitor Not Found");
        return pb;
    }

    @ExceptionHandler(value = InvalidIncidentTransitionException.class)
    public ProblemDetail handleInvalidIncidentTransitionException(InvalidIncidentTransitionException ex) {
        ProblemDetail pb = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pb.setTitle("Invalid Incident Transition");
        return pb;
    }

    @ExceptionHandler(value = IncidentNotFoundException.class)
    public  ProblemDetail handleIncidentNotFoundException(IncidentNotFoundException ex) {
        ProblemDetail pb = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pb.setTitle("Incident Not Found");
        return pb;
    }

}
