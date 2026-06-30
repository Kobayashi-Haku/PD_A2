package com.example.foodmanager.model;

/**
 * 食品のカテゴリ
 */
public enum FoodCategory {
    VEGETABLE("野菜"),
    MEAT("肉"),
    FISH("魚"),
    DAIRY("乳製品"),
    GRAIN("穀物・米・パン"),
    SEASONING("調味料"),
    FROZEN("冷凍食品"),
    OTHER("その他");

    private final String label;

    FoodCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
