package com.basketball.app.controller;

import com.basketball.app.dto.CategoryCreateRequest;
import com.basketball.app.dto.CategoryUpdateRequest;
import com.basketball.app.model.Category;
import com.basketball.app.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Create a new category
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        try {
            Category category = categoryService.createCategory(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.toCategoryMap(category));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all categories.
     */
    @GetMapping
    public ResponseEntity<?> getAllCategories(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) Long coachId,
            @RequestParam(required = false, defaultValue = "false") boolean summary) {
        
        List<Category> categories;
        if (search != null && !search.trim().isEmpty()) {
            categories = categoryService.searchCategories(search);
        } else if (season != null) {
            categories = categoryService.findBySeason(season);
        } else if (coachId != null) {
            categories = categoryService.findByCoach(coachId);
        } else if (summary) {
            categories = categoryService.findAllForSummary();
        } else {
            categories = categoryService.findAll();
        }
        
        List<Map<String, Object>> result = categories.stream()
                .map(c -> summary ? categoryService.toCategorySummaryMap(c) : categoryService.toCategoryMap(c))
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get player count per category (stats only, no user list). For admin categories overview.
     */
    @GetMapping("/player-counts")
    public ResponseEntity<?> getPlayerCounts() {
        return ResponseEntity.ok(categoryService.getPlayerCountByCategoryId());
    }

    /**
     * Get category by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        try {
            // Fetch category and ensure coaches are loaded
            Optional<Category> categoryOpt = categoryService.findById(id);
            if (categoryOpt.isEmpty()) {
                // Try fetching from all categories with coaches loaded
                List<Category> allCategories = categoryService.findAll();
                Category category = allCategories.stream()
                        .filter(c -> c.getId().equals(id))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Category not found: " + id));
                return ResponseEntity.ok(categoryService.toCategoryMap(category));
            }
            
            Category category = categoryOpt.get();
            // Ensure coaches are loaded
            try {
                category.getCoaches().size(); // Force lazy load
            } catch (Exception e) {
                // If lazy loading fails, fetch with coaches
                List<Category> allCategories = categoryService.findAll();
                category = allCategories.stream()
                        .filter(c -> c.getId().equals(id))
                        .findFirst()
                        .orElse(category);
            }
            
            return ResponseEntity.ok(categoryService.toCategoryMap(category));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update category
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCategory(@PathVariable Long id,
                                           @Valid @RequestBody CategoryUpdateRequest request) {
        try {
            Category category = categoryService.updateCategory(id, request);
            return ResponseEntity.ok(categoryService.toCategoryMap(category));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete category
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(Map.of("message", "Category deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Assign coach to category
     */
    @PostMapping("/{id}/assign-coach")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignCoach(@PathVariable Long id,
                                        @RequestBody Map<String, Long> request) {
        try {
            Long coachId = request.get("coachId");
            if (coachId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "coachId is required"));
            }
            Category category = categoryService.assignCoach(id, coachId);
            return ResponseEntity.ok(categoryService.toCategoryMap(category));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Remove coach from category
     */
    @PostMapping("/{id}/remove-coach")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeCoach(@PathVariable Long id) {
        try {
            Category category = categoryService.removeCoach(id);
            return ResponseEntity.ok(categoryService.toCategoryMap(category));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Search categories
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchCategories(@RequestParam String q) {
        List<Category> categories = categoryService.searchCategories(q);
        List<Map<String, Object>> enrichedCategories = categories.stream()
                .map(categoryService::toCategoryMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(enrichedCategories);
    }

    /**
     * Get categories by season
     */
    @GetMapping("/season/{season}")
    public ResponseEntity<?> getCategoriesBySeason(@PathVariable String season) {
        List<Category> categories = categoryService.findBySeason(season);
        List<Map<String, Object>> enrichedCategories = categories.stream()
                .map(categoryService::toCategoryMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(enrichedCategories);
    }
}

