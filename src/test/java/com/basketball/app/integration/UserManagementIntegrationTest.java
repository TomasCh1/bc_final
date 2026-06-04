package com.basketball.app.integration;

import com.basketball.app.dto.CategoryCreateRequest;
import com.basketball.app.dto.UserCreateRequest;
import com.basketball.app.model.Category;
import com.basketball.app.model.User;
import com.basketball.app.repository.CategoryRepository;
import com.basketball.app.repository.UserRepository;
import com.basketball.app.service.CategoryService;
import com.basketball.app.service.UserService;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for complete user management workflow:
 * 1. Create category
 * 2. Create admin user
 * 3. Create trainer user
 * 4. Create player users
 * 5. Assign players to category
 * 6. Verify relationships
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("integration")
class UserManagementIntegrationTest {

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL =
            new GreenMailExtension(ServerSetupTest.SMTP)
                    .withPerMethodLifecycle(false);

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        categoryRepository.deleteAll();
        GREEN_MAIL.purgeEmailFromAllMailboxes();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCompleteUserManagementWorkflow() {
        CategoryCreateRequest categoryRequest = new CategoryCreateRequest();
        categoryRequest.setName("U13");
        categoryRequest.setSeason("2024-2025");
        
        Category category = categoryService.createCategory(categoryRequest);
        assertNotNull(category);
        assertEquals("U13", category.getName());
        assertNotNull(category.getId());

        UserCreateRequest adminRequest = new UserCreateRequest();
        adminRequest.setName("Admin User");
        adminRequest.setEmail("admin@test.com");
        adminRequest.setRole(User.Role.ADMIN);
        adminRequest.setCategoryIds(List.of(category.getId()));

        Map<String, Object> adminResult = userService.createUser(adminRequest);
        assertNotNull(adminResult);
        assertNotNull(adminResult.get("user"));
        assertNotNull(adminResult.get("generatedPassword"));
        assertTrue((Boolean) adminResult.get("credentialsEmailSent"));

        Map<?, ?> adminUserMap = (Map<?, ?>) adminResult.get("user");
        Long adminId = ((Number) adminUserMap.get("id")).longValue();
        assertNotNull(adminId);

        UserCreateRequest trainerRequest = new UserCreateRequest();
        trainerRequest.setName("Trainer User");
        trainerRequest.setEmail("trainer@test.com");
        trainerRequest.setRole(User.Role.TRAINER);
        trainerRequest.setCategoryIds(List.of(category.getId()));

        Map<String, Object> trainerResult = userService.createUser(trainerRequest);
        assertNotNull(trainerResult);
        assertNotNull(trainerResult.get("user"));
        assertTrue((Boolean) trainerResult.get("credentialsEmailSent"));

        UserCreateRequest player1Request = new UserCreateRequest();
        player1Request.setName("Player 1");
        player1Request.setEmail("player1@test.com");
        player1Request.setRole(User.Role.PLAYER);
        player1Request.setCategoryIds(List.of(category.getId()));

        Map<String, Object> player1Result = userService.createUser(player1Request);
        assertNotNull(player1Result);
        assertTrue((Boolean) player1Result.get("credentialsEmailSent"));

        UserCreateRequest player2Request = new UserCreateRequest();
        player2Request.setName("Player 2");
        player2Request.setEmail("player2@test.com");
        player2Request.setRole(User.Role.PLAYER);
        player2Request.setCategoryIds(List.of(category.getId()));

        Map<String, Object> player2Result = userService.createUser(player2Request);
        assertNotNull(player2Result);
        assertTrue((Boolean) player2Result.get("credentialsEmailSent"));

        List<User> allUsers = userService.findAll();
        assertTrue(allUsers.size() >= 4);

        List<User> players = userService.findByRole(User.Role.PLAYER);
        assertTrue(players.size() >= 2);

        List<User> trainers = userService.findByRole(User.Role.TRAINER);
        assertTrue(trainers.size() >= 1);

        List<User> admins = userService.findByRole(User.Role.ADMIN);
        assertTrue(admins.size() >= 1);

        List<User> categoryUsers = userService.findByCategory(category.getId());
        assertTrue(categoryUsers.size() >= 4);

        List<User> searchResults = userService.searchUsers("Player");
        assertTrue(searchResults.size() >= 2);

        assertEquals(4, GREEN_MAIL.getReceivedMessages().length);
        assertCredentialsMailSentTo("admin@test.com");
        assertCredentialsMailSentTo("trainer@test.com");
        assertCredentialsMailSentTo("player1@test.com");
        assertCredentialsMailSentTo("player2@test.com");
    }

