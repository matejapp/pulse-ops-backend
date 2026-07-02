package com.mateja.pulseops.auth.application;

import com.mateja.pulseops.auth.domain.Role;
import com.mateja.pulseops.auth.domain.UserAccount;
import com.mateja.pulseops.auth.persistence.UserAccountRepo;
import com.mateja.pulseops.auth.web.RegisterRequest;
import com.mateja.pulseops.auth.web.RegisterResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private AuthService authService;

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
}