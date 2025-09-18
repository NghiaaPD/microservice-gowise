package com.example.auth_service.service;

import com.example.auth_service.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(String toEmail, String username, String token) {
        try {
            // Tạo context cho template
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("verificationUrl", appProperties.getBaseUrl() + "/auth/verify?token=" + token);

            // Render HTML template
            String htmlContent = templateEngine.process("email/verification-email", context);

            // Tạo MimeMessage
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Email Address - GoWise");
            helper.setText(htmlContent, true); // true = HTML content

            // Embed logo image
            ClassPathResource logoResource = new ClassPathResource("asset/logo.png");
            helper.addInline("logo", logoResource);

            mailSender.send(mimeMessage);

            logger.info("Verification email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
            // KHÔNG throw exception nữa - để signup vẫn thành công
        }
    }

    public void sendPasswordResetEmail(String toEmail, String username, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set email properties
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset - Verification Code");

            // Create Thymeleaf context
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("otp", otp);

            // Process template
            String htmlContent = templateEngine.process("email/forgot-password", context);

            // Attach logo
            ClassPathResource logoResource = new ClassPathResource("asset/logo.png");
            if (logoResource.exists()) {
                helper.addInline("logo", logoResource);
            }

            // Set HTML content
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}