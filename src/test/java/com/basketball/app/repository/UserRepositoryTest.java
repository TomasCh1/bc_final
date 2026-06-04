package com.basketball.app.repository;

import com.basketball.app.model.User;
import com.basketball.app.util.UserNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User testAdmin;
    private User testTrainer;
    private User testPlayer;

    @BeforeEach
    void setUp() {
        testUser = createTestUser("John Doe", "john@test.com", "password123", User.Role.PLAYER);
        testAdmin = createTestUser("Admin User", "admin@test.com", "admin123", User.Role.ADMIN);
        testTrainer = createTestUser("Trainer User", "trainer@test.com", "trainer123", User.Role.TRAINER);
        testPlayer = createTestUser("Player User", "player@test.com", "player123", User.Role.PLAYER);
    }

    @Test
    void testSaveAndFindById() {
        User saved = entityManager.persistAndFlush(testUser);
        Optional<User> found = userRepository.findById(saved.getId());
        
        assertTrue(found.isPresent());
        assertEquals("John Doe", UserNames.getDisplayName(found.get()));
        assertEquals("john@test.com", found.get().getEmail());
        assertEquals(User.Role.PLAYER, found.get().getRole());
    }

    @Test
    void testFindByEmail() {
        entityManager.persistAndFlush(testUser);
        Optional<User> found = userRepository.findByEmail("john@test.com");
        
        assertTrue(found.isPresent());
        assertEquals("John Doe", UserNames.getDisplayName(found.get()));
    }

    @Test
    void testFindByEmail_NotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@test.com");
        assertFalse(found.isPresent());
    }

    @Test
    void testExistsByEmail() {
        entityManager.persistAndFlush(testUser);
        assertTrue(userRepository.existsByEmail("john@test.com"));
        assertFalse(userRepository.existsByEmail("nonexistent@test.com"));
    }

    @Test
    void testFindByRole() {
        entityManager.persistAndFlush(testAdmin);
        entityManager.persistAndFlush(testTrainer);
        entityManager.persistAndFlush(testPlayer);
        entityManager.persistAndFlush(testUser);
        entityManager.flush();

        List<User> players = userRepository.findByRole(User.Role.PLAYER);
        assertEquals(2, players.size());
        assertTrue(players.stream().allMatch(u -> u.getRole() == User.Role.PLAYER));

        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        assertEquals(1, admins.size());
        assertEquals("Admin User", UserNames.getDisplayName(admins.get(0)));
    }

    @Test
    void testFindByCategoryId() {
        testUser.setCategoryId(1L);
        testPlayer.setCategoryId(1L);
        entityManager.persistAndFlush(testUser);
        entityManager.persistAndFlush(testPlayer);
        entityManager.flush();

        List<User> users = userRepository.findByCategoryId(1L);
        assertEquals(2, users.size());
        assertTrue(users.stream().allMatch(u -> u.getCategoryId() != null && u.getCategoryId().equals(1L)));
    }

    @Test
    void testFindByIsActive() {
        testUser.setIsActive(true);
        testPlayer.setIsActive(false);
        entityManager.persistAndFlush(testUser);
        entityManager.persistAndFlush(testPlayer);
        entityManager.flush();

        List<User> activeUsers = userRepository.findByIsActive(true);
        assertTrue(activeUsers.size() >= 1);
        assertTrue(activeUsers.stream().allMatch(u -> Boolean.TRUE.equals(u.getIsActive())));

        List<User> inactiveUsers = userRepository.findByIsActive(false);
        assertTrue(inactiveUsers.size() >= 1);
        assertTrue(inactiveUsers.stream().allMatch(u -> Boolean.FALSE.equals(u.getIsActive())));
    }

    @Test
    void testSearchByNameOrEmail() {
        entityManager.persistAndFlush(testUser);
        entityManager.persistAndFlush(testAdmin);
        entityManager.flush();

        List<User> results = userRepository.searchByNameOrEmail("john");
        assertEquals(1, results.size());
        assertEquals("john@test.com", results.get(0).getEmail());

        List<User> emailResults = userRepository.searchByNameOrEmail("admin");
        assertEquals(1, emailResults.size());
        assertEquals("admin@test.com", emailResults.get(0).getEmail());
    }

    @Test
    void testFindByRoleAndCategoryId() {
        testUser.setCategoryId(1L);
        testPlayer.setCategoryId(1L);
        testAdmin.setCategoryId(2L);
        entityManager.persistAndFlush(testUser);
        entityManager.persistAndFlush(testPlayer);
        entityManager.persistAndFlush(testAdmin);
        entityManager.flush();

        List<User> playersInCategory1 = userRepository.findByRoleAndCategoryId(User.Role.PLAYER, 1L);
        assertEquals(2, playersInCategory1.size());
        assertTrue(playersInCategory1.stream().allMatch(u -> 
            u.getRole() == User.Role.PLAYER && u.getCategoryId().equals(1L)));
    }

    @Test
    void testFindAllWithCategories() {
        entityManager.persistAndFlush(testUser);
        entityManager.persistAndFlush(testAdmin);
        entityManager.flush();

        List<User> allUsers = userRepository.findAllWithCategories();
        assertTrue(allUsers.size() >= 2);
    }

    @Test
    void testUpdateUser() {
        User saved = entityManager.persistAndFlush(testUser);
        saved.setName("Updated Name");
        saved.setEmail("updated@test.com");
        User updated = userRepository.save(saved);
        
        assertEquals("Updated Name", updated.getName());
        assertEquals("updated@test.com", updated.getEmail());
    }

    @Test
    void testDeleteUser() {
        User saved = entityManager.persistAndFlush(testUser);
        Long id = saved.getId();
        
        userRepository.deleteById(id);
        entityManager.flush();
        
        Optional<User> found = userRepository.findById(id);
        assertFalse(found.isPresent());
    }

    @Test
    void testEmailUniqueness() {
        entityManager.persistAndFlush(testUser);
        entityManager.flush();
        
        User duplicate = createTestUser("Another User", "john@test.com", "password", User.Role.PLAYER);
        
        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(duplicate);
        });
    }

    @Test
    void testUserTimestamps() {
        User saved = entityManager.persistAndFlush(testUser);
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        
        LocalDateTime originalUpdated = saved.getUpdatedAt();
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        saved.setName("Updated");
        User updated = userRepository.save(saved);
        entityManager.flush();
        
        assertTrue(updated.getUpdatedAt().isAfter(originalUpdated));
    }

    @Test
    void testMustChangePassword() {
        testUser.setMustChangePassword(true);
        User saved = entityManager.persistAndFlush(testUser);
        
        assertTrue(saved.getMustChangePassword());
        
        saved.setMustChangePassword(false);
        User updated = userRepository.save(saved);
        assertFalse(updated.getMustChangePassword());
    }

    private User createTestUser(String fullName, String email, String password, User.Role role) {
        User user = new User();
        UserNames.applyFullName(user, fullName);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        user.setIsActive(true);
        user.setMustChangePassword(false);
        return user;
    }
}
