package com.mateja.pulseops.auth.web;

import com.mateja.pulseops.auth.application.AuthService;
import com.mateja.pulseops.auth.application.EmailAlreadyRegisteredException;
import com.mateja.pulseops.auth.domain.Role;
import com.mateja.pulseops.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerShouldReturnCreatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-07-01T12:00:00Z");

        RegisterRequest request =
                new RegisterRequest("test@gmail.com", "password123");

        RegisterResponse response = new RegisterResponse(
                userId,
                "test@gmail.com",
                Role.RESPONDER,
                createdAt
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId")
                        .value(userId.toString()))
                .andExpect(jsonPath("$.email")
                        .value("test@gmail.com"))
                .andExpect(jsonPath("$.role")
                        .value("RESPONDER"))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-07-01T12:00:00Z"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void registerShouldReturnBadRequestForInvalidInput() throws Exception {
        RegisterRequest request =
                new RegisterRequest("invalid-email", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.detail")
                        .value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void registerShouldReturnConflictForDuplicateEmail() throws Exception {
        RegisterRequest request =
                new RegisterRequest("existing@gmail.com", "password123");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyRegisteredException(
                        "Email is already registered"
                ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title")
                        .value("Email Already in Use"))
                .andExpect(jsonPath("$.detail")
                        .value("Email is already registered"));

        verify(authService).register(any(RegisterRequest.class));
    }
}