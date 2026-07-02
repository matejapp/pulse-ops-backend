package com.mateja.pulseops.auth.web;

import com.mateja.pulseops.auth.application.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    // @Valid triggers the Jakarta annotations on RegisterRequest BEFORE the method body runs;
    // on failure Spring throws MethodArgumentNotValidException (handled globally -> 400), so the
    // service only ever sees structurally-valid input. @RequestBody binds the JSON to the record.
    // 201 Created is the correct status for a successful resource creation.
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest){
        RegisterResponse registeredUser = service.register(registerRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
    }

    // Login is not "creating" anything, so 200 OK (not 201). The controller stays thin: validate,
    // delegate to the service, wrap the result — all business logic lives in AuthService.
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        LoginResponse loginResponse = service.login(loginRequest);
        return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
    }
}
