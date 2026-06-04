package com.basketball.app.service;

import com.basketball.app.dto.CategoryCreateRequest;
import com.basketball.app.dto.CategoryUpdateRequest;
import com.basketball.app.model.Category;
import com.basketball.app.model.User;
import com.basketball.app.repository.AttendanceRepository;
import com.basketball.app.repository.CategoryRepository;
import com.basketball.app.repository.EventRepository;
import com.basketball.app.repository.StatisticsRepository;
import com.basketball.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private StatisticsRepository statisticsRepository;
    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category testCategory;
    private User testCoach;
    private User testPlayer;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("U13");
        testCategory.setSeason("2024-2025");

        testCoach = new User();
        testCoach.setId(1L);
        testCoach.setName("Coach");
        testCoach.setEmail("coach@test.com");
        testCoach.setRole(User.Role.TRAINER);

        testPlayer = new User();
        testPlayer.setId(2L);
        testPlayer.setName("Player");
        testPlayer.setEmail("player@test.com");
        testPlayer.setRole(User.Role.PLAYER);
    }

    @Test
    void testCreateCategory_Success() {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("U15");
        request.setSeason("2024-2025");

        Category savedCategory = new Category();
        savedCategory.setId(2L);
        savedCategory.setName("U15");
        savedCategory.setSeason("2024-2025");

        when(categoryRepository.findByName("U15")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        Category result = categoryService.createCategory(request);

        assertNotNull(result);
        assertEquals("U15", result.getName());
        verify(categoryRepository).findByName("U15");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testCreateCategory_WithCoach() {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("U15");
        request.setSeason("2024-2025");
        request.setCoachIds(List.of(1L));

        Category savedCategory = new Category();
        savedCategory.setId(2L);
        savedCategory.setName("U15");

        when(categoryRepository.findByName("U15")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testCoach));
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        Category result = categoryService.createCategory(request);

        assertNotNull(result);
        verify(userRepository).findById(1L);
    }

    @Test
    void testCreateCategory_DuplicateName() {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("U13");

        when(categoryRepository.findByName("U13")).thenReturn(Optional.of(testCategory));

        assertThrows(RuntimeException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testCreateCategory_CoachNotFound() {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("U15");
        request.setCoachIds(List.of(999L));

        when(categoryRepository.findByName("U15")).thenReturn(Optional.empty());
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> categoryService.createCategory(request));
    }

    @Test
    void testCreateCategory_PlayerCannotBeCoach() {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("U15");
        request.setCoachIds(List.of(2L));

        when(categoryRepository.findByName("U15")).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(testPlayer));

        assertThrows(RuntimeException.class, () -> categoryService.createCategory(request));
    }

    @Test
    void testUpdateCategory_Success() {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setName("U13 Updated");
        request.setSeason("2025-2026");

        Category updatedCategory = new Category();
        updatedCategory.setId(1L);
        updatedCategory.setName("U13 Updated");
        updatedCategory.setSeason("2025-2026");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.findByName("U13 Updated")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);

        Category result = categoryService.updateCategory(1L, request);

        assertNotNull(result);
        assertEquals("U13 Updated", result.getName());
        verify(categoryRepository).findById(1L);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testUpdateCategory_NotFound() {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> categoryService.updateCategory(999L, request));
    }

    @Test
    void testUpdateCategory_DuplicateName() {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setName("U15");

        Category existingCategory = new Category();
        existingCategory.setId(2L);
        existingCategory.setName("U15");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.findByName("U15")).thenReturn(Optional.of(existingCategory));

        assertThrows(RuntimeException.class, () -> categoryService.updateCategory(1L, request));
    }

    @Test
    void testUpdateCategory_WithCoaches() {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setCoachIds(List.of(1L));

        Category updatedCategory = new Category();
        updatedCategory.setId(1L);
        updatedCategory.setName("U13");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testCoach));
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);

        Category result = categoryService.updateCategory(1L, request);

        assertNotNull(result);
        verify(userRepository).findById(1L);
    }

    @Test
    void testFindById_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        Optional<Category> result = categoryService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(categoryRepository).findById(1L);
    }

    @Test
    void testFindById_NotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Category> result = categoryService.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void testFindAll() {
        List<Category> categories = Arrays.asList(testCategory);
        when(categoryRepository.findAllWithCoaches()).thenReturn(categories);

        List<Category> result = categoryService.findAll();

        assertEquals(1, result.size());
        verify(categoryRepository).findAllWithCoaches();
    }

    @Test
    void testFindBySeason() {
        List<Category> categories = Arrays.asList(testCategory);
        when(categoryRepository.findBySeason("2024-2025")).thenReturn(categories);

        List<Category> result = categoryService.findBySeason("2024-2025");

        assertEquals(1, result.size());
        verify(categoryRepository).findBySeason("2024-2025");
    }

    @Test
    void testSearchCategories() {
        List<Category> categories = Arrays.asList(testCategory);
        when(categoryRepository.searchByNameOrSeason("U13")).thenReturn(categories);

        List<Category> result = categoryService.searchCategories("U13");

        assertEquals(1, result.size());
        verify(categoryRepository).searchByNameOrSeason("U13");
    }

    @Test
    void testSearchCategories_EmptySearch() {
        List<Category> categories = Arrays.asList(testCategory);
        when(categoryRepository.findAllWithCoaches()).thenReturn(categories);

        List<Category> result = categoryService.searchCategories("");

        assertEquals(1, result.size());
        verify(categoryRepository).findAllWithCoaches();
    }

    @Test
    void testDeleteCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(eventRepository.findByCategoryId(1L)).thenReturn(List.of());

        categoryService.deleteCategory(1L);

        verify(categoryRepository).findById(1L);
        verify(categoryRepository).delete(testCategory);
    }

    @Test
    void testDeleteCategory_NotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> categoryService.deleteCategory(999L));
        verify(categoryRepository, never()).deleteById(anyLong());
    }
}
