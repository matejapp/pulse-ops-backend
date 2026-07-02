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
        // Normalize once, up front: trim stray spaces and lowercase with Locale.ROOT (locale-independent,
        // avoids the Turkish-i bug where "I".toLowerCase() differs by locale). We store emails lowercased
        // so lookups are consistent.
        String email = registerRequest.email().trim().toLowerCase(Locale.ROOT);
        String password = registerRequest.password();

        // Friendly pre-check for the common case. NOTE: this races the DB unique index under
        // concurrent signups — two requests can both pass this check, then one hits the index and
        // currently surfaces as a 500. Known tech debt: translate that DB violation to this same 409.
        if(repo.existsByEmailIgnoreCase(email)) {
            throw  new EmailAlreadyRegisteredException("Email is already registered");
        }

        // Never store the raw password — hash it. Public registration is always RESPONDER;
        // ADMIN is granted out-of-band (a client cannot self-assign a role).
        String hashedPassword = encoder.encode(password);
        UserAccount newUser = new UserAccount(email,hashedPassword, Role.RESPONDER);
        // saveAndFlush forces the INSERT now (inside this tx) so the DB-generated id + created_at
        // are populated on the returned entity for the response.
        UserAccount registeredUser = repo.saveAndFlush(newUser);

        return new RegisterResponse(registeredUser.getUserId(), registeredUser.getEmail(), registeredUser.getRole(), registeredUser.getCreatedAt());
    }


    public LoginResponse login(LoginRequest loginRequest){
        String normalizedEmail =  loginRequest.email().trim().toLowerCase(Locale.ROOT);
        Optional<UserAccount> existingUser = repo.findByEmailIgnoreCase(normalizedEmail);

        // SECURITY: both "no such email" and "wrong password" throw the SAME exception with the
        // SAME generic message. Revealing "email not found" vs "wrong password" would let an
        // attacker enumerate which emails are registered (account enumeration).
        if(existingUser.isEmpty()) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        UserAccount user  = existingUser.get();

        // encoder.matches re-hashes the raw attempt with the stored salt and compares — we never
        // decrypt the stored hash (BCrypt is one-way). Note the '!': throw when it does NOT match.
        if(!encoder.matches(loginRequest.password(), user.getPasswordHash())){
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = generateToken(user);
        // expiresIn is a hint for the client (seconds until expiry); the real expiry is the signed
        // "exp" claim inside the token, which the server enforces on every request.
        return new LoginResponse(token, "Bearer", jwtProperties.accessTokenTtl().toSeconds());
    }

    // Builds and signs the access token. Claims are the facts we assert about the user; because the
    // token is signed, the client cannot tamper with them without invalidating the signature.
    private String generateToken(UserAccount user) {
        Instant now = Instant.now();
        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())          // iss  — who minted it (validated on the way back in)
                .issuedAt(now)                            // iat  — when
                .expiresAt(now.plus(jwtProperties.accessTokenTtl())) // exp — when it stops being valid
                .subject(user.getUserId().toString())     // sub  — stable user id, NOT the email (emails change)
                .claim("email", user.getEmail())
                .claim("roles", List.of(user.getRole().name())) // plural + a list: matches the SecurityConfig converter; .name() gives the exact enum text
                .build();
        // Header declares the signing algorithm; must match what the decoder expects (HS256).
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header,claimsSet)).getTokenValue();
    }
}
