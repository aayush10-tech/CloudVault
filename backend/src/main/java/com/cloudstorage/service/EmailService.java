package com.cloudstorage.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendShareEmail(
            String recipientEmail,
            String ownerEmail,
            String fileName) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(recipientEmail);
        message.setSubject("A file has been shared with you - CloudVault");

        message.setText(
                "Hello,\n\n" +
                "A file has been shared with you through CloudVault.\n\n" +
                "File: " + fileName + "\n" +
                "Shared by: " + ownerEmail + "\n\n" +
                "Please log in to your CloudVault account to access the file.\n\n" +
                "Regards,\n" +
                "CloudVault"
        );

        mailSender.send(message);
    }
}