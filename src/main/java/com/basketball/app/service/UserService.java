package com.basketball.app.service;

import com.basketball.app.dto.PasswordChangeRequest;
import com.basketball.app.dto.UserCreateRequest;
import com.basketball.app.dto.UserUpdateRequest;
import com.basketball.app.model.User;
import com.basketball.app.repository.UserRepository;
import com.basketball.app.util.PasswordGenerator;
import com.basketball.app.util.UserNames;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.basketball.app.model.Category;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryService categoryService;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@localhost}")
    private String mailFrom;

    public UserService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder,
                      CategoryService categoryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoryService = categoryService;
    }

    /**
     * Create a new user with auto-generated password (if not provided)
     * Returns the generated password in the response (for display/email)
     */
    @Transactional
    public Map<String, Object> createUser(UserCreateRequest request) {
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new RuntimeException("User with email " + request.getEmail() + " already exists");
        }

        Set<Category> categories = new HashSet<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            for (Long categoryId : request.getCategoryIds()) {
                Category category = categoryService.findById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));
                categories.add(category);
            }
        } else if (request.getCategoryId() != null) {
            Category category = categoryService.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + request.getCategoryId()));
            categories.add(category);
        }

        String generatedPassword = request.getPassword();
        if (generatedPassword == null || generatedPassword.isBlank()) {
            generatedPassword = PasswordGenerator.generate();
        }

        User user = new User();
        user.setName(UserNames.normalize(request.getName()));
        user.setSurname(UserNames.normalize(request.getSurname()));
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setCategoryId(request.getCategoryId());
        user.setCategories(categories);
        user.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        user.setMustChangePassword(request.getMustChangePassword() != null ? request.getMustChangePassword() : true);
        user.setDateOfBirth(request.getDateOfBirth());
        user.setPassword(passwordEncoder.encode(generatedPassword));

        User savedUser = userRepository.save(user);
        boolean credentialsEmailSent = sendCredentialsEmail(savedUser, generatedPassword);

        Map<String, Object> response = new HashMap<>();
        response.put("user", toUserMap(savedUser));
        response.put("generatedPassword", generatedPassword);
        response.put("message", "User created successfully. Password: " + generatedPassword);
        response.put("credentialsEmailSent", credentialsEmailSent);

        return response;
    }

    private boolean sendCredentialsEmail(User user, String plainPassword) {
        if (mailSender == null) {
            log.warn("Cannot send credentials email to {}: JavaMailSender is not configured", user.getEmail());
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailFrom);
            helper.setTo(user.getEmail());
            helper.setSubject("Prihlasovacie udaje - MBK Slavia Trnava");
            helper.setText(
                    "Dobry den,\n\n"
                            + "vas ucet bol vytvoreny administratorom.\n\n"
                            + "Prihlasovacie udaje:\n"
                            + "Email: " + user.getEmail() + "\n"
                            + "Heslo: " + plainPassword + "\n\n"
                            + "Po prvom prihlaseni si prosim zmente heslo.\n\n"
                            + "MBK Slavia Trnava",
                    "<p>Dobry den,</p>"
                            + "<p>vas ucet bol vytvoreny administratorom.</p>"
                            + "<p><strong>Prihlasovacie udaje:</strong><br/>"
                            + "Email: " + user.getEmail() + "<br/>"
                            + "Heslo: " + plainPassword + "</p>"
                            + "<p>Po prvom prihlaseni si prosim zmente heslo.</p>"
                            + "<p>MBK Slavia Trnava</p>"
            );
            mailSender.send(message);
            return true;
        } catch (MessagingException | RuntimeException e) {
            log.error("Failed to send credentials email to {}", user.getEmail(), e);
            return false;
        }
    }

    /**
     * Update user information
     */
    @Transactional
    public User updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new RuntimeException("User not found: " + id);
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
                throw new RuntimeException("User with email " + request.getEmail() + " already exists");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getCategoryIds() != null) {
            Set<Category> categories = new HashSet<>();
            for (Long categoryId : request.getCategoryIds()) {
                Category category = categoryService.findById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));
                categories.add(category);
            }
            user.setCategories(categories);
            if (user.getRole() == User.Role.TRAINER || user.getRole() == User.Role.ADMIN) {
                java.util.Set<Long> newCategoryIds = new HashSet<>(request.getCategoryIds());
                List<Category> currentlyCoaching = categoryService.findByCoach(user.getId());
                for (Category cat : currentlyCoaching) {
                    if (!newCategoryIds.contains(cat.getId())) {
                        categoryService.removeCoachFromCategory(cat.getId(), user.getId());
                    }
                }
                for (Long categoryId : newCategoryIds) {
                    Category cat = categoryService.findById(categoryId).orElse(null);
                    if (cat != null && (cat.getCoaches() == null || !cat.getCoaches().stream().anyMatch(c -> c.getId().equals(user.getId())))) {
                        categoryService.assignCoach(categoryId, user.getId());
                    }
                }
            }
        } else if (request.getCategoryId() != null) {
            Category category = categoryService.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + request.getCategoryId()));
            user.setCategoryId(request.getCategoryId());
            Set<Category> categories = user.getCategories();
            if (categories == null) {
                categories = new HashSet<>();
                user.setCategories(categories);
            }
            categories.clear();
            categories.add(category);
        }

        if (request.getName() != null) {
            user.setName(UserNames.normalize(request.getName()));
        }
        if (request.getSurname() != null) {
            user.setSurname(UserNames.normalize(request.getSurname()));
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        user.setDateOfBirth(request.getDateOfBirth());

        return userRepository.save(user);
    }

    /**
     * Get user by ID (with categories loaded). Returns empty if user is deleted (hidden from API).
     */
    public Optional<User> findById(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return userOpt;
        }
        User user = userOpt.get();
        if (Boolean.TRUE.equals(user.getDeleted())) {
            return Optional.empty();
        }
        try {
            user.getCategories().size();
        } catch (Exception e) {
            List<User> allUsers = findAll();
            return allUsers.stream()
                    .filter(u -> u.getId().equals(id))
                    .findFirst();
        }
        return userOpt;
    }

    /**
     * Get all users
     */
    public List<User> findAll() {
        return userRepository.findAllWithCategories();
    }

    /**
     * Search users by name or email
     */
    public List<User> searchUsers(String search) {
        if (search == null || search.trim().isEmpty()) {
            return findAll();
        }
        return userRepository.searchByNameOrEmail(search.trim());
    }

    /**
     * Filter users by role
     */
    public List<User> findByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    /**
     * Filter users by category (supports many-to-many: players with category in their categories,
     * or trainers/admins who coach that category)
     */
    public List<User> findByCategory(Long categoryId) {
        return userRepository.findByCategoryInCategoriesOrCoached(categoryId);
    }

    /**
     * Filter users by role and category (supports many-to-many relationship)
     */
    public List<User> findByRoleAndCategory(User.Role role, Long categoryId) {
        return userRepository.findByRoleAndCategoryInCategoriesOrCoached(role, categoryId);
    }

    /**
     * Compute age structure for all players in the club.
     * Returns a list of maps with label and count so the frontend
     * can directly render chart labels and values.
     */
    public List<Map<String, Object>> getAgeStructure() {
        List<User> players = findByRole(User.Role.PLAYER);
        java.time.LocalDate today = java.time.LocalDate.now();

        class AgeGroup {
            final String label;
            final int min;
            final int max;

            AgeGroup(String label, int min, int max) {
                this.label = label;
                this.min = min;
                this.max = max;
            }
        }

        List<AgeGroup> groups = List.of(
                new AgeGroup("U10 (6-10)", 6, 10),
                new AgeGroup("U12 (11-12)", 11, 12),
                new AgeGroup("U14 (13-14)", 13, 14),
                new AgeGroup("U16 (15-16)", 15, 16),
                new AgeGroup("U18 (17-18)", 17, 18),
                new AgeGroup("U22 (19-22)", 19, 22),
                new AgeGroup("Muži (23+)", 23, 99)
        );

        List<Map<String, Object>> result = new ArrayList<>();

        for (AgeGroup group : groups) {
            long count = players.stream()
                    .filter(u -> u.getDateOfBirth() != null)
                    .map(User::getDateOfBirth)
                    .map(dob -> java.time.Period.between(dob, today).getYears())
                    .filter(age -> age >= group.min && age <= group.max)
                    .count();

            Map<String, Object> map = new HashMap<>();
            map.put("label", group.label);
            map.put("count", count);
            result.add(map);
        }

        return result;
    }

    /**
     * Delete user: hide from lists and free email
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new RuntimeException("User not found: " + id);
        }
        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("Cannot delete administrator account");
        }
        user.setDeleted(true);
        user.setIsActive(false);
        user.setEmail("__deleted_" + id + "_" + System.currentTimeMillis() + "@deleted.local");
        userRepository.save(user);
    }

    /**
     * Hard delete user
     */
    @Transactional
    public void hardDeleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * Change user password (for first login or regular password change)
     */
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new RuntimeException("User not found: " + userId);
        }

        boolean requiresCurrentPassword = user.getMustChangePassword() == null || !user.getMustChangePassword();
        if (requiresCurrentPassword) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new RuntimeException("Current password is required");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("Current password is incorrect");
            }
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirmation do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    /**
     * Assign user to category
     */
    @Transactional
    public User assignToCategory(Long userId, Long categoryId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new RuntimeException("User not found: " + userId);
        }

        categoryService.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));

        user.setCategoryId(categoryId);
        return userRepository.save(user);
    }

    /**
     * Get current authenticated user
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    /**
     * Convert User entity to Map
     */
    public Map<String, Object> toUserMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getName());
        userMap.put("surname", user.getSurname());
        userMap.put("displayName", UserNames.getDisplayName(user));
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole().name());
        userMap.put("categoryId", user.getCategoryId());
        
        try {
            Set<Category> categories = user.getCategories();
            if (categories != null && !categories.isEmpty()) {
                List<Map<String, Object>> categoriesList = categories.stream()
                        .map(cat -> {
                            Map<String, Object> catMap = new HashMap<>();
                            catMap.put("id", cat.getId());
                            catMap.put("name", cat.getName());
                            catMap.put("season", cat.getSeason());
                            return catMap;
                        })
                        .collect(Collectors.toList());
                userMap.put("categoryIds", categories.stream()
                        .map(Category::getId)
                        .collect(Collectors.toList()));
                userMap.put("categories", categoriesList);
            } else {
                userMap.put("categoryIds", Collections.emptyList());
                userMap.put("categories", Collections.emptyList());
            }
        } catch (Exception e) {
            userMap.put("categoryIds", Collections.emptyList());
            userMap.put("categories", Collections.emptyList());
        }
        
        if (user.getRole() == User.Role.TRAINER || user.getRole() == User.Role.ADMIN) {
            try {
                List<Category> coachedCategories = categoryService.findByCoach(user.getId());
                List<String> coachedCategoryNames = coachedCategories.stream()
                        .map(Category::getName)
                        .collect(Collectors.toList());
                userMap.put("coachedCategories", coachedCategoryNames);
            } catch (Exception e) {
                userMap.put("coachedCategories", Collections.emptyList());
            }
        } else {
            userMap.put("coachedCategories", Collections.emptyList());
        }
        
        userMap.put("dateOfBirth", user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null);
        userMap.put("isActive", user.getIsActive());
        userMap.put("mustChangePassword", user.getMustChangePassword());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("updatedAt", user.getUpdatedAt());
        return userMap;
    }

    /**
     * Primary category name for sorting.
     */
    public String getPrimaryCategoryNameForSort(User user) {
        try {
            if (user.getCategories() != null && !user.getCategories().isEmpty()) {
                return user.getCategories().stream()
                        .map(Category::getName)
                        .filter(name -> name != null && !name.isEmpty())
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .findFirst()
                        .orElse("");
            }
            if (user.getRole() == User.Role.TRAINER || user.getRole() == User.Role.ADMIN) {
                List<Category> coached = categoryService.findByCoach(user.getId());
                if (coached != null && !coached.isEmpty()) {
                    return coached.stream()
                            .map(Category::getName)
                            .filter(name -> name != null && !name.isEmpty())
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .findFirst()
                            .orElse("");
                }
            }
            if (user.getCategoryId() != null) {
                return categoryService.findById(user.getCategoryId())
                        .map(Category::getName)
                        .orElse("");
            }
        } catch (Exception e) {
        }
        return "";
    }

    /**
     * Map for list views.
     */
    public Map<String, Object> toUserSummaryMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("surname", user.getSurname());
        map.put("displayName", UserNames.getDisplayName(user));
        map.put("email", user.getEmail());
        map.put("role", user.getRole().name());
        map.put("categoryId", user.getCategoryId());
        map.put("isActive", user.getIsActive());
        try {
            if (user.getCategories() != null && !user.getCategories().isEmpty()) {
                map.put("categoryIds", user.getCategories().stream()
                        .map(Category::getId)
                        .collect(Collectors.toList()));
            } else {
                map.put("categoryIds", Collections.emptyList());
            }
        } catch (Exception e) {
            map.put("categoryIds", Collections.emptyList());
        }
        if (user.getRole() == User.Role.TRAINER || user.getRole() == User.Role.ADMIN) {
            try {
                List<String> coachedCategoryNames = categoryService.findByCoach(user.getId()).stream()
                        .map(Category::getName)
                        .collect(Collectors.toList());
                map.put("coachedCategories", coachedCategoryNames);
            } catch (Exception e) {
                map.put("coachedCategories", Collections.emptyList());
            }
        } else {
            map.put("coachedCategories", Collections.emptyList());
        }
        return map;
    }
}

