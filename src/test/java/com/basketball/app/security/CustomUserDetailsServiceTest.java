package com.basketball.app.security;

import com.basketball.app.model.User;
import com.basketball.app.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void loadUserByUsername_ActiveUser_ReturnsUserDetails() {
        User user = new User();
        user.setEmail("player@example.com");
        user.setPassword("encoded");
        user.setRole(User.Role.PLAYER);
        user.setIsActive(true);
        when(userRepository.findByEmailAndDeletedFalse("player@example.com")).thenReturn(Optional.of(user));

        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);
        UserDetails details = service.loadUserByUsername("player@example.com");

        assertEquals("player@example.com", details.getUsername());
        assertEquals("encoded", details.getPassword());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PLAYER")));
    }

    @Test
    void loadUserByUsername_InactiveOrMissing_Throws() {
        User inactive = new User();
        inactive.setEmail("inactive@example.com");
        inactive.setPassword("x");
        inactive.setRole(User.Role.PLAYER);
        inactive.setIsActive(false);

        when(userRepository.findByEmailAndDeletedFalse("inactive@example.com")).thenReturn(Optional.of(inactive));
        when(userRepository.findByEmailAndDeletedFalse("missing@example.com")).thenReturn(Optional.empty());

        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("inactive@example.com"));
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing@example.com"));
    }
}
