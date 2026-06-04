package com.basketball.app.controller;

import com.basketball.app.model.User;
import com.basketball.app.repository.UserRepository;
import com.basketball.app.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TestController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Tag("api")
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private AuthService authService;
    @MockBean
    private com.basketball.app.security.JwtTokenProvider jwtTokenProvider;
    @MockBean
    private com.basketball.app.security.CustomUserDetailsService userDetailsService;

    @Test
    void createTestUser_MissingEmail_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/test/create-test-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email is required"));
    }

    @Test
    void createTestUser_Success_ReturnsCreatedUserDetails() throws Exception {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(authService.encodePassword("abc123")).thenReturn("encoded");

        User saved = new User();
        saved.setId(5L);
        saved.setEmail("test@example.com");
        saved.setName("Tester");
        saved.setRole(User.Role.PLAYER);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        mockMvc.perform(post("/api/test/create-test-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "test@example.com",
                                "password", "abc123",
                                "name", "Tester",
                                "role", "PLAYER"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(5))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }
}
