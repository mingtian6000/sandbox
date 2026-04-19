package com.example.verificationservice.repository;

import com.example.verificationservice.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findByEmailAndCodeAndVerificationTypeAndUsedFalse(String email, String code, String verificationType);
    Optional<VerificationCode> findTopByEmailAndVerificationTypeAndUsedFalseOrderByCreatedAtDesc(String email, String verificationType);
    boolean existsByEmailAndVerificationTypeAndUsedFalse(String email, String verificationType);
}