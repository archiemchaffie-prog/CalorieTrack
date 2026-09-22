package com.calorietrack;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private RadioGroup rgWeightUnit, rgHeightUnit, rgStepSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("calorietrack", MODE_PRIVATE);

        rgWeightUnit = findViewById(R.id.rg_weight_unit);
        rgHeightUnit = findViewById(R.id.rg_height_unit);
        rgStepSource = findViewById(R.id.rg_step_source);

        loadSettings();
        setupListeners();
    }

    private void loadSettings() {
        String weightUnit = prefs.getString("weight_unit", "kg");
        String heightUnit = prefs.getString("height_unit", "cm");
        String stepSource = prefs.getString("step_source", "phone");

        if (weightUnit.equals("kg")) rgWeightUnit.check(R.id.rb_kg);
        else rgWeightUnit.check(R.id.rb_stone);

        if (heightUnit.equals("cm")) rgHeightUnit.check(R.id.rb_cm);
        else rgHeightUnit.check(R.id.rb_ft_in);

        if (stepSource.equals("phone")) rgStepSource.check(R.id.rb_phone);
        else rgStepSource.check(R.id.rb_samsung_health);
    }

    private void setupListeners() {
        rgWeightUnit.setOnCheckedChangeListener((group, checkedId) -> {
            String unit = checkedId == R.id.rb_kg ? "kg" : "stone";
            prefs.edit().putString("weight_unit", unit).apply();
        });

        rgHeightUnit.setOnCheckedChangeListener((group, checkedId) -> {
            String unit = checkedId == R.id.rb_cm ? "cm" : "ft_in";
            prefs.edit().putString("height_unit", unit).apply();
        });

        rgStepSource.setOnCheckedChangeListener((group, checkedId) -> {
            String source = checkedId == R.id.rb_phone ? "phone" : "samsung_health";
            prefs.edit().putString("step_source", source).apply();
        });
    }
}
