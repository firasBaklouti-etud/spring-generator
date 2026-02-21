package ${packageName}.security;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails (password reset, notifications, etc.).
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    @Value("${"$"}{spring.mail.username:noreply@example.com}")
    private String fromEmail;

    @Value("${"$"}{spring.application.name:Application}")
    private String applicationName;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send a password reset email with an HTML template.
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        String subject = applicationName + " - Password Reset Request";
        String htmlContent = buildPasswordResetEmail(resetUrl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}", toEmail, e);
        }
    }

    private String buildPasswordResetEmail(String resetUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; padding: 40px; }
                        .header { text-align: center; margin-bottom: 30px; }
                        .header h1 { color: #333; font-size: 24px; }
                        .content { color: #555; line-height: 1.6; }
                        .button { display: inline-block; background-color: #4F46E5; color: white; text-decoration: none;
                                  padding: 12px 30px; border-radius: 6px; margin: 20px 0; font-weight: bold; }
                        .footer { margin-top: 30px; font-size: 12px; color: #999; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Password Reset</h1>
                        </div>
                        <div class="content">
                            <p>You have requested to reset your password. Click the button below to proceed:</p>
                            <p style="text-align: center;">
                                <a href="%s" class="button">Reset Password</a>
                            </p>
                            <p>This link will expire in 60 minutes.</p>
                            <p>If you did not request a password reset, please ignore this email.</p>
                        </div>
                        <div class="footer">
                            <p>This is an automated message. Please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(resetUrl);
    }
}
