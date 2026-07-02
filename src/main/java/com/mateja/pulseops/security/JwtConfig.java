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


    @Bean
    JwtEncoder jwtEncoder(JwtProperties props) {
        JWKSource<SecurityContext> jwks = new ImmutableSecret<>(props.secretKey());
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties props) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withSecretKey(props.secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        String expectedIssuer = props.issuer();

        OAuth2TokenValidator<Jwt> withIssuer = new JwtIssuerValidator(expectedIssuer);
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator(Duration.ofMinutes(1));

        OAuth2TokenValidator<Jwt> validatorChain = new DelegatingOAuth2TokenValidator<>(withTimestamp, withIssuer);

        jwtDecoder.setJwtValidator(validatorChain);
        return  jwtDecoder;
    }
}
