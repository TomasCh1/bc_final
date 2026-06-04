package com.basketball.app.service;

import com.basketball.app.model.User;
import com.basketball.app.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.frontend.base-url:http://localhost:8080}")
    private String frontendBaseUrl;

    @Value("${app.mail.from:noreply@localhost}")
    private String mailFrom;

    @Value("${app.password-reset.email-disabled:true}")
    private boolean emailDisabled;

    @Value("${app.password-reset.token-validity-minutes:60}")
    private int tokenValidityMinutes;

    public PasswordResetService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * If a matching active account exists, issues a reset token and sends email (or logs the link when email is disabled).
     */
    @Transactional
    public void requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        Optional<User> opt = userRepository.findByEmailTrimmedIgnoreCaseAndDeletedFalse(email);
        if (opt.isEmpty()) {
            return;
        }
        User user = opt.get();
        if (Boolean.FALSE.equals(user.getIsActive())) {
            return;
        }

        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        String hash = sha256Hex(rawToken);

        user.setPasswordResetTokenHash(hash);
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(tokenValidityMinutes));
        userRepository.save(user);

        String resetUrl = buildResetUrl(rawToken);
        if (emailDisabled || mailSender == null) {
            log.info("Password reset link for {} (email disabled or no mail sender): {}", user.getEmail(), resetUrl);
            return;
        }
        try {
            sendResetEmail(user.getEmail(), resetUrl);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", user.getEmail(), e);
        }
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword, String confirmPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new RuntimeException("Neplatný alebo expirovaný odkaz na obnovenie hesla.");
        }
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Nové heslo a potvrdenie sa nezhodujú.");
        }

        String hash = sha256Hex(rawToken.trim());
        User user = userRepository.findByPasswordResetTokenHash(hash)
                .orElseThrow(() -> new RuntimeException("Neplatný alebo expirovaný odkaz na obnovenie hesla."));

        if (Boolean.TRUE.equals(user.getDeleted()) || Boolean.FALSE.equals(user.getIsActive())) {
            throw new RuntimeException("Neplatný alebo expirovaný odkaz na obnovenie hesla.");
        }
        if (user.getPasswordResetExpiresAt() == null
                || user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Neplatný alebo expirovaný odkaz na obnovenie hesla.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    private String buildResetUrl(String rawToken) {
        String base = frontendBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/pages/reset-password.html?token=" + java.net.URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private void sendResetEmail(String to, String resetUrl) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(mailFrom);
        helper.setTo(to);
        helper.setSubject("Obnovenie hesla – MBK Slávia Trnava");
        helper.setText(
                "Dobrý deň,\n\n"
                        + "Požiadali ste o obnovenie hesla. Otvorte nasledujúci odkaz v prehliadači (platnosť je obmedzená):\n\n"
                        + resetUrl
                        + "\n\n"
                        + "Ak ste o obnovenie nežiadali, tento e-mail ignorujte.\n\n"
                        + "MBK Slávia Trnava",
                "<p>Dobrý deň,</p>"
                        + "<p>Požiadali ste o obnovenie hesla. Kliknite na odkaz nižšie (platnosť je obmedzená):</p>"
                        + "<p><a href=\"" + resetUrl + "\">Obnoviť heslo</a></p>"
                        + "<p>Ak ste o obnovenie nežiadali, tento e-mail ignorujte.</p>"
                        + "<p>MBK Slávia Trnava</p>"
        );
        mailSender.send(message);
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
