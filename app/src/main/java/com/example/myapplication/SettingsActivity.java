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
    private TextInputEditText loginEndpointEditText;
    private TextInputEditText registerEndpointEditText;
    private TextInputEditText signupOtpRequestEndpointEditText;
    private TextInputEditText signupOtpResendEndpointEditText;
    private TextInputEditText signupOtpCompleteEndpointEditText;
    private TextInputEditText passwordResetRequestEndpointEditText;
    private TextInputEditText passwordResetResendEndpointEditText;
    private TextInputEditText passwordResetConfirmEndpointEditText;
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
        loginEndpointEditText = findViewById(R.id.login_endpoint_edit_text);
        registerEndpointEditText = findViewById(R.id.register_endpoint_edit_text);
        signupOtpRequestEndpointEditText = findViewById(R.id.signup_otp_request_endpoint_edit_text);
        signupOtpResendEndpointEditText = findViewById(R.id.signup_otp_resend_endpoint_edit_text);
        signupOtpCompleteEndpointEditText = findViewById(R.id.signup_otp_complete_endpoint_edit_text);
        passwordResetRequestEndpointEditText = findViewById(R.id.password_reset_request_endpoint_edit_text);
        passwordResetResendEndpointEditText = findViewById(R.id.password_reset_resend_endpoint_edit_text);
        passwordResetConfirmEndpointEditText = findViewById(R.id.password_reset_confirm_endpoint_edit_text);
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
            loginEndpointEditText.setText(config.loginEndpoint);
            registerEndpointEditText.setText(config.registerEndpoint);
            signupOtpRequestEndpointEditText.setText(config.signupOtpRequestEndpoint);
            signupOtpResendEndpointEditText.setText(config.signupOtpResendEndpoint);
            signupOtpCompleteEndpointEditText.setText(config.signupOtpCompleteEndpoint);
            passwordResetRequestEndpointEditText.setText(config.passwordResetRequestEndpoint);
            passwordResetResendEndpointEditText.setText(config.passwordResetResendEndpoint);
            passwordResetConfirmEndpointEditText.setText(config.passwordResetConfirmEndpoint);
        }
    }

    private void saveSettings() {
        String baseUrl = baseUrlEditText.getText().toString().trim();
        String loginEndpoint = loginEndpointEditText.getText().toString().trim();
        String registerEndpoint = registerEndpointEditText.getText().toString().trim();
        String signupOtpRequestEndpoint = signupOtpRequestEndpointEditText.getText().toString().trim();
        String signupOtpResendEndpoint = signupOtpResendEndpointEditText.getText().toString().trim();
        String signupOtpCompleteEndpoint = signupOtpCompleteEndpointEditText.getText().toString().trim();
        String passwordResetRequestEndpoint = passwordResetRequestEndpointEditText.getText().toString().trim();
        String passwordResetResendEndpoint = passwordResetResendEndpointEditText.getText().toString().trim();
        String passwordResetConfirmEndpoint = passwordResetConfirmEndpointEditText.getText().toString().trim();

        if (baseUrl.isEmpty()
            || loginEndpoint.isEmpty()
            || registerEndpoint.isEmpty()
            || signupOtpRequestEndpoint.isEmpty()
            || signupOtpResendEndpoint.isEmpty()
            || signupOtpCompleteEndpoint.isEmpty()
            || passwordResetRequestEndpoint.isEmpty()
            || passwordResetResendEndpoint.isEmpty()
            || passwordResetConfirmEndpoint.isEmpty()) {
            showError(getString(R.string.invalid_input));
            return;
        }

        saveProgress.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        AppConfig newConfig = new AppConfig();
        newConfig.baseUrl = baseUrl;
        newConfig.loginEndpoint = loginEndpoint;
        newConfig.registerEndpoint = registerEndpoint;
        newConfig.signupOtpRequestEndpoint = signupOtpRequestEndpoint;
        newConfig.signupOtpResendEndpoint = signupOtpResendEndpoint;
        newConfig.signupOtpCompleteEndpoint = signupOtpCompleteEndpoint;
        newConfig.passwordResetRequestEndpoint = passwordResetRequestEndpoint;
        newConfig.passwordResetResendEndpoint = passwordResetResendEndpoint;
        newConfig.passwordResetConfirmEndpoint = passwordResetConfirmEndpoint;

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
