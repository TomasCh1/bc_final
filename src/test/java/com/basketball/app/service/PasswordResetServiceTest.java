package com.basketball.app.service;

import com.basketball.app.model.User;
import com.basketball.app.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void requestPasswordReset_ActiveUser_SetsTokenHashAndExpiry() {
        ReflectionTestUtils.setField(passwordResetService, "emailDisabled", true);
        ReflectionTestUtils.setField(passwordResetService, "tokenValidityMinutes", 30);
        ReflectionTestUtils.setField(passwordResetService, "frontendBaseUrl", "http://localhost:3000");

        User user = new User();
        user.setId(1L);
        user.setEmail("player@example.com");
        user.setIsActive(true);
        user.setDeleted(false);
        when(userRepository.findByEmailTrimmedIgnoreCaseAndDeletedFalse("player@example.com")).thenReturn(Optional.of(user));

        passwordResetService.requestPasswordReset("player@example.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertNotNull(saved.getPasswordResetTokenHash());
        assertEquals(64, saved.getPasswordResetTokenHash().length());
        assertNotNull(saved.getPasswordResetExpiresAt());
        assertTrue(saved.getPasswordResetExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void resetPassword_ValidToken_UpdatesPasswordAndClearsResetFields() {
        when(passwordEncoder.encode("newPassword")).thenReturn("encoded");
        User user = new User();
        user.setIsActive(true);
        user.setDeleted(false);
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(15));
        when(userRepository.findByPasswordResetTokenHash(anyString())).thenReturn(Optional.of(user));

        passwordResetService.resetPassword("raw-token", "newPassword", "newPassword");

        assertEquals("encoded", user.getPassword());
        assertNull(user.getPasswordResetTokenHash());
        assertNull(user.getPasswordResetExpiresAt());
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_MismatchedPasswords_Throws() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordResetService.resetPassword("token", "a", "b"));
        assertTrue(ex.getMessage().contains("nezhodujú"));
    }
}
