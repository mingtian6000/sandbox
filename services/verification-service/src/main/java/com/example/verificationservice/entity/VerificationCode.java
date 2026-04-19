package com.example.verificationservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_codes")
public class VerificationCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "verification_type", nullable = false)
    private String verificationType; // "REGISTRATION", "PASSWORD_RESET", "LOGIN"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "is_used", nullable = false)
    private boolean used = false;

    // Constructors
    public VerificationCode() {
        this.createdAt = LocalDateTime.now();
    }

    public VerificationCode(String email, String code, String verificationType, int expirationMinutes) {
        this();
        this.email = email;
        this.code = code;
        this.verificationType = verificationType;
        this.expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getVerificationType() {
        return verificationType;
    }

    public void setVerificationType(String verificationType) {
        this.verificationType = verificationType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    // Helper methods
    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void markAsVerified() {
        this.used = true;
        this.verifiedAt = LocalDateTime.now();
    }
}