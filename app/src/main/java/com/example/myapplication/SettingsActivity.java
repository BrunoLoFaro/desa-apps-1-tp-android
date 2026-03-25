package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.network.RetrofitClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText baseUrlEditText;
    private TextInputEditText endpointEditText;
    private TextInputEditText otpRequestEndpointEditText;
    private TextInputEditText otpVerifyEndpointEditText;
    private MaterialButton saveButton;
    private LinearProgressIndicator saveProgress;
    private View coordinator;
    private ConfigLoader configLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        configLoader = new ConfigLoader(this);
        coordinator = findViewById(R.id.settings_coordinator);
        baseUrlEditText = findViewById(R.id.base_url_edit_text);
        endpointEditText = findViewById(R.id.endpoint_edit_text);
        otpRequestEndpointEditText = findViewById(R.id.otp_request_endpoint_edit_text);
        otpVerifyEndpointEditText = findViewById(R.id.otp_verify_endpoint_edit_text);
        saveButton = findViewById(R.id.save_button);
        saveProgress = findViewById(R.id.save_progress);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_coordinator), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadCurrentSettings();

        saveButton.setOnClickListener(v -> saveSettings());
    }

    private void loadCurrentSettings() {
        AppConfig config = configLoader.loadConfig();
        if (config != null) {
            baseUrlEditText.setText(config.baseUrl);
            endpointEditText.setText(config.loginEndpoint);
            otpRequestEndpointEditText.setText(config.otpRequestEndpoint);
            otpVerifyEndpointEditText.setText(config.otpVerifyEndpoint);
        }
    }

    private void saveSettings() {
        String baseUrl = baseUrlEditText.getText().toString().trim();
        String endpoint = endpointEditText.getText().toString().trim();
        String otpRequestEndpoint = otpRequestEndpointEditText.getText().toString().trim();
        String otpVerifyEndpoint = otpVerifyEndpointEditText.getText().toString().trim();

        if (baseUrl.isEmpty() || endpoint.isEmpty() || otpRequestEndpoint.isEmpty() || otpVerifyEndpoint.isEmpty()) {
            showError(getString(R.string.invalid_input));
            return;
        }

        saveProgress.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        AppConfig newConfig = new AppConfig();
        newConfig.baseUrl = baseUrl;
        newConfig.loginEndpoint = endpoint;
        newConfig.otpRequestEndpoint = otpRequestEndpoint;
        newConfig.otpVerifyEndpoint = otpVerifyEndpoint;
        newConfig.otpTtlSeconds = 120;

        configLoader.saveConfig(newConfig);
        
        // Reset RetrofitClient to force re-initialization with new config
        RetrofitClient.reset();

        saveProgress.setVisibility(View.GONE);
        saveButton.setEnabled(true);
        Snackbar.make(coordinator, R.string.settings_saved, Snackbar.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Snackbar.make(coordinator, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.error, getTheme()))
                .setTextColor(getResources().getColor(R.color.onError, getTheme()))
                .show();
    }
}
