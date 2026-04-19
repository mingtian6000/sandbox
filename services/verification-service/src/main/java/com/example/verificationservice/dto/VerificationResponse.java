package com.example.verificationservice.dto;

public class VerificationResponse {
    private boolean success;
    private String message;
    private String email;
    private String verificationType;

    public VerificationResponse(boolean success, String message, String email, String verificationType) {
        this.success = success;
        this.message = message;
        this.email = email;
        this.verificationType = verificationType;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

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