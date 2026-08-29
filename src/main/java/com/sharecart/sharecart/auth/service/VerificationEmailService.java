package com.sharecart.sharecart.auth.service;

public interface VerificationEmailService {

    void sendVerificationEmail(String toEmail, String recipientName, String verificationLink);
}
