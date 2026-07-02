package com.mateja.pulseops.auth.domain;


import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;


import java.time.Instant;
import java.util.UUID;

// @Entity maps this class to a table; "app_users" because "user" is a reserved word in Postgres.
// Flyway owns the schema (see V1 migration); Hibernate is set to ddl-auto=validate, so these
// mappings are CHECKED against the real table at startup but never modify it.
@Entity
@Table(name = "app_users")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // app generates a random UUID PK (no DB sequence)
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "email" , nullable = false, length = 255)
    private String email;
    // We store the BCrypt HASH, never the raw password. length 255 comfortably fits a bcrypt string.
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    // EnumType.STRING persists "ADMIN"/"RESPONDER" as text (readable, reorder-safe). ORDINAL would
    // store 0/1 and silently corrupt if the enum order ever changed — avoid it.
    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Role role;
    // @CreationTimestamp: Hibernate sets this once on insert. updatable=false means UPDATEs never touch it.
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    // Public constructor for the fields WE supply. id + createdAt are omitted — they are generated.
    public UserAccount( String email, String passwordHash, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // JPA requires a no-arg constructor to instantiate entities when reading rows. protected (not
    // public) hides it from application code so nobody creates an invalid, empty UserAccount.
    protected UserAccount() {}

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
