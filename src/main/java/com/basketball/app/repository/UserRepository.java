package com.basketball.app.repository;

import com.basketball.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByPasswordResetTokenHash(String passwordResetTokenHash);

    /** For login: only non-deleted users can log in. Treats NULL deleted as not deleted (existing rows before column existed). */
    @Query("SELECT u FROM User u WHERE u.email = :email AND (u.deleted = false OR u.deleted IS NULL)")
    Optional<User> findByEmailAndDeletedFalse(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE LOWER(TRIM(u.email)) = LOWER(TRIM(:email)) AND (u.deleted = false OR u.deleted IS NULL)")
    Optional<User> findByEmailTrimmedIgnoreCaseAndDeletedFalse(@Param("email") String email);

    /** Same as above but with categories loaded (for login response with categoryIds). */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.categories WHERE u.email = :email AND (u.deleted = false OR u.deleted IS NULL)")
    Optional<User> findByEmailAndDeletedFalseWithCategories(@Param("email") String email);

    boolean existsByEmail(String email);

    /** For create/update: allow same email if only deleted user had it. Treats NULL deleted as not deleted. */
    @Query("SELECT COUNT(u) FROM User u WHERE u.email = :email AND (u.deleted = false OR u.deleted IS NULL)")
    long countByEmailAndNotDeleted(@Param("email") String email);

    default boolean existsByEmailAndDeletedFalse(String email) {
        return countByEmailAndNotDeleted(email) > 0;
    }

    // Search and filter methods (exclude deleted users from lists)
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.categories WHERE u.role = :role AND (u.deleted = false OR u.deleted IS NULL)")
    List<User> findByRole(@Param("role") User.Role role);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.categories WHERE u.categoryId = :categoryId AND (u.deleted = false OR u.deleted IS NULL)")
    List<User> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.categories c WHERE c.id = :categoryId AND (u.deleted = false OR u.deleted IS NULL)")
    List<User> findByCategoryInCategories(@Param("categoryId") Long categoryId);

    /** Coaches linked via category_coaches (many-to-many), not deprecated coachId. */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.categories WHERE " +
           "EXISTS (SELECT 1 FROM Category cat JOIN cat.coaches coach WHERE cat.id = :categoryId AND coach = u) AND (u.deleted = false OR u.deleted IS NULL)")
    List<User> findByCoachedCategory(@Param("categoryId") Long categoryId);

    /** Users in category (user_categories) OR coaches of category (category_coaches). Uses many-to-many, not deprecated coachId. */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.categories c WHERE (u.deleted = false OR u.deleted IS NULL) AND " +
           "(EXISTS (SELECT 1 FROM User u2 JOIN u2.categories c2 WHERE u2 = u AND c2.id = :categoryId) OR " +
           "EXISTS (SELECT 1 FROM Category cat JOIN cat.coaches coach WHERE cat.id = :categoryId AND coach = u))")
    List<User> findByCategoryInCategoriesOrCoached(@Param("categoryId") Long categoryId);

    List<User> findByIsActive(Boolean isActive);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.categories WHERE u.role = :role AND u.categoryId = :categoryId AND (u.deleted = false OR u.deleted IS NULL)")
    List<User> findByRoleAndCategoryId(@Param("role") User.Role role, @Param("categoryId") Long categoryId);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.categories c WHERE u.role = :role AND (u.deleted = false OR u.deleted IS NULL) AND " +
           "(EXISTS (SELECT 1 FROM User u2 JOIN u2.categories c2 WHERE u2 = u AND c2.id = :categoryId) OR " +
           "EXISTS (SELECT 1 FROM Category cat JOIN cat.coaches coach WHERE cat.id = :categoryId AND coach = u))")
    List<User> findByRoleAndCategoryInCategoriesOrCoached(@Param("role") User.Role role, @Param("categoryId") Long categoryId);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.categories WHERE " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.surname) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND (u.deleted = false OR u.deleted IS NULL)")
    List<User> searchByNameOrEmail(@Param("search") String search);

    @Query("SELECT u FROM User u WHERE u.categoryId = :categoryId AND u.role = :role AND (u.deleted = false OR u.deleted IS NULL)")
    List<User> findByCategoryIdAndRole(@Param("categoryId") Long categoryId, @Param("role") User.Role role);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.categories WHERE (u.deleted = false OR u.deleted IS NULL)")
    List<User> findAllWithCategories();

    /** Player count per category (user_categories + users with role PLAYER, non-deleted). Single query, no full user load. */
    @Query(value = "SELECT uc.category_id AS categoryId, COUNT(uc.user_id) AS playerCount FROM user_categories uc " +
            "INNER JOIN users u ON u.id = uc.user_id WHERE (u.deleted = false OR u.deleted IS NULL) AND u.role = 'PLAYER' " +
            "GROUP BY uc.category_id", nativeQuery = true)
    List<PlayerCountByCategory> getPlayerCountPerCategory();

    /** Remove all user–category links for a category (including deleted users). Use when deleting a category. */
    @Modifying
    @Query(value = "DELETE FROM user_categories WHERE category_id = :categoryId", nativeQuery = true)
    void deleteUserCategoryLinksByCategoryId(@Param("categoryId") Long categoryId);

    /** Clear legacy category_id on users for a category. Use when deleting a category. */
    @Modifying
    @Query(value = "UPDATE users SET category_id = NULL WHERE category_id = :categoryId", nativeQuery = true)
    void clearLegacyCategoryIdByCategoryId(@Param("categoryId") Long categoryId);
}

