package ${packageName}.security;

import ${packageName}.entity.${principalEntity};
import ${packageName}.repository.${principalEntity}Repository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling password reset requests.
 * Generates a unique token, stores it on the user entity, and sends reset emails.
 */
@Service
public class PasswordResetService {

    private final ${principalEntity}Repository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Value("${"$"}{app.password-reset.token-validity-minutes:60}")
    private int tokenValidityMinutes;

    @Value("${"$"}{app.password-reset.base-url:http://localhost:8080}")
    private String baseUrl;

    public PasswordResetService(${principalEntity}Repository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 MailService mailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    /**
     * Initiates a password reset by generating a token and sending an email.
     * Silently succeeds even if the email is not found (to prevent email enumeration).
     */
    @Transactional
    public void requestPasswordReset(String email) {
        Optional<${principalEntity}> userOpt = userRepository.findBy${usernameField?cap_first}(email);
        if (userOpt.isEmpty()) {
            // Silent return to prevent email enumeration attacks
            return;
        }

        ${principalEntity} user = userOpt.get();
        String token = UUID.randomUUID().toString();

        user.set${passwordResetTokenField?cap_first}(token);
        user.set${passwordResetExpiryField?cap_first}(LocalDateTime.now().plusMinutes(tokenValidityMinutes));
        userRepository.save(user);

        String resetUrl = baseUrl + "/api/auth/reset-password?token=" + token;
        mailService.sendPasswordResetEmail(email, resetUrl);
    }

    /**
     * Validates the token and resets the password.
     *
     * @return true if the password was successfully reset
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<${principalEntity}> userOpt = userRepository.findBy${passwordResetTokenField?cap_first}(token);
        if (userOpt.isEmpty()) {
            return false;
        }

        ${principalEntity} user = userOpt.get();

        // Check token expiry
        LocalDateTime expiry = user.get${passwordResetExpiryField?cap_first}();
        if (expiry == null || expiry.isBefore(LocalDateTime.now())) {
            return false;
        }

        // Update password and clear reset fields
        user.set${passwordField?cap_first}(passwordEncoder.encode(newPassword));
        user.set${passwordResetTokenField?cap_first}(null);
        user.set${passwordResetExpiryField?cap_first}(null);
        userRepository.save(user);

        return true;
    }
}
