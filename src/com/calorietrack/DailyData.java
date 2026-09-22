package com.calorietrack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class DailyData {
    private String date;
    private int caloriesGoal;
    private int proteinGoal;
    private int carbsGoal;
    private int fatGoal;
    private List<FoodItem> foodItems;
    private int caloriesBurned;
    private int steps;
    private int waterGlasses;
    private long lastMealTime;

    public DailyData(String date) {
        this.date = date;
        this.caloriesGoal = 2000;
        this.proteinGoal = 150;
        this.carbsGoal = 250;
        this.fatGoal = 65;
        this.foodItems = new ArrayList<>();
        this.caloriesBurned = 0;
        this.steps = 0;
        this.waterGlasses = 0;
        this.lastMealTime = 0;
    }

    public String getDate() { return date; }
    
    public int getCaloriesEaten() {
        int total = 0;
        for (FoodItem item : foodItems) total += item.getCalories();
        return total;
    }
    
    public int getProteinEaten() {
        int total = 0;
        for (FoodItem item : foodItems) total += item.getProtein();
        return total;
    }
    
    public int getCarbsEaten() {
        int total = 0;
        for (FoodItem item : foodItems) total += item.getCarbs();
        return total;
    }
    
    public int getFatEaten() {
        int total = 0;
        for (FoodItem item : foodItems) total += item.getFat();
        return total;
    }

    public int getCaloriesRemaining() { return caloriesGoal - getCaloriesEaten(); }
    
    public void addFoodItem(FoodItem item) {
        foodItems.add(item);
        lastMealTime = System.currentTimeMillis();
    }
    
    public void removeFoodItem(FoodItem item) { foodItems.remove(item); }
    
    public List<FoodItem> getFoodItems() { return foodItems; }
    
    public List<FoodItem> getMealItems(String mealType) {
        List<FoodItem> result = new ArrayList<>();
        for (FoodItem item : foodItems) {
            if (mealType.equals(item.getMealType())) result.add(item);
        }
        return result;
    }

    public int getCaloriesGoal() { return caloriesGoal; }
    public void setCaloriesGoal(int caloriesGoal) { this.caloriesGoal = caloriesGoal; }
    public int getProteinGoal() { return proteinGoal; }
    public void setProteinGoal(int proteinGoal) { this.proteinGoal = proteinGoal; }
    public int getCarbsGoal() { return carbsGoal; }
    public void setCarbsGoal(int carbsGoal) { this.carbsGoal = carbsGoal; }
    public int getFatGoal() { return fatGoal; }
    public void setFatGoal(int fatGoal) { this.fatGoal = fatGoal; }
    public int getCaloriesBurned() { return caloriesBurned; }
    public void setCaloriesBurned(int caloriesBurned) { this.caloriesBurned = caloriesBurned; }
    public int getSteps() { return steps; }
    public void setSteps(int steps) { this.steps = steps; }
    public int getWaterGlasses() { return waterGlasses; }
    public void setWaterGlasses(int waterGlasses) { this.waterGlasses = waterGlasses; }
    public void addWaterGlass() { this.waterGlasses++; }
    public long getLastMealTime() { return lastMealTime; }
    public void setLastMealTime(long time) { this.lastMealTime = time; }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("date", date);
        json.put("caloriesGoal", caloriesGoal);
        json.put("proteinGoal", proteinGoal);
        json.put("carbsGoal", carbsGoal);
        json.put("fatGoal", fatGoal);
        json.put("caloriesBurned", caloriesBurned);
        json.put("steps", steps);
        json.put("waterGlasses", waterGlasses);
        json.put("lastMealTime", lastMealTime);
        
        JSONArray foodArray = new JSONArray();
        for (FoodItem item : foodItems) {
            JSONObject fi = new JSONObject();
            fi.put("name", item.getName());
            fi.put("calories", item.getCalories());
            fi.put("protein", item.getProtein());
            fi.put("carbs", item.getCarbs());
            fi.put("fat", item.getFat());
            fi.put("barcode", item.getBarcode());
            fi.put("mealType", item.getMealType());
            fi.put("timestamp", item.getTimestamp());
            foodArray.put(fi);
        }
        json.put("foodItems", foodArray);
        return json;
    }

    public static DailyData fromJson(JSONObject json) throws JSONException {
        DailyData data = new DailyData(json.getString("date"));
        data.caloriesGoal = json.getInt("caloriesGoal");
        data.proteinGoal = json.getInt("proteinGoal");
        data.carbsGoal = json.getInt("carbsGoal");
        data.fatGoal = json.getInt("fatGoal");
        data.caloriesBurned = json.getInt("caloriesBurned");
        data.steps = json.getInt("steps");
        data.waterGlasses = json.getInt("waterGlasses");
        data.lastMealTime = json.optLong("lastMealTime", 0);
        
        JSONArray foodArray = json.optJSONArray("foodItems");
        if (foodArray != null) {
            for (int i = 0; i < foodArray.length(); i++) {
                JSONObject fi = foodArray.getJSONObject(i);
                FoodItem item = new FoodItem(
                    fi.getString("name"),
                    fi.getInt("calories"),
                    fi.getInt("protein"),
                    fi.getInt("carbs"),
                    fi.getInt("fat"),
                    fi.optString("barcode", ""),
                    fi.getString("mealType")
                );
                data.foodItems.add(item);
            }
        }
        return data;
    }
}
