package com.basketball.app.controller;

import com.basketball.app.model.User;
import com.basketball.app.repository.UserRepository;
import com.basketball.app.service.AuthService;
import com.basketball.app.util.UserNames;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Profile("!prod")
@RequestMapping("/api/test")
public class TestController {

    private final UserRepository userRepository;
    private final AuthService authService;

    public TestController(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    /**
     * Create a test user for development/testing
     * REMOVE THIS IN PRODUCTION!
     */
    @PostMapping("/create-test-user")
    public ResponseEntity<?> createTestUser(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.getOrDefault("password", "password123");
            String role = request.getOrDefault("role", "PLAYER");
            String name = request.getOrDefault("name", "Test");
            String surname = request.getOrDefault("surname", "User");
            if (request.containsKey("name") && !request.containsKey("surname") && name.contains(" ")) {
                String[] split = UserNames.splitFullName(name);
                name = split[0];
                surname = split[1];
            }

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }

            if (userRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User with this email already exists"));
            }

            User user = new User();
            user.setEmail(email);
            user.setName(UserNames.normalize(name));
            user.setSurname(UserNames.normalize(surname));
            user.setPassword(authService.encodePassword(password));
            user.setRole(User.Role.valueOf(role.toUpperCase()));
            user.setIsActive(true);
            user.setMustChangePassword(false);

            User saved = userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Test user created successfully",
                    "user", Map.of(
                            "id", saved.getId(),
                            "email", saved.getEmail(),
                            "name", saved.getName(),
                            "surname", saved.getSurname(),
                            "displayName", UserNames.getDisplayName(saved),
                            "role", saved.getRole().name()
                    ),
                    "password", password // Return password for testing
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

