package com.app.expense_tracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender emailSender;

    public void sendSimpleMessage(String to, String subject, String text) {
        if (emailSender == null) {
            System.out.println("📬 SIMULATED EMAIL DISPATCH (Configure application.properties for live SMTP routing):");
            System.out.println("To: " + to + " | Subject: " + subject + "\nText: " + text);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("your-app-email@gmail.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }
}