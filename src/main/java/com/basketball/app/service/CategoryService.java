package com.basketball.app.service;

import com.basketball.app.dto.CategoryCreateRequest;
import com.basketball.app.dto.CategoryUpdateRequest;
import com.basketball.app.model.Category;
import com.basketball.app.model.Event;
import com.basketball.app.repository.AttendanceRepository;
import com.basketball.app.repository.CategoryRepository;
import com.basketball.app.repository.EventRepository;
import com.basketball.app.repository.StatisticsRepository;
import com.basketball.app.repository.UserRepository;
import com.basketball.app.repository.PlayerCountByCategory;
import com.basketball.app.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final StatisticsRepository statisticsRepository;
    private final AttendanceRepository attendanceRepository;

    public CategoryService(CategoryRepository categoryRepository,
                          UserRepository userRepository,
                          EventRepository eventRepository,
                          StatisticsRepository statisticsRepository,
                          AttendanceRepository attendanceRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.statisticsRepository = statisticsRepository;
        this.attendanceRepository = attendanceRepository;
    }

    /**
     * Create a new category
     */
    @Transactional
    public Category createCategory(CategoryCreateRequest request) {
        // Validate name uniqueness
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Category with name " + request.getName() + " already exists");
        }

        // Validate and set coaches (if provided)
        Set<User> coaches = new HashSet<>();
        if (request.getCoachIds() != null && !request.getCoachIds().isEmpty()) {
            for (Long coachId : request.getCoachIds()) {
                User coach = userRepository.findById(coachId)
                        .orElseThrow(() -> new RuntimeException("Coach not found: " + coachId));
                if (Boolean.TRUE.equals(coach.getDeleted())) {
                    throw new RuntimeException("Coach not found: " + coachId);
                }
                if (coach.getRole() != User.Role.TRAINER && coach.getRole() != User.Role.ADMIN) {
                    throw new RuntimeException("User must be a TRAINER or ADMIN to be assigned as coach");
                }
                coaches.add(coach);
            }
        } else if (request.getCoachId() != null) {
            // Support legacy single coachId for backwards compatibility
            User coach = userRepository.findById(request.getCoachId())
                    .orElseThrow(() -> new RuntimeException("Coach not found: " + request.getCoachId()));
            if (Boolean.TRUE.equals(coach.getDeleted())) {
                throw new RuntimeException("Coach not found: " + request.getCoachId());
            }
            if (coach.getRole() != User.Role.TRAINER && coach.getRole() != User.Role.ADMIN) {
                throw new RuntimeException("User must be a TRAINER or ADMIN to be assigned as coach");
            }
            coaches.add(coach);
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setSeason(request.getSeason());
        category.setCoachId(request.getCoachId()); // Keep for backwards compatibility
        category.setCoaches(coaches); // Set multiple coaches

        return categoryRepository.save(category);
    }

    /**
     * Update category
     */
    @Transactional
    public Category updateCategory(Long id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));

        // Validate name uniqueness if changed
        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.findByName(request.getName()).isPresent()) {
                throw new RuntimeException("Category with name " + request.getName() + " already exists");
            }
            category.setName(request.getName());
        }

        // Update coaches (if provided)
        if (request.getCoachIds() != null) {
            // Clear all existing coaches first to ensure proper replacement
            category.getCoaches().clear();
            // Also clear legacy coachId field to prevent stale data showing old coaches
            category.setCoachId(null);
            
            Set<User> coaches = new HashSet<>();
            for (Long coachId : request.getCoachIds()) {
                User coach = userRepository.findById(coachId)
                        .orElseThrow(() -> new RuntimeException("Coach not found: " + coachId));
                if (Boolean.TRUE.equals(coach.getDeleted())) {
                    throw new RuntimeException("Coach not found: " + coachId);
                }
                if (coach.getRole() != User.Role.TRAINER && coach.getRole() != User.Role.ADMIN) {
                    throw new RuntimeException("User must be a TRAINER or ADMIN to be assigned as coach");
                }
                coaches.add(coach);
            }
            // Add new coaches - this will replace all existing coaches
            category.getCoaches().addAll(coaches);
        } else if (request.getCoachId() != null) {
            // Support legacy single coachId for backwards compatibility
            User coach = userRepository.findById(request.getCoachId())
                    .orElseThrow(() -> new RuntimeException("Coach not found: " + request.getCoachId()));
            if (Boolean.TRUE.equals(coach.getDeleted())) {
                throw new RuntimeException("Coach not found: " + request.getCoachId());
            }
            if (coach.getRole() != User.Role.TRAINER && coach.getRole() != User.Role.ADMIN) {
                throw new RuntimeException("User must be a TRAINER or ADMIN to be assigned as coach");
            }
            // Clear many-to-many relationship and use legacy field
            category.getCoaches().clear();
            category.setCoachId(request.getCoachId());
            category.getCoaches().add(coach);
        }
        // If neither coachIds nor coachId provided, leave existing coaches unchanged

        if (request.getSeason() != null) {
            category.setSeason(request.getSeason());
        }

        return categoryRepository.save(category);
    }

    /**
     * Get category by ID (with coaches loaded)
     */
    public Optional<Category> findById(Long id) {
        // First try to get from cache if available, otherwise fetch fresh
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isPresent()) {
            // Force load coaches by accessing them
            try {
                category.get().getCoaches().size(); // Trigger lazy load
            } catch (Exception e) {
                // If lazy loading fails, try fetching with coaches
                List<Category> categoriesWithCoaches = categoryRepository.findAllWithCoaches();
                return categoriesWithCoaches.stream()
                        .filter(c -> c.getId().equals(id))
                        .findFirst();
            }
        }
        return category;
    }

    /**
     * Get all categories
     */
    public List<Category> findAll() {
        return categoryRepository.findAllWithCoaches();
    }

    /**
     * Get all categories without loading coaches (for summary/list payloads).
     */
    public List<Category> findAllForSummary() {
        return categoryRepository.findAll();
    }
    
    /**
     * Get category with coach names (for enriched responses)
     */
    public Map<String, Object> toCategoryMap(Category category) {
        Map<String, Object> categoryMap = new HashMap<>();
        categoryMap.put("id", category.getId());
        categoryMap.put("name", category.getName());
        categoryMap.put("season", category.getSeason());
        categoryMap.put("coachId", category.getCoachId()); // Keep for backwards compatibility
        
        try {
            Set<User> coaches = category.getCoaches();
            if (coaches != null && !coaches.isEmpty()) {
                List<Map<String, Object>> coachesList = coaches.stream()
                        .map(coach -> {
                            Map<String, Object> coachMap = new HashMap<>();
                            coachMap.put("id", coach.getId());
                            coachMap.put("name", com.basketball.app.util.UserNames.getDisplayName(coach));
                            coachMap.put("email", coach.getEmail());
                            coachMap.put("role", coach.getRole().name());
                            return coachMap;
                        })
                        .collect(Collectors.toList());
                categoryMap.put("coachIds", coaches.stream()
                        .map(User::getId)
                        .collect(Collectors.toList()));
                categoryMap.put("coaches", coachesList);
                
                List<String> coachNames = coaches.stream()
                        .map(User::getName)
                        .collect(Collectors.toList());
                categoryMap.put("coachNames", coachNames);
                categoryMap.put("coachName", String.join(", ", coachNames)); // For backwards compatibility
            } else {
                categoryMap.put("coachIds", Collections.emptyList());
                categoryMap.put("coaches", Collections.emptyList());
                categoryMap.put("coachNames", Collections.emptyList());
                
                // Fallback to old coachId if no coaches in set
                if (category.getCoachId() != null) {
                    Optional<User> coach = userRepository.findById(category.getCoachId());
                    if (coach.isPresent()) {
                        categoryMap.put("coachName", com.basketball.app.util.UserNames.getDisplayName(coach.get()));
                    } else {
                        categoryMap.put("coachName", null);
                    }
                } else {
                    categoryMap.put("coachName", null);
                }
            }
        } catch (Exception e) {
            categoryMap.put("coachIds", Collections.emptyList());
            categoryMap.put("coaches", Collections.emptyList());
            categoryMap.put("coachNames", Collections.emptyList());
            categoryMap.put("coachName", null);
        }
        
        categoryMap.put("createdAt", category.getCreatedAt());
        categoryMap.put("updatedAt", category.getUpdatedAt());
        return categoryMap;
    }

    /**
     * Minimal map for list/dropdown: id, name, season. No coaches loaded.
     */
    public Map<String, Object> toCategorySummaryMap(Category category) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", category.getId());
        map.put("name", category.getName());
        map.put("season", category.getSeason());
        return map;
    }

    /**
     * Player count per category (only counts, no user data). For admin categories overview.
     */
    public Map<Long, Long> getPlayerCountByCategoryId() {
        Map<Long, Long> map = new HashMap<>();
        for (PlayerCountByCategory p : userRepository.getPlayerCountPerCategory()) {
            if (p.getCategoryId() != null && p.getPlayerCount() != null) {
                map.put(p.getCategoryId(), p.getPlayerCount());
            }
        }
        return map;
    }

    /**
     * Search categories by name or season
     */
    public List<Category> searchCategories(String search) {
        if (search == null || search.trim().isEmpty()) {
            return findAll();
        }
        return categoryRepository.searchByNameOrSeason(search.trim());
    }

    /**
     * Filter categories by season
     */
    public List<Category> findBySeason(String season) {
        return categoryRepository.findBySeason(season);
    }

    /**
     * Filter categories by coach (supports many-to-many relationship)
     */
    public List<Category> findByCoach(Long coachId) {
        List<Category> allCategories = categoryRepository.findAllWithCoaches();
        
        List<Category> categories = allCategories.stream()
                .filter(category -> {
                    boolean hasCoach = category.getCoaches() != null && 
                            category.getCoaches().stream()
                                    .anyMatch(coach -> coach.getId().equals(coachId));
                    boolean isLegacyCoach = category.getCoachId() != null && 
                            category.getCoachId().equals(coachId);
                    return hasCoach || isLegacyCoach;
                })
                .collect(Collectors.toList());
        
        return categories;
    }

    /**
     * Assign coach to category (legacy method - use updateCategory with coachIds instead)
     */
    @Transactional
    public Category assignCoach(Long categoryId, Long coachId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));

        User coach = userRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("Coach not found: " + coachId));
        if (Boolean.TRUE.equals(coach.getDeleted())) {
            throw new RuntimeException("Coach not found: " + coachId);
        }
        if (coach.getRole() != User.Role.TRAINER && coach.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("User must be a TRAINER or ADMIN to be assigned as coach");
        }

        category.getCoaches().add(coach);
        category.setCoachId(coachId);
        return categoryRepository.save(category);
    }

    /**
     * Remove coach from category
     */
    @Transactional
    public Category removeCoach(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));

        category.getCoaches().clear();
        category.setCoachId(null);
        return categoryRepository.save(category);
    }

    /**
     * Remove a single coach from a category.
     */
    @Transactional
    public void removeCoachFromCategory(Long categoryId, Long coachId) {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) return;
        if (category.getCoaches() != null) {
            category.getCoaches().removeIf(c -> c.getId().equals(coachId));
            if (coachId.equals(category.getCoachId())) {
                category.setCoachId(null);
            }
            categoryRepository.save(category);
        }
    }

    /**
     * Delete category and all data tied to it (events, stats, attendance).
     * Users are not deleted; they are only unassigned from this category.
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));

        userRepository.deleteUserCategoryLinksByCategoryId(id);
        userRepository.clearLegacyCategoryIdByCategoryId(id);

        category.getCoaches().clear();
        category.setCoachId(null);
        categoryRepository.save(category);

        List<Event> events = eventRepository.findByCategoryId(id);
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        if (!eventIds.isEmpty()) {
            statisticsRepository.deleteAll(statisticsRepository.findByEventIdIn(eventIds));
            attendanceRepository.deleteAll(attendanceRepository.findByEventIdIn(eventIds));
        }
        eventRepository.deleteAll(events);

        categoryRepository.delete(category);
    }
}

