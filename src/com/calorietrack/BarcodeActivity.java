package com.calorietrack;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeActivity extends AppCompatActivity {

    public static final String EXTRA_BARCODE = "barcode";
    public static final String EXTRA_PRODUCT_NAME = "product_name";
    public static final String EXTRA_CALORIES = "calories";
    public static final String EXTRA_PROTEIN = "protein";
    public static final String EXTRA_CARBS = "carbs";
    public static final String EXTRA_FAT = "fat";

    private EditText etBarcode;
    private Button btnScan, btnLookup, btnAddManual;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean ukOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);

        etBarcode = findViewById(R.id.et_barcode);
        btnScan = findViewById(R.id.btn_scan);
        btnLookup = findViewById(R.id.btn_lookup);
        btnAddManual = findViewById(R.id.btn_add_manual);

        ukOnly = getSharedPreferences("calorietrack", MODE_PRIVATE).getBoolean("uk_products_only", false);

        btnScan.setOnClickListener(v -> launchScanner());
        btnLookup.setOnClickListener(v -> lookupBarcode());
        btnAddManual.setOnClickListener(v -> showManualEntryDialog());
    }

    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.EAN_13);
        options.setPrompt("Point camera at barcode");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        barcodeLauncher.launch(options);
    }

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher =
        registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                etBarcode.setText(result.getContents());
                lookupBarcode();
            } else {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            }
        });

    private void lookupBarcode() {
        String barcode = etBarcode.getText().toString().trim();
        if (barcode.isEmpty()) {
            Toast.makeText(this, "Enter or scan a barcode", Toast.LENGTH_SHORT).show();
            return;
        }

        String formattedBarcode = formatBarcode(barcode);
        etBarcode.setText(formattedBarcode);

        executor.execute(() -> {
            JSONObject product = fetchProductFromOpenFoodFacts(formattedBarcode);
            runOnUiThread(() -> {
                if (product != null) {
                    returnProductData(product);
                } else {
                    Toast.makeText(this, "Product not found. Add manually?", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private String formatBarcode(String barcode) {
        String clean = barcode.replaceAll("\\D", "");
        if (clean.length() < 13) {
            while (clean.length() < 13) clean = "0" + clean;
        }
        return clean;
    }

    private JSONObject fetchProductFromOpenFoodFacts(String barcode) {
        try {
            String country = ukOnly ? "uk." : "";
            String urlStr = "https://" + country + "openfoodfacts.org/api/v2/product/" + barcode + ".json?fields=product_name,nutriments";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            JSONObject json = new JSONObject(response.toString());
            if (!json.has("product")) return null;
            return json.getJSONObject("product");
        } catch (Exception e) {
            return null;
        }
    }

    private void returnProductData(JSONObject product) {
        try {
            String name = product.optString("product_name", "Unknown Product");
            JSONObject nutriments = product.optJSONObject("nutriments");
            if (nutriments == null) {
                showManualEntryDialog();
                return;
            }

            int calories = nutriments.optInt("energy-kcal_100g", 0);
            int protein = nutriments.optInt("proteins_100g", 0);
            int carbs = nutriments.optInt("carbohydrates_100g", 0);
            int fat = nutriments.optInt("fat_100g", 0);

            Intent result = new Intent();
            result.putExtra(EXTRA_BARCODE, etBarcode.getText().toString());
            result.putExtra(EXTRA_PRODUCT_NAME, name);
            result.putExtra(EXTRA_CALORIES, calories);
            result.putExtra(EXTRA_PROTEIN, protein);
            result.putExtra(EXTRA_CARBS, carbs);
            result.putExtra(EXTRA_FAT, fat);
            setResult(RESULT_OK, result);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error reading product data", Toast.LENGTH_SHORT).show();
        }
    }

    private void showManualEntryDialog() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_BARCODE, etBarcode.getText().toString());
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
