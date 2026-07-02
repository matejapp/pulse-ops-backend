package com.mateja.pulseops.auth.application;


import com.mateja.pulseops.auth.domain.Role;
import com.mateja.pulseops.auth.domain.UserAccount;
import com.mateja.pulseops.auth.persistence.UserAccountRepo;
import com.mateja.pulseops.auth.web.RegisterRequest;
import com.mateja.pulseops.auth.web.RegisterResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserAccountRepo repo;
    private final PasswordEncoder encoder;

    public AuthService(UserAccountRepo repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder ;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest){
        String email = registerRequest.email().trim().toLowerCase(Locale.ROOT);
        String password = registerRequest.password();

        if(repo.existsByEmailIgnoreCase(email)) {
            throw  new EmailAlreadyRegisteredException("Email is already registered");
        }

        String hashedPassword = encoder.encode(password);
        UserAccount newUser = new UserAccount(email,hashedPassword, Role.RESPONDER);
        UserAccount registeredUser = repo.saveAndFlush(newUser);

        return new RegisterResponse(registeredUser.getUserId(), registeredUser.getEmail(), registeredUser.getRole(), registeredUser.getCreatedAt());
    }
}
