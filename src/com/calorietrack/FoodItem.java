package com.calorietrack;

public class FoodItem {
    private String name;
    private int calories;
    private int protein;
    private int carbs;
    private int fat;
    private String barcode;
    private String mealType; // breakfast, lunch, dinner, snacks
    private long timestamp;

    public FoodItem() {}

    public FoodItem(String name, int calories, int protein, int carbs, int fat, 
                    String barcode, String mealType) {
        this.name = name;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.barcode = barcode;
        this.mealType = mealType;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getName() { return name; }
    public int getCalories() { return calories; }
    public int getProtein() { return protein; }
    public int getCarbs() { return carbs; }
    public int getFat() { return fat; }
    public String getBarcode() { return barcode; }
    public String getMealType() { return mealType; }
    public long getTimestamp() { return timestamp; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setCalories(int calories) { this.calories = calories; }
    public void setProtein(int protein) { this.protein = protein; }
    public void setCarbs(int carbs) { this.carbs = carbs; }
    public void setFat(int fat) { this.fat = fat; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public void setMealType(String mealType) { this.mealType = mealType; }
}
