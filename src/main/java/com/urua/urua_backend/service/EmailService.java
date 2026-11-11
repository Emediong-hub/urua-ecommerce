package com.urua.urua_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:}") String fromAddress,
                        @Value("${app.mail.from-name:Uru\u00e0}") String fromName) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    public void sendVerificationEmail(String to, String name, String link) {
        // example.com is reserved for demos and tests, so nothing can be delivered there
        if (to.toLowerCase().endsWith("@example.com")) {
            log.info("Skipping verification email to the reserved demo address {}", to);
            return;
        }

        String safeName = HtmlUtils.htmlEscape(name);
        String safeLink = HtmlUtils.htmlEscape(link);

        String text = "Hi " + name + ",\n\n"
                + "Welcome to Uru\u00e0! Please confirm your email address by opening this link:\n\n"
                + link + "\n\n"
                + "The link expires in 24 hours. If you didn't create an account, you can ignore this email.\n\n"
                + "The Uru\u00e0 team";

        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;color:#1c2b24">
                  <h2 style="color:#0b3d2a">Welcome to Uru\u00e0, %s!</h2>
                  <p>Please confirm your email address to activate your account.</p>
                  <p>
                    <a href="%s" style="display:inline-block;background:#0b3d2a;color:#ffffff;padding:12px 22px;border-radius:8px;text-decoration:none;font-weight:bold">Verify my email</a>
                  </p>
                  <p style="font-size:13px;color:#5d6b64">This link expires in 24 hours. If the button doesn't work, copy this address into your browser:<br>%s</p>
                  <p style="font-size:13px;color:#5d6b64">If you didn't create an Uru\u00e0 account, you can ignore this email.</p>
                </div>
                """.formatted(safeName, safeLink, safeLink);

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject("Verify your Uru\u00e0 email");
            helper.setText(text, html);
            mailSender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Could not send email: " + e.getMessage(), e);
        }
    }
}