package com.calorietrack;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements FoodAdapter.OnFoodItemClickListener {

    private SharedPreferences prefs;
    private DailyData todayData;
    private String currentDate;
    private Map<String, DailyData> allData;
    private FoodAdapter breakfastAdapter, lunchAdapter, dinnerAdapter, snacksAdapter;
    
    private TextView tvCaloriesEaten, tvCaloriesGoal, tvCaloriesRemaining;
    private TextView tvProtein, tvCarbs, tvFat;
    private TextView tvSteps, tvWater, tvFastingSince;
    private TextView tvWeight, tvMaintenanceCalories;
    
    private static final int SCAN_BARCODE_REQUEST = 100;
    private String currentMealType = "breakfast";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("calorietrack", MODE_PRIVATE);
        currentDate = getCurrentDate();
        allData = new HashMap<>();
        loadData();
        todayData = allData.get(currentDate);
        if (todayData == null) {
            todayData = new DailyData(currentDate);
            allData.put(currentDate, todayData);
        }

        initViews();
        updateUI();
        setupBottomNav();
    }

    private String getCurrentDate() {
        return DateFormat.format("yyyy-MM-dd", new Date()).toString();
    }

    private void initViews() {
        tvCaloriesEaten = findViewById(R.id.tv_calories_eaten);
        tvCaloriesGoal = findViewById(R.id.tv_calories_goal);
        tvCaloriesRemaining = findViewById(R.id.tv_calories_remaining);
        tvProtein = findViewById(R.id.tv_protein);
        tvCarbs = findViewById(R.id.tv_carbs);
        tvFat = findViewById(R.id.tv_fat);
        tvSteps = findViewById(R.id.tv_steps);
        tvWater = findViewById(R.id.tv_water);
        tvFastingSince = findViewById(R.id.tv_fasting_since);
        tvWeight = findViewById(R.id.tv_weight);
        tvMaintenanceCalories = findViewById(R.id.tv_maintenance);
    }

    private void updateUI() {
        // Calories
        tvCaloriesEaten.setText(String.valueOf(todayData.getCaloriesEaten()));
        tvCaloriesGoal.setText("/ " + todayData.getCaloriesGoal());
        tvCaloriesRemaining.setText(String.valueOf(todayData.getCaloriesRemaining()));

        // Macros
        tvProtein.setText(todayData.getProteinEaten() + "g / " + todayData.getProteinGoal() + "g");
        tvCarbs.setText(todayData.getCarbsEaten() + "g / " + todayData.getCarbsGoal() + "g");
        tvFat.setText(todayData.getFatEaten() + "g / " + todayData.getFatGoal() + "g");

        // Steps & Water
        tvSteps.setText(String.valueOf(todayData.getSteps()));
        tvWater.setText(todayData.getWaterGlasses() + " glasses");

        // Fasting timer
        long lastMeal = todayData.getLastMealTime();
        if (lastMeal > 0) {
            long hours = (System.currentTimeMillis() - lastMeal) / (1000 * 60 * 60);
            long mins = ((System.currentTimeMillis() - lastMeal) / (1000 * 60)) % 60;
            tvFastingSince.setText(hours + "h " + mins + "m");
        } else {
            tvFastingSince.setText("Not started");
        }

        // Weight & Maintenance
        double weightKg = prefs.getFloat("weight_kg", 70f);
        int age = prefs.getInt("age", 30);
        int heightCm = prefs.getInt("height_cm", 170);
        String sex = prefs.getString("sex", "male");
        int activityLevel = prefs.getInt("activity_level", 2);
        
        String weightUnit = prefs.getString("weight_unit", "kg");
        if (weightUnit.equals("stone")) {
            int st = (int)(weightKg / 6.35029);
            double lbs = (weightKg / 6.35029 - st) * 14;
            tvWeight.setText(String.format("%dst %.1flbs", st, lbs));
        } else {
            tvWeight.setText(String.format("%.1fkg", weightKg));
        }

        int bmr;
        if (sex.equals("male")) {
            bmr = (int)Math.round(10 * weightKg + 6.25 * heightCm - 5 * age + 5);
        } else {
            bmr = (int)Math.round(10 * weightKg + 6.25 * heightCm - 5 * age - 161);
        }
        double[] multipliers = {1.2, 1.375, 1.55, 1.725, 1.9};
        int tdee = (int)Math.round(bmr * multipliers[activityLevel]);
        tvMaintenanceCalories.setText(tdee + " kcal");
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_today) showTodayTab();
            else if (id == R.id.nav_food) showFoodTab();
            else if (id == R.id.nav_burn) showBurnTab();
            else if (id == R.id.nav_body) showBodyTab();
            else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            }
            return true;
        });
    }

    private void showTodayTab() {
        setContentView(R.layout.tab_today);
        initViews();
        updateUI();
        
        findViewById(R.id.btn_add_water).setOnClickListener(v -> {
            todayData.addWaterGlass();
            saveData();
            updateUI();
        });
        
        findViewById(R.id.btn_scan).setOnClickListener(v -> scanBarcode());
        findViewById(R.id.btn_add_food).setOnClickListener(v -> showAddFoodDialog());
    }

    private void showFoodTab() {
        setContentView(R.layout.tab_food);
        setupMealSection(R.id.rv_breakfast, "breakfast");
        setupMealSection(R.id.rv_lunch, "lunch");
        setupMealSection(R.id.rv_dinner, "dinner");
        setupMealSection(R.id.rv_snacks, "snacks");
        
        findViewById(R.id.btn_add_breakfast).setOnClickListener(v -> {
            currentMealType = "breakfast";
            showAddFoodDialog();
        });
        findViewById(R.id.btn_add_lunch).setOnClickListener(v -> {
            currentMealType = "lunch";
            showAddFoodDialog();
        });
        findViewById(R.id.btn_add_dinner).setOnClickListener(v -> {
            currentMealType = "dinner";
            showAddFoodDialog();
        });
        findViewById(R.id.btn_add_snacks).setOnClickListener(v -> {
            currentMealType = "snacks";
            showAddFoodDialog();
        });
        findViewById(R.id.btn_scan_food).setOnClickListener(v -> scanBarcode());
    }

    private void showBurnTab() {
        setContentView(R.layout.tab_burn);
        TextView tvBurned = findViewById(R.id.tv_burned_today);
        tvBurned.setText(todayData.getCaloriesBurned() + " kcal");
        
        findViewById(R.id.btn_add_activity).setOnClickListener(v -> showAddActivityDialog());
    }

    private void showBodyTab() {
        setContentView(R.layout.tab_body);
        initViews();
        updateUI();
        
        findViewById(R.id.btn_edit_weight).setOnClickListener(v -> showEditWeightDialog());
        findViewById(R.id.btn_edit_goals).setOnClickListener(v -> showEditGoalsDialog());
    }

    private void setupMealSection(int recyclerViewId, String mealType) {
        RecyclerView rv = findViewById(recyclerViewId);
        rv.setLayoutManager(new LinearLayoutManager(this));
        FoodAdapter adapter = new FoodAdapter(this, todayData.getMealItems(mealType), this);
        rv.setAdapter(adapter);
        switch(mealType) {
            case "breakfast": breakfastAdapter = adapter; break;
            case "lunch": lunchAdapter = adapter; break;
            case "dinner": dinnerAdapter = adapter; break;
            case "snacks": snacksAdapter = adapter; break;
        }
    }

    private void scanBarcode() {
        Intent intent = new Intent(this, BarcodeActivity.class);
        startActivityForResult(intent, SCAN_BARCODE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCAN_BARCODE_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                String name = data.getStringExtra(BarcodeActivity.EXTRA_PRODUCT_NAME);
                int calories = data.getIntExtra(BarcodeActivity.EXTRA_CALORIES, 0);
                int protein = data.getIntExtra(BarcodeActivity.EXTRA_PROTEIN, 0);
                int carbs = data.getIntExtra(BarcodeActivity.EXTRA_CARBS, 0);
                int fat = data.getIntExtra(BarcodeActivity.EXTRA_FAT, 0);
                String barcode = data.getStringExtra(BarcodeActivity.EXTRA_BARCODE);
                
                showConfirmProductDialog(name, calories, protein, carbs, fat, barcode);
            } else if (resultCode == RESULT_CANCELED && data != null) {
                String barcode = data.getStringExtra(BarcodeActivity.EXTRA_BARCODE);
                showManualEntryDialog(barcode);
            }
        }
    }

    private void showConfirmProductDialog(String name, int calories, int protein, int carbs, int fat, String barcode) {
        new AlertDialog.Builder(this)
            .setTitle("Product Found")
            .setName(name)
            .setMessage("Calories: " + calories + "\nProtein: " + protein + "g\nCarbs: " + carbs + "g\nFat: " + fat + "g")
            .setPositiveButton("Add to " + currentMealType, (d, w) -> {
                FoodItem item = new FoodItem(name, calories, protein, carbs, fat, barcode, currentMealType);
                todayData.addFoodItem(item);
                saveData();
                updateUI();
                notifyAdapters();
                Toast.makeText(this, "Added to " + currentMealType, Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton("Add Manually Instead", (d, w) -> showManualEntryDialog(barcode))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showAddFoodDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_add_food, null);
        EditText etName = view.findViewById(R.id.et_name);
        EditText etCalories = view.findViewById(R.id.et_calories);
        EditText etProtein = view.findViewById(R.id.et_protein);
        EditText etCarbs = view.findViewById(R.id.et_carbs);
        EditText etFat = view.findViewById(R.id.et_fat);

        new AlertDialog.Builder(this)
            .setTitle("Add Food — " + currentMealType)
            .setView(view)
            .setPositiveButton("Add", (d, w) -> {
                String name = etName.getText().toString().trim();
                int calories = parseInt(etCalories.getText().toString());
                int protein = parseInt(etProtein.getText().toString());
                int carbs = parseInt(etCarbs.getText().toString());
                int fat = parseInt(etFat.getText().toString());
                
                if (!name.isEmpty()) {
                    FoodItem item = new FoodItem(name, calories, protein, carbs, fat, "", currentMealType);
                    todayData.addFoodItem(item);
                    saveData();
                    updateUI();
                    notifyAdapters();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showManualEntryDialog(String barcode) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_add_food, null);
        EditText etName = view.findViewById(R.id.et_name);
        EditText etCalories = view.findViewById(R.id.et_calories);
        EditText etProtein = view.findViewById(R.id.et_protein);
        EditText etCarbs = view.findViewById(R.id.et_carbs);
        EditText etFat = view.findViewById(R.id.et_fat);
        
        if (barcode != null && !barcode.isEmpty()) {
            etName.setHint("Barcode: " + barcode);
        }

        new AlertDialog.Builder(this)
            .setTitle("Add Product Manually")
            .setMessage("Product not found — please enter details from the label")
            .setView(view)
            .setPositiveButton("Save Product", (d, w) -> {
                String name = etName.getText().toString().trim();
                int calories = parseInt(etCalories.getText().toString());
                int protein = parseInt(etProtein.getText().toString());
                int carbs = parseInt(etCarbs.getText().toString());
                int fat = parseInt(etFat.getText().toString());
                
                if (!name.isEmpty()) {
                    FoodItem item = new FoodItem(name, calories, protein, carbs, fat, barcode, currentMealType);
                    todayData.addFoodItem(item);
                    saveData();
                    updateUI();
                    notifyAdapters();
                    Toast.makeText(this, "Saved — next time it will find this product!", Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showAddActivityDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_add_activity, null);
        EditText etName = view.findViewById(R.id.et_activity_name);
        EditText etCalories = view.findViewById(R.id.et_activity_calories);

        new AlertDialog.Builder(this)
            .setTitle("Add Activity")
            .setView(view)
            .setPositiveButton("Add", (d, w) -> {
                int calories = parseInt(etCalories.getText().toString());
                todayData.setCaloriesBurned(todayData.getCaloriesBurned() + calories);
                saveData();
                updateUI();
                showBurnTab();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showEditWeightDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_edit_weight, null);
        EditText etWeight = view.findViewById(R.id.et_weight);
        
        String weightUnit = prefs.getString("weight_unit", "kg");
        float currentWeight = prefs.getFloat("weight_kg", 70f);
        if (weightUnit.equals("stone")) {
            etWeight.setHint("Weight in pounds");
            etWeight.setText(String.valueOf(currentWeight * 2.20462f));
        } else {
            etWeight.setText(String.valueOf(currentWeight));
        }

        new AlertDialog.Builder(this)
            .setTitle("Update Weight")
            .setView(view)
            .setPositiveButton("Save", (d, w) -> {
                try {
                    double input = Double.parseDouble(etWeight.getText().toString());
                    if (weightUnit.equals("stone")) {
                        prefs.edit().putFloat("weight_kg", (float)(input / 2.20462f)).apply();
                    } else {
                        prefs.edit().putFloat("weight_kg", (float)input).apply();
                    }
                    updateUI();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showEditGoalsDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_edit_goals, null);
        EditText etCalories = view.findViewById(R.id.et_goal_calories);
        EditText etProtein = view.findViewById(R.id.et_goal_protein);
        EditText etCarbs = view.findViewById(R.id.et_goal_carbs);
        EditText etFat = view.findViewById(R.id.et_goal_fat);
        
        etCalories.setText(String.valueOf(todayData.getCaloriesGoal()));
        etProtein.setText(String.valueOf(todayData.getProteinGoal()));
        etCarbs.setText(String.valueOf(todayData.getCarbsGoal()));
        etFat.setText(String.valueOf(todayData.getFatGoal()));

        new AlertDialog.Builder(this)
            .setTitle("Set Daily Goals")
            .setView(view)
            .setPositiveButton("Save", (d, w) -> {
                todayData.setCaloriesGoal(parseInt(etCalories.getText().toString()));
                todayData.setProteinGoal(parseInt(etProtein.getText().toString()));
                todayData.setCarbsGoal(parseInt(etCarbs.getText().toString()));
                todayData.setFatGoal(parseInt(etFat.getText().toString()));
                saveData();
                updateUI();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return 0; }
    }

    private void notifyAdapters() {
        if (breakfastAdapter != null) breakfastAdapter.notifyDataSetChanged();
        if (lunchAdapter != null) lunchAdapter.notifyDataSetChanged();
        if (dinnerAdapter != null) dinnerAdapter.notifyDataSetChanged();
        if (snacksAdapter != null) snacksAdapter.notifyDataSetChanged();
    }

    @Override
    public void onFoodItemClick(FoodItem item) {}

    @Override
    public void onFoodItemLongClick(FoodItem item) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Remove " + item.getName() + "?")
            .setPositiveButton("Delete", (d, w) -> {
                todayData.removeFoodItem(item);
                saveData();
                updateUI();
                notifyAdapters();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void loadData() {
        try {
            String saved = prefs.getString("daily_data", "{}");
            JSONObject root = new JSONObject(saved);
            for (String date : root.keySet()) {
                allData.put(date, DailyData.fromJson(root.getJSONObject(date)));
            }
        } catch (JSONException e) {
            allData = new HashMap<>();
        }
    }

    private void saveData() {
        try {
            JSONObject root = new JSONObject();
            for (Map.Entry<String, DailyData> entry : allData.entrySet()) {
                root.put(entry.getKey(), entry.getValue().toJson());
            }
            prefs.edit().putString("daily_data", root.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
