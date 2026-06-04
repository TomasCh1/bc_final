package com.basketball.app.controller;

import com.basketball.app.dto.CategoryCreateRequest;
import com.basketball.app.dto.CategoryUpdateRequest;
import com.basketball.app.model.Category;
import com.basketball.app.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = CategoryController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private com.basketball.app.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.basketball.app.security.CustomUserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private Category testCategory;
    private Map<String, Object> categoryMap;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("U13");
        testCategory.setSeason("2024-2025");

        categoryMap = new HashMap<>();
        categoryMap.put("id", 1L);
        categoryMap.put("name", "U13");
        categoryMap.put("season", "2024-2025");
    }

    @Test
    void testCreateCategory_Success() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("U15");
        request.setSeason("2024-2025");

        Category savedCategory = new Category();
        savedCategory.setId(2L);
        savedCategory.setName("U15");

        when(categoryService.createCategory(any(CategoryCreateRequest.class))).thenReturn(savedCategory);
        when(categoryService.toCategoryMap(any(Category.class))).thenReturn(categoryMap);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        verify(categoryService).createCategory(any(CategoryCreateRequest.class));
    }

    @Test
    void testGetAllCategories_Success() throws Exception {
        List<Category> categories = Arrays.asList(testCategory);
        when(categoryService.findAll()).thenReturn(categories);
        when(categoryService.toCategoryMap(any(Category.class))).thenReturn(categoryMap);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("U13"));

        verify(categoryService).findAll();
    }

    @Test
    void testGetAllCategories_WithSearch() throws Exception {
        List<Category> categories = Arrays.asList(testCategory);
        when(categoryService.searchCategories("U13")).thenReturn(categories);
        when(categoryService.toCategoryMap(any(Category.class))).thenReturn(categoryMap);

        mockMvc.perform(get("/api/categories")
                        .param("search", "U13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("U13"));

        verify(categoryService).searchCategories("U13");
    }

    @Test
    void testGetAllCategories_WithSeason() throws Exception {
        List<Category> categories = Arrays.asList(testCategory);
        when(categoryService.findBySeason("2024-2025")).thenReturn(categories);
        when(categoryService.toCategoryMap(any(Category.class))).thenReturn(categoryMap);

        mockMvc.perform(get("/api/categories")
                        .param("season", "2024-2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].season").value("2024-2025"));

        verify(categoryService).findBySeason("2024-2025");
    }

    @Test
    void testGetCategoryById_Success() throws Exception {
        when(categoryService.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryService.toCategoryMap(any(Category.class))).thenReturn(categoryMap);

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("U13"));

        verify(categoryService).findById(1L);
    }

    @Test
    void testGetCategoryById_NotFound() throws Exception {
        when(categoryService.findById(999L)).thenReturn(Optional.empty());
        when(categoryService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        verify(categoryService).findById(999L);
    }

    @Test
    void testUpdateCategory_Success() throws Exception {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setName("U13 Updated");

        Category updatedCategory = new Category();
        updatedCategory.setId(1L);
        updatedCategory.setName("U13 Updated");

        when(categoryService.updateCategory(eq(1L), any(CategoryUpdateRequest.class))).thenReturn(updatedCategory);
        when(categoryService.toCategoryMap(any(Category.class))).thenReturn(categoryMap);

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(categoryService).updateCategory(eq(1L), any(CategoryUpdateRequest.class));
    }

    @Test
    void testDeleteCategory_Success() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));

        verify(categoryService).deleteCategory(1L);
    }
}
