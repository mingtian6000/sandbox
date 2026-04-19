package com.example.verificationservice.service;

import com.example.verificationservice.dto.VerificationRequest;
import com.example.verificationservice.dto.VerificationResponse;
import com.example.verificationservice.entity.VerificationCode;
import com.example.verificationservice.repository.VerificationCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class VerificationService {
    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${verification.otp.length:6}")
    private int otpLength;

    @Value("${verification.otp.expiration-minutes:10}")
    private int expirationMinutes;

    private static final String CHARACTERS = "0123456789";
    private static final SecureRandom random = new SecureRandom();

    public VerificationResponse generateVerificationCode(VerificationRequest request) {
        // Check if there's already a valid code for this email and type
        Optional<VerificationCode> existingCode = verificationCodeRepository
                .findTopByEmailAndVerificationTypeAndUsedFalseOrderByCreatedAtDesc(request.getEmail(), request.getVerificationType());
        
        if (existingCode.isPresent() && existingCode.get().isValid()) {
            return new VerificationResponse(false, 
                "A valid verification code already exists for this email. Please check your email or wait for it to expire.",
                request.getEmail(), request.getVerificationType());
        }

        // Generate new code
        String code = generateRandomCode();
        
        VerificationCode verificationCode = new VerificationCode(
            request.getEmail(), 
            code, 
            request.getVerificationType(),
            expirationMinutes
        );
        
        verificationCodeRepository.save(verificationCode);
        
        // Send email
        sendVerificationEmail(request.getEmail(), code, request.getVerificationType());
        
        return new VerificationResponse(true, 
            "Verification code sent successfully. Please check your email.",
            request.getEmail(), request.getVerificationType());
    }

    public VerificationResponse verifyCode(String email, String code, String verificationType) {
        Optional<VerificationCode> verificationCodeOpt = verificationCodeRepository
                .findByEmailAndCodeAndVerificationTypeAndUsedFalse(email, code, verificationType);
        
        if (verificationCodeOpt.isEmpty()) {
            return new VerificationResponse(false, 
                "Invalid verification code or code already used.",
                email, verificationType);
        }
        
        VerificationCode verificationCode = verificationCodeOpt.get();
        
        if (verificationCode.isExpired()) {
            return new VerificationResponse(false, 
                "Verification code has expired. Please request a new one.",
                email, verificationType);
        }
        
        verificationCode.markAsVerified();
        verificationCodeRepository.save(verificationCode);
        
        return new VerificationResponse(true, 
            "Verification successful.",
            email, verificationType);
    }

    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(otpLength);
        for (int i = 0; i < otpLength; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }

    private void sendVerificationEmail(String email, String code, String verificationType) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Verification Code - " + verificationType);
            message.setText("Your verification code is: " + code + "\n\n" +
                          "This code will expire in " + expirationMinutes + " minutes.\n" +
                          "If you didn't request this, please ignore this email.");
            
            mailSender.send(message);
        } catch (Exception e) {
            // Log error but don't fail the request
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
}