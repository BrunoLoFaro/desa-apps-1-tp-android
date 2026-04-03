package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.PasswordResetConfirmRequest;
import com.example.myapplication.data.network.AuthService;
import com.example.myapplication.data.network.RetrofitClient;
import com.example.myapplication.data.session.SessionManager;
import com.example.myapplication.util.AuthEndpoints;
import com.example.myapplication.util.AuthInputValidator;
import com.example.myapplication.util.NetworkErrorParser;
import com.example.myapplication.util.ToolbarHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordNewPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_CODE = "code";

    private TextInputEditText passwordEditText;
    private MaterialButton savePasswordButton;
    private CircularProgressIndicator progressIndicator;
    private View coordinator;

    private String email;
    private String code;
    private AppConfig appConfig;
    private AuthService authService;
    private ConfigLoader configLoader;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password_new_password);

        configLoader = new ConfigLoader(this);
        sessionManager = new SessionManager(this);
        coordinator = findViewById(R.id.forgot_new_password_coordinator);
        passwordEditText = findViewById(R.id.forgot_new_password_edit_text);
        savePasswordButton = findViewById(R.id.forgot_save_password_button);
        progressIndicator = findViewById(R.id.forgot_new_password_progress_indicator);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(this, toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgot_new_password_coordinator), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = getIntent().getStringExtra(EXTRA_EMAIL);
        code = getIntent().getStringExtra(EXTRA_CODE);
        if (email == null || email.trim().isEmpty() || code == null || code.trim().isEmpty()) {
            finish();
            return;
        }

        savePasswordButton.setOnClickListener(v -> confirmNewPassword());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConfiguration();
    }

    private void loadConfiguration() {
        try {
            appConfig = configLoader.loadConfig();
            if (appConfig != null && appConfig.baseUrl != null && !appConfig.baseUrl.isEmpty()) {
                authService = RetrofitClient.getClient(appConfig).create(AuthService.class);
                savePasswordButton.setEnabled(true);
            } else {
                authService = null;
                savePasswordButton.setEnabled(false);
                showError(getString(R.string.error_config_load));
            }
        } catch (Exception e) {
            authService = null;
            savePasswordButton.setEnabled(false);
            showError(getString(R.string.error_invalid_config));
        }
    }

    private void confirmNewPassword() {
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
        String passwordError = AuthInputValidator.validatePassword(this, password);
        if (passwordError != null) {
            showError(passwordError);
            return;
        }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(email, code, password);
        authService.confirmPasswordReset(AuthEndpoints.passwordResetConfirm(appConfig), request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    handleLoginSuccess(response.body());
                } else {
                    String errorMessage = NetworkErrorParser.getErrorMessage(response, getString(R.string.error_password_reset_confirm_default));
                    showError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                String message = t != null && t.getLocalizedMessage() != null
                        ? t.getLocalizedMessage()
                        : getString(R.string.error_network_generic);
                showError(getString(R.string.generic_error, message));
            }
        });
    }

    private void handleLoginSuccess(LoginResponse response) {
        if (response != null && response.token != null && !response.token.trim().isEmpty()) {
            sessionManager.saveAccessToken(response.token);
        }
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        savePasswordButton.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Snackbar.make(coordinator, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.error, getTheme()))
                .setTextColor(getResources().getColor(R.color.onError, getTheme()))
                .show();
    }
}
