package com.basketball.app.repository;

import com.basketball.app.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findBySeason(String season);
    
    Optional<Category> findByName(String name);
    
    List<Category> findByCoachId(Long coachId);
    
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.coaches coach WHERE coach.id = :coachId")
    List<Category> findByCoachInCoaches(@Param("coachId") Long coachId);
    
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.coaches")
    List<Category> findAllWithCoaches();
    
    @Query("SELECT c FROM Category c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.season) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Category> searchByNameOrSeason(@Param("search") String search);
}

