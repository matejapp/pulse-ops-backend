package com.mateja.pulseops.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {


    // ENCODER = signs/mints tokens (used at login). HS256 is symmetric: the SAME secret key
    // both signs and verifies. ImmutableSecret wraps our secret as the signing key source.
    @Bean
    JwtEncoder jwtEncoder(JwtProperties props) {
        JWKSource<SecurityContext> jwks = new ImmutableSecret<>(props.secretKey());
        return new NimbusJwtEncoder(jwks);
    }

    // DECODER = verifies/parses tokens on every incoming request (used by the Resource Server).
    // withSecretKey + HS256 checks the signature was produced by our secret (rejects tampering).
    @Bean
    JwtDecoder jwtDecoder(JwtProperties props) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withSecretKey(props.secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        String expectedIssuer = props.issuer();

        // A valid signature isn't enough — we also assert WHO issued it and WHEN.
        // withIssuer: the "iss" claim must equal our configured issuer (rejects tokens
        //   signed by some other system that happens to share... it shouldn't, but defense-in-depth).
        // withTimestamp: reject expired tokens; the 1-minute arg is clock-skew leeway between hosts.
        OAuth2TokenValidator<Jwt> withIssuer = new JwtIssuerValidator(expectedIssuer);
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator(Duration.ofMinutes(1));

        // Delegating validator runs BOTH; the token must pass every validator in the chain.
        OAuth2TokenValidator<Jwt> validatorChain = new DelegatingOAuth2TokenValidator<>(withTimestamp, withIssuer);

        jwtDecoder.setJwtValidator(validatorChain);
        return  jwtDecoder;
    }
}
