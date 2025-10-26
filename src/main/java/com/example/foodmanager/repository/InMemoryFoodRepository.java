package com.example.foodmanager.repository;

import com.example.foodmanager.model.Food;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class InMemoryFoodRepository {
    private final List<Food> store = new CopyOnWriteArrayList<>();

    public List<Food> findAll() {
        return store.stream()
                .sorted(Comparator.comparing(Food::getExpirationDate))
                .collect(Collectors.toList());
    }

    public void save(Food food) {
        store.add(food);
    }

    public boolean deleteById(String id) {
        return store.removeIf(f -> f.getId().equals(id));
    }

    public long count() { return store.size(); }

    public long countWarning() {
        LocalDate now = LocalDate.now();
        return store.stream()
                .filter(f -> {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(now, f.getExpirationDate());
                    return days >= 0 && days <= 3;
                }).count();
    }

    public long countExpired() {
        LocalDate now = LocalDate.now();
        return store.stream()
                .filter(f -> f.getExpirationDate().isBefore(now))
                .count();
    }
}