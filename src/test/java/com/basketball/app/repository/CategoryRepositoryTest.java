package com.basketball.app.repository;

import com.basketball.app.model.Category;
import com.basketball.app.model.User;
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
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private Category testCategory;
    private Category testCategory2;
    private User testCoach;

    @BeforeEach
    void setUp() {
        testCoach = createTestUser("Coach User", "coach@test.com", "password", User.Role.TRAINER);
        entityManager.persistAndFlush(testCoach);

        testCategory = createTestCategory("U13", "2024-2025", testCoach.getId());
        testCategory2 = createTestCategory("U15", "2024-2025", null);
    }

    @Test
    void testSaveAndFindById() {
        Category saved = entityManager.persistAndFlush(testCategory);
        Optional<Category> found = categoryRepository.findById(saved.getId());
        
        assertTrue(found.isPresent());
        assertEquals("U13", found.get().getName());
        assertEquals("2024-2025", found.get().getSeason());
    }

    @Test
    void testFindByName() {
        entityManager.persistAndFlush(testCategory);
        Optional<Category> found = categoryRepository.findByName("U13");
        
        assertTrue(found.isPresent());
        assertEquals("U13", found.get().getName());
    }

    @Test
    void testFindByName_NotFound() {
        Optional<Category> found = categoryRepository.findByName("Nonexistent");
        assertFalse(found.isPresent());
    }

    @Test
    void testFindBySeason() {
        Category category3 = createTestCategory("U17", "2023-2024", null);
        entityManager.persistAndFlush(testCategory);
        entityManager.persistAndFlush(testCategory2);
        entityManager.persistAndFlush(category3);
        entityManager.flush();

        List<Category> season2024 = categoryRepository.findBySeason("2024-2025");
        assertEquals(2, season2024.size());
        assertTrue(season2024.stream().allMatch(c -> "2024-2025".equals(c.getSeason())));
    }

    @Test
    void testFindByCoachId() {
        entityManager.persistAndFlush(testCategory);
        entityManager.persistAndFlush(testCategory2);
        entityManager.flush();

        List<Category> coachedCategories = categoryRepository.findByCoachId(testCoach.getId());
        assertEquals(1, coachedCategories.size());
        assertEquals("U13", coachedCategories.get(0).getName());
    }

    @Test
    void testFindByCoachInCoaches() {
        testCategory.getCoaches().add(testCoach);
        entityManager.persistAndFlush(testCategory);
        entityManager.flush();

        List<Category> coachedCategories = categoryRepository.findByCoachInCoaches(testCoach.getId());
        assertEquals(1, coachedCategories.size());
        assertEquals("U13", coachedCategories.get(0).getName());
    }

    @Test
    void testFindAllWithCoaches() {
        testCategory.getCoaches().add(testCoach);
        entityManager.persistAndFlush(testCategory);
        entityManager.persistAndFlush(testCategory2);
        entityManager.flush();

        List<Category> allCategories = categoryRepository.findAllWithCoaches();
        assertTrue(allCategories.size() >= 2);
    }

    @Test
    void testSearchByNameOrSeason() {
        entityManager.persistAndFlush(testCategory);
        entityManager.persistAndFlush(testCategory2);
        entityManager.flush();

        List<Category> results = categoryRepository.searchByNameOrSeason("U13");
        assertEquals(1, results.size());
        assertEquals("U13", results.get(0).getName());

        List<Category> seasonResults = categoryRepository.searchByNameOrSeason("2024");
        assertEquals(2, seasonResults.size());
    }

    @Test
    void testUpdateCategory() {
        Category saved = entityManager.persistAndFlush(testCategory);
        saved.setName("U13 Updated");
        saved.setSeason("2025-2026");
        Category updated = categoryRepository.save(saved);
        
        assertEquals("U13 Updated", updated.getName());
        assertEquals("2025-2026", updated.getSeason());
    }

    @Test
    void testDeleteCategory() {
        Category saved = entityManager.persistAndFlush(testCategory);
        Long id = saved.getId();
        
        categoryRepository.deleteById(id);
        entityManager.flush();
        
        Optional<Category> found = categoryRepository.findById(id);
        assertFalse(found.isPresent());
    }

    @Test
    void testNameUniqueness() {
        entityManager.persistAndFlush(testCategory);
        entityManager.flush();
        
        Category duplicate = createTestCategory("U13", "2025-2026", null);
        
        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(duplicate);
        });
    }

    @Test
    void testCategoryTimestamps() {
        Category saved = entityManager.persistAndFlush(testCategory);
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        
        LocalDateTime originalUpdated = saved.getUpdatedAt();
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        saved.setName("Updated");
        Category updated = categoryRepository.save(saved);
        entityManager.flush();
        
        assertTrue(updated.getUpdatedAt().isAfter(originalUpdated));
    }

    @Test
    void testManyToManyCoachesRelationship() {
        User coach2 = createTestUser("Coach 2", "coach2@test.com", "password", User.Role.TRAINER);
        entityManager.persistAndFlush(coach2);

        testCategory.getCoaches().add(testCoach);
        testCategory.getCoaches().add(coach2);
        Category saved = entityManager.persistAndFlush(testCategory);
        entityManager.flush();

        Category found = categoryRepository.findById(saved.getId()).orElseThrow();
        assertEquals(2, found.getCoaches().size());
    }

    private Category createTestCategory(String name, String season, Long coachId) {
        Category category = new Category();
        category.setName(name);
        category.setSeason(season);
        category.setCoachId(coachId);
        return category;
    }

    private User createTestUser(String name, String email, String password, User.Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        user.setIsActive(true);
        return user;
    }
}
