package com.app.expense_tracker.security;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils; // 🌟 NEW: Standard Spring utility to decode HTML

@Component
public class SecuritySanitizer {

    // Strict text policy that completely strips out all HTML tags (<script>, <iframe>, etc.)
    private final PolicyFactory strictTextPolicy = new HtmlPolicyBuilder().toFactory();

    /**
     * Sanitizes raw text parameters to neutralize XSS payload injections and decodes HTML entities.
     * @param rawInput Raw input string from the client request
     * @return Fully clean, unescaped plain text string
     */
    public String sanitizeText(String rawInput) {
        if (rawInput == null) {
            return null;
        }

        // 1. Strip out dangerous scripts/HTML tags safely
        String cleanHtml = strictTextPolicy.sanitize(rawInput).trim();

        // 2. 🌟 FIXED: Convert "&amp;" back to "&", "&lt;" to "<", etc. natively
        return HtmlUtils.htmlUnescape(cleanHtml);
    }
}