package com.example.foodmanager.model;

import java.time.LocalDate;

public class Food {
    private final String id;
    private final String name;
    private final LocalDate expirationDate;

    public Food(String id, String name, LocalDate expirationDate) {
        this.id = id;
        this.name = name;
        this.expirationDate = expirationDate;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public LocalDate getExpirationDate() { return expirationDate; }
}
