package com.basketball.app.controller;

import com.basketball.app.dto.PasswordChangeRequest;
import com.basketball.app.dto.UserCreateRequest;
import com.basketball.app.dto.UserUpdateRequest;
import com.basketball.app.model.User;
import com.basketball.app.service.UserService;
import com.basketball.app.util.UserNames;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Create a new user
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserCreateRequest request) {
        try {
            Map<String, Object> response = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all users.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "false") boolean summary,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        
        User currentUser = userService.getCurrentUser();
        boolean isAdmin = currentUser.getRole().equals(User.Role.ADMIN);
        
        List<User> users;
        
        if (search != null && !search.trim().isEmpty()) {
            users = userService.searchUsers(search);
        } else {
            users = userService.findAll();
        }
        
        if (role != null && !role.trim().isEmpty()) {
            users = users.stream()
                    .filter(u -> u.getRole().name().equalsIgnoreCase(role.trim()))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        if (categoryId != null) {
            List<User> categoryFilteredUsers = userService.findByCategory(categoryId);
            List<Long> currentUserIds = users.stream()
                    .map(User::getId)
                    .collect(java.util.stream.Collectors.toList());
            users = categoryFilteredUsers.stream()
                    .filter(u -> currentUserIds.contains(u.getId()))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        if (!isAdmin) {
            users = users.stream()
                    .filter(u -> !u.getRole().equals(User.Role.TRAINER) || u.getId().equals(currentUser.getId()))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        final List<User> sorted = users.stream()
                .sorted(java.util.Comparator
                        .comparing((User u) -> (userService.getPrimaryCategoryNameForSort(u) + "\0").toLowerCase())
                        .thenComparing(u -> UserNames.normalize(u.getSurname()).toLowerCase())
                        .thenComparing(u -> UserNames.normalize(u.getName()).toLowerCase()))
                .collect(java.util.stream.Collectors.toList());
        
        if (page != null && size != null && page >= 0 && size > 0) {
            int total = sorted.size();
            int from = Math.min(page * size, total);
            int to = Math.min(from + size, total);
            List<Map<String, Object>> content = sorted.subList(from, to).stream()
                    .map(u -> summary ? userService.toUserSummaryMap(u) : userService.toUserMap(u))
                    .collect(java.util.stream.Collectors.toList());
            int activePlayers = (int) sorted.stream().filter(u -> u.getRole() == User.Role.PLAYER && Boolean.TRUE.equals(u.getIsActive())).count();
            int trainers = (int) sorted.stream().filter(u -> u.getRole() == User.Role.TRAINER).count();
            int admins = (int) sorted.stream().filter(u -> u.getRole() == User.Role.ADMIN).count();
            int activeUsers = (int) sorted.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
            return ResponseEntity.ok(java.util.Map.of(
                    "content", content,
                    "totalElements", total,
                    "stats", java.util.Map.of(
                            "totalElements", total,
                            "activePlayers", activePlayers,
                            "trainers", trainers,
                            "admins", admins,
                            "activeUsers", activeUsers
                    )));
        }
        
        List<Map<String, Object>> result = sorted.stream()
                .map(u -> summary ? userService.toUserSummaryMap(u) : userService.toUserMap(u))
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get age structure for all players in the club.
     */
    @GetMapping("/age-structure")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<?> getAgeStructure() {
        try {
            java.util.List<java.util.Map<String, Object>> ageStructure = userService.getAgeStructure();
            return ResponseEntity.ok(ageStructure);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found: " + id));
            return ResponseEntity.ok(userService.toUserMap(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update user
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, 
                                        @Valid @RequestBody UserUpdateRequest request) {
        try {
            User updatedUser = userService.updateUser(id, request);
            return ResponseEntity.ok(userService.toUserMap(updatedUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete user
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Assign user to category
     */
    @PostMapping("/{id}/assign-category")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignToCategory(@PathVariable Long id, 
                                               @RequestBody Map<String, Long> request) {
        try {
            Long categoryId = request.get("categoryId");
            if (categoryId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "categoryId is required"));
            }
            User user = userService.assignToCategory(id, categoryId);
            return ResponseEntity.ok(userService.toUserMap(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Change password
     */
    @PostMapping("/{id}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable Long id,
                                           @Valid @RequestBody PasswordChangeRequest request) {
        try {
            User currentUser = userService.getCurrentUser();
            
            if (!userService.findById(id).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found: " + id));
            }
            
            if (!currentUser.getRole().equals(User.Role.ADMIN) && 
                !currentUser.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only change your own password"));
            }
            
            userService.changePassword(id, request);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get current user profile
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            User currentUser = userService.getCurrentUser();
            return ResponseEntity.ok(userService.toUserMap(currentUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Search users
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchUsers(@RequestParam String q) {
        List<User> users = userService.searchUsers(q);
        List<Map<String, Object>> userMaps = users.stream()
                .map(userService::toUserMap)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(userMaps);
    }
}