    @Test
    void testUserPasswordManagement() {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("Test User");
        request.setEmail("test@test.com");
        request.setRole(User.Role.PLAYER);

        Map<String, Object> result = userService.createUser(request);
        assertNotNull(result);
        assertNotNull(result.get("generatedPassword"));
        assertTrue((Boolean) result.get("credentialsEmailSent"));

        String generatedPassword = (String) result.get("generatedPassword");
        assertNotNull(generatedPassword);
        assertFalse(generatedPassword.isEmpty());

        Map<?, ?> userMap = (Map<?, ?>) result.get("user");
        Long userId = ((Number) userMap.get("id")).longValue();

        Optional<User> foundUser = userService.findById(userId);
        assertTrue(foundUser.isPresent());
        assertTrue(foundUser.get().getMustChangePassword());

        assertEquals(1, GREEN_MAIL.getReceivedMessages().length);
        assertCredentialsMailSentTo("test@test.com");
    }

    @Test
    void testCategoryWithMultipleCoaches() {
        CategoryCreateRequest categoryRequest = new CategoryCreateRequest();
        categoryRequest.setName("U15");
        categoryRequest.setSeason("2024-2025");
        Category category = categoryService.createCategory(categoryRequest);

        UserCreateRequest trainer1Request = new UserCreateRequest();
        trainer1Request.setName("Trainer 1");
        trainer1Request.setEmail("trainer1@test.com");
        trainer1Request.setRole(User.Role.TRAINER);
        Map<String, Object> trainer1Result = userService.createUser(trainer1Request);
        assertTrue((Boolean) trainer1Result.get("credentialsEmailSent"));

        UserCreateRequest trainer2Request = new UserCreateRequest();
        trainer2Request.setName("Trainer 2");
        trainer2Request.setEmail("trainer2@test.com");
        trainer2Request.setRole(User.Role.TRAINER);
        Map<String, Object> trainer2Result = userService.createUser(trainer2Request);
        assertTrue((Boolean) trainer2Result.get("credentialsEmailSent"));

        List<User> trainers = userService.findByRole(User.Role.TRAINER);
        assertTrue(trainers.size() >= 2);

        com.basketball.app.dto.CategoryUpdateRequest updateRequest = 
            new com.basketball.app.dto.CategoryUpdateRequest();
        List<Long> coachIds = new ArrayList<>();
        trainers.forEach(t -> coachIds.add(t.getId()));
        updateRequest.setCoachIds(coachIds);

        Category updatedCategory = categoryService.updateCategory(category.getId(), updateRequest);
        assertNotNull(updatedCategory);

        assertEquals(2, GREEN_MAIL.getReceivedMessages().length);
        assertCredentialsMailSentTo("trainer1@test.com");
        assertCredentialsMailSentTo("trainer2@test.com");
    }

    private void assertCredentialsMailSentTo(String expectedRecipient) {
        MimeMessage[] receivedMessages = GREEN_MAIL.getReceivedMessages();
        boolean hasRecipient = Arrays.stream(receivedMessages).anyMatch(message -> {
            try {
                return Arrays.stream(message.getAllRecipients())
                        .anyMatch(address -> expectedRecipient.equalsIgnoreCase(address.toString()));
            } catch (Exception e) {
                return false;
            }
        });
        assertTrue(hasRecipient, "Expected credentials email for " + expectedRecipient);
    }
}
