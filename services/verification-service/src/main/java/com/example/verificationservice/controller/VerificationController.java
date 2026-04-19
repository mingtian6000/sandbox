package com.example.verificationservice.controller;

import com.example.verificationservice.dto.VerificationRequest;
import com.example.verificationservice.dto.VerificationResponse;
import com.example.verificationservice.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/verification")
public class VerificationController {
    @Autowired
    private VerificationService verificationService;

    @PostMapping("/generate")
    public ResponseEntity<VerificationResponse> generateCode(@Valid @RequestBody VerificationRequest request) {
        VerificationResponse response = verificationService.generateVerificationCode(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<VerificationResponse> verifyCode(
            @RequestParam String email,
            @RequestParam String code,
            @RequestParam String verificationType) {
        
        VerificationResponse response = verificationService.verifyCode(email, code, verificationType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Verification service is healthy");
    }
}