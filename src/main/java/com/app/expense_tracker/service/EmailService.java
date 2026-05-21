package com.app.expense_tracker.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final Resend resend;

    // Injects your API key from application.properties
    public EmailService(@Value("${app.email.resend.api-key}") String apiKey) {
        this.resend = new Resend(apiKey);
    }

    public void sendSimpleMessage(String to, String subject, String text) {

        // Convert plain text newlines (\n) to HTML breaks (<br>) so it formats correctly
        String htmlFormattedText = "<p>" + text.replace("\n", "<br>") + "</p>";

        CreateEmailOptions params = CreateEmailOptions.builder()
                // MUST use onboarding@resend.dev until you verify a custom domain on their dashboard
                .from("Expense Tracker <onboarding@resend.dev>")
                .to(to)
                .subject(subject)
                .html(htmlFormattedText)
                .build();

        try {
            CreateEmailResponse data = resend.emails().send(params);
            System.out.println("✅ HTTP Email sent successfully! ID: " + data.getId());
        } catch (ResendException e) {
            System.err.println("❌ Failed to send HTTP email via Resend: " + e.getMessage());
        }
    }
}
