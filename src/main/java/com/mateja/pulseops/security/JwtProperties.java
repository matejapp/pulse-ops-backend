package com.mateja.pulseops.security;


import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Base64;


// Binds the pulseops.security.jwt.* properties (secret/issuer/access-token-ttl) into one
// immutable record. The secret comes from the PULSEOPS_JWT_SECRET env var via application.properties
// so it is never hard-coded or committed. Duration is parsed from strings like "3600s".
@ConfigurationProperties(prefix = "pulseops.security.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl) {
    // Turns the configured Base64 secret into the actual HMAC key used to sign/verify tokens.
    public SecretKey secretKey() {
        // The secret is stored Base64-encoded; decode back to raw bytes. Must be STANDARD Base64
        // (getDecoder), not URL-safe — a mismatched alphabet throws "Illegal base64 character".
        byte[] bytes = Base64.getDecoder().decode(secret);

        // HS256 requires a key of at least 256 bits = 32 bytes. Fail fast at startup with a clear
        // message rather than minting weak/insecure tokens or erroring cryptically later.
        if(bytes.length < 32){
            throw new IllegalStateException("Secret Key is too short");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
