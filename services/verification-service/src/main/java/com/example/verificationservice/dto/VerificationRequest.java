package com.example.verificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class VerificationRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String verificationType; // "REGISTRATION", "PASSWORD_RESET", "LOGIN"

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVerificationType() {
        return verificationType;
    }

    public void setVerificationType(String verificationType) {
        this.verificationType = verificationType;
    }
}