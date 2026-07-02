package com.mateja.pulseops.auth.application;

import com.mateja.pulseops.auth.domain.Role;
import com.mateja.pulseops.auth.domain.UserAccount;
import com.mateja.pulseops.auth.persistence.UserAccountRepo;
import com.mateja.pulseops.auth.web.LoginRequest;
import com.mateja.pulseops.auth.web.LoginResponse;
import com.mateja.pulseops.auth.web.RegisterRequest;
import com.mateja.pulseops.auth.web.RegisterResponse;
import com.mateja.pulseops.security.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepo repo;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtEncoder jwtEncoder;

    private JwtProperties jwtProperties;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // secret is never decoded here (secretKey() is only used by JwtConfig),
        // so any placeholder works; issuer + ttl are what login reads.
        jwtProperties = new JwtProperties("test-secret", "pulseops", Duration.ofSeconds(3600));
        authService = new AuthService(repo, encoder, jwtEncoder, jwtProperties);
    }

    @Test
    void registerShouldNormalizeEmailEncodePasswordAndAssignResponderRole() {
        String rawEmail = "  Test@Gmail.com ";
        String normalizedEmail = "test@gmail.com";
        String password = "password123";
        String passwordHash = "known-password-hash";

        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        RegisterRequest request = new RegisterRequest(rawEmail, password);
        UserAccount savedAccount = org.mockito.Mockito.mock(UserAccount.class);

        when(repo.existsByEmailIgnoreCase(normalizedEmail))
                .thenReturn(false);
        when(encoder.encode(password))
                .thenReturn(passwordHash);
        when(repo.saveAndFlush(any(UserAccount.class)))
                .thenReturn(savedAccount);

        when(savedAccount.getUserId()).thenReturn(userId);
        when(savedAccount.getEmail()).thenReturn(normalizedEmail);
        when(savedAccount.getRole()).thenReturn(Role.RESPONDER);
        when(savedAccount.getCreatedAt()).thenReturn(createdAt);

        RegisterResponse response = authService.register(request);

        ArgumentCaptor<UserAccount> captor =
                ArgumentCaptor.forClass(UserAccount.class);

        verify(repo).saveAndFlush(captor.capture());

        UserAccount accountToSave = captor.getValue();

        assertEquals(normalizedEmail, accountToSave.getEmail());
        assertEquals(passwordHash, accountToSave.getPasswordHash());
        assertEquals(Role.RESPONDER, accountToSave.getRole());

        assertEquals(userId, response.userId());
        assertEquals(normalizedEmail, response.email());
        assertEquals(Role.RESPONDER, response.role());
        assertEquals(createdAt, response.createdAt());

        verify(repo).existsByEmailIgnoreCase(normalizedEmail);
        verify(encoder).encode(password);
    }

    @Test
    void registerShouldRejectDuplicateEmailWithoutEncodingOrSaving() {
        String rawEmail = "  Existing@Gmail.com ";
        String normalizedEmail = "existing@gmail.com";
        String password = "password123";

        RegisterRequest request = new RegisterRequest(rawEmail, password);

        when(repo.existsByEmailIgnoreCase(normalizedEmail))
                .thenReturn(true);

        EmailAlreadyRegisteredException exception = assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> authService.register(request)
        );

        assertEquals("Email is already registered", exception.getMessage());

        verify(repo).existsByEmailIgnoreCase(normalizedEmail);
        verify(repo, never()).saveAndFlush(any(UserAccount.class));
        verifyNoInteractions(encoder);
    }

    @Test
    void loginShouldReturnTokenForValidCredentials() {
        String rawEmail = "  Test@Gmail.com ";
        String normalizedEmail = "test@gmail.com";
        String password = "password123";
        String passwordHash = "known-password-hash";
        UUID userId = UUID.randomUUID();

        LoginRequest request = new LoginRequest(rawEmail, password);
        UserAccount user = mock(UserAccount.class);

        when(repo.findByEmailIgnoreCase(normalizedEmail))
                .thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn(passwordHash);
        when(encoder.matches(password, passwordHash)).thenReturn(true);
        when(user.getUserId()).thenReturn(userId);
        when(user.getEmail()).thenReturn(normalizedEmail);
        when(user.getRole()).thenReturn(Role.RESPONDER);

        Jwt fakeJwt = Jwt.withTokenValue("fake-token")
                .header("alg", "HS256")
                .claim("sub", userId.toString())
                .build();
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(fakeJwt);

        LoginResponse response = authService.login(request);

        assertEquals("fake-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600, response.expiresIn());

        verify(repo).findByEmailIgnoreCase(normalizedEmail);
        verify(encoder).matches(password, passwordHash);
        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void loginShouldThrowWhenEmailNotFound() {
        String email = "missing@gmail.com";
        LoginRequest request = new LoginRequest(email, "password123");

        when(repo.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());

        verify(repo).findByEmailIgnoreCase(email);
        verifyNoInteractions(encoder, jwtEncoder);
    }

    @Test
    void loginShouldThrowWhenPasswordDoesNotMatch() {
        String email = "test@gmail.com";
        String password = "wrong-password";
        String passwordHash = "known-password-hash";

        LoginRequest request = new LoginRequest(email, password);
        UserAccount user = mock(UserAccount.class);

        when(repo.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn(passwordHash);
        when(encoder.matches(password, passwordHash)).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());

        verify(encoder).matches(password, passwordHash);
        verifyNoInteractions(jwtEncoder);
    }
}