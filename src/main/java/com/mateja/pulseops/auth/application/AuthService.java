package com.mateja.pulseops.auth.application;


import com.mateja.pulseops.auth.domain.Role;
import com.mateja.pulseops.auth.domain.UserAccount;
import com.mateja.pulseops.auth.persistence.UserAccountRepo;
import com.mateja.pulseops.auth.web.LoginRequest;
import com.mateja.pulseops.auth.web.LoginResponse;
import com.mateja.pulseops.auth.web.RegisterRequest;
import com.mateja.pulseops.auth.web.RegisterResponse;
import com.mateja.pulseops.security.JwtProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AuthService {

    private final UserAccountRepo repo;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public AuthService(UserAccountRepo repo, PasswordEncoder encoder,  JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
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


    public LoginResponse login(LoginRequest loginRequest){
        String normalizedEmail =  loginRequest.email().trim().toLowerCase(Locale.ROOT);
        Optional<UserAccount> existingUser = repo.findByEmailIgnoreCase(normalizedEmail);

        if(existingUser.isEmpty()) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        UserAccount user  = existingUser.get();

        if(!encoder.matches(loginRequest.password(), user.getPasswordHash())){
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = generateToken(user);
        return new LoginResponse(token, "Bearer", jwtProperties.accessTokenTtl().toSeconds());
    }

    private String generateToken(UserAccount user) {
        Instant now = Instant.now();
        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.accessTokenTtl()))
                .subject(user.getUserId().toString())
                .claim("email", user.getEmail())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header,claimsSet)).getTokenValue();
    }
}
