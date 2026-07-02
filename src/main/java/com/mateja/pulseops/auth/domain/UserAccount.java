package com.mateja.pulseops.auth.domain;


import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;


import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "email" , nullable = false, length = 255)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Role role;
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    public UserAccount( String email, String passwordHash, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

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
