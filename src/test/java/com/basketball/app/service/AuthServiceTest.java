package com.basketball.app.service;

import com.basketball.app.model.User;
import com.basketball.app.repository.UserRepository;
import com.basketball.app.security.JwtTokenProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    @Test
    void authenticate_ReturnsBearerTokenAndUserMap() {
        Authentication auth = new UsernamePasswordAuthenticationToken("x", "y");
        User user = new User();
        user.setEmail("admin@example.com");
        user.setRole(User.Role.ADMIN);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByEmailAndDeletedFalseWithCategories("admin@example.com")).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken("admin@example.com", "ADMIN")).thenReturn("jwt-token");
        when(userService.toUserMap(user)).thenReturn(Map.of("email", user.getEmail()));

        Map<String, Object> result = authService.authenticate("admin@example.com", "secret");

        assertEquals("jwt-token", result.get("token"));
        assertEquals("Bearer", result.get("type"));
        assertEquals("admin@example.com", ((Map<?, ?>) result.get("user")).get("email"));
    }

    @Test
    void passwordHelpers_DelegateToEncoder() {
        when(passwordEncoder.encode("raw")).thenReturn("encoded");
        when(passwordEncoder.matches("raw", "encoded")).thenReturn(true);

        assertEquals("encoded", authService.encodePassword("raw"));
        assertTrue(authService.verifyPassword("raw", "encoded"));
    }
}
