package com.mateja.pulseops.security;


import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Base64;


@ConfigurationProperties(prefix = "pulseops.security.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl) {
    public SecretKey secretKey() {
        byte[] bytes = Base64.getDecoder().decode(secret);

        if(bytes.length < 32){
            throw new IllegalStateException("Secret Key is too short");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
