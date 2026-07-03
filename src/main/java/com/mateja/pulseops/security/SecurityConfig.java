package com.mateja.pulseops.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecurityConfig {



    // The SecurityFilterChain bean IS the security config: every HTTP request passes
    // through this chain of filters before it reaches a controller. Spring Boot injects
    // our JwtAuthenticationConverter bean (defined below) as a method parameter.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        // CSRF protection guards session-cookie apps against forged form posts. We are a
        // stateless token API (no cookies, no session), so it adds nothing and blocks POSTs.
        http.csrf(csrf -> csrf.disable());
        // STATELESS = never create or use an HttpSession. Every request must re-authenticate
        // from its Bearer token. This is what makes the API horizontally scalable.
        http.sessionManagement(sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        // Rules are evaluated top-to-bottom, first match wins. permitAll() opens the auth
        // endpoints + health + public status; anyRequest().authenticated() locks the rest,
        // so any new endpoint is secure-by-default (requires a valid token unless listed here).
        http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
                .requestMatchers("/api/auth/register", "/api/auth/login", "/actuator/health", "/api/public/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/services/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/services").hasRole("ADMIN")
                .anyRequest().authenticated());
        // Disable the built-in login mechanisms we don't use. Without this, Spring would pop
        // a browser Basic-auth dialog / redirect to a login form on 401 instead of returning JSON.
        http.httpBasic(httpBasicAuth -> httpBasicAuth.disable());
        http.formLogin(formLogin -> formLogin.disable());
        http.logout(logout -> logout.disable());
        // Turn this app into an OAuth2 Resource Server: it validates incoming Bearer JWTs
        // (signature + expiry + issuer, via the JwtDecoder bean) and, on success, builds the
        // Authentication using our converter to translate claims -> authorities.
        http.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    // Translates the JWT into Spring Security authorities. By default Spring reads the "scope"
    // claim and prefixes "SCOPE_"; our tokens carry roles in a "roles" claim, and hasRole(...)
    // / @PreAuthorize expect the "ROLE_" prefix. So: roles=["ADMIN"] -> authority "ROLE_ADMIN".
    // Get this wrong and authentication still succeeds but every authorization check silently 403s.
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }





}
