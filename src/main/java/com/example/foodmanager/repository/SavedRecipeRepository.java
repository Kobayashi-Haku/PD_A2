package com.example.foodmanager.repository;

import com.example.foodmanager.model.SavedRecipe;
import com.example.foodmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavedRecipeRepository extends JpaRepository<SavedRecipe, Long> {
    List<SavedRecipe> findByUserOrderBySavedAtDesc(User user);
    long countByUser(User user);
}