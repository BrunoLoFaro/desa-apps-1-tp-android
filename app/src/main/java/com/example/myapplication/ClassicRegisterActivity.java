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
import com.example.myapplication.data.model.RegisterRequest;
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

public class ClassicRegisterActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText firstNameEditText;
    private TextInputEditText lastNameEditText;
    private TextInputEditText dniEditText;
    private MaterialButton registerButton;
    private CircularProgressIndicator progressIndicator;
    private View coordinator;

    private AppConfig appConfig;
    private AuthService authService;
    private ConfigLoader configLoader;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_classic_register);

        configLoader = new ConfigLoader(this);
        sessionManager = new SessionManager(this);
        coordinator = findViewById(R.id.classic_register_coordinator);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        firstNameEditText = findViewById(R.id.first_name_edit_text);
        lastNameEditText = findViewById(R.id.last_name_edit_text);
        dniEditText = findViewById(R.id.dni_edit_text);
        registerButton = findViewById(R.id.register_button);
        progressIndicator = findViewById(R.id.register_progress_indicator);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(this, toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.classic_register_coordinator), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        registerButton.setOnClickListener(v -> attemptClassicRegister());
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
                registerButton.setEnabled(true);
            } else {
                authService = null;
                registerButton.setEnabled(false);
                showError(getString(R.string.error_config_load));
            }
        } catch (Exception e) {
            authService = null;
            registerButton.setEnabled(false);
            showError(getString(R.string.error_invalid_config));
        }
    }

    private void attemptClassicRegister() {
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
        String firstName = firstNameEditText.getText() != null ? firstNameEditText.getText().toString().trim() : "";
        String lastName = lastNameEditText.getText() != null ? lastNameEditText.getText().toString().trim() : "";
        String dni = dniEditText.getText() != null ? dniEditText.getText().toString().trim() : "";

        String validationError = validateClassicInput(email, password, firstName, lastName, dni);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_register_service_not_initialized));
            return;
        }

        setLoading(true);
        RegisterRequest request = new RegisterRequest(email, password, firstName, lastName, dni);
        authService.register(AuthEndpoints.register(appConfig), request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    handleLoginSuccess(response.body());
                } else {
                    String errorMessage = NetworkErrorParser.getErrorMessage(response, getString(R.string.error_register_failed_default));
                    showError(getString(R.string.register_failed, errorMessage));
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

    private String validateClassicInput(String email, String password, String firstName, String lastName, String dni) {
        String emailError = AuthInputValidator.validateEmail(this, email);
        if (emailError != null) {
            return emailError;
        }

        String passwordError = AuthInputValidator.validatePassword(this, password);
        if (passwordError != null) {
            return passwordError;
        }

        String firstNameError = AuthInputValidator.validateFirstName(this, firstName);
        if (firstNameError != null) {
            return firstNameError;
        }

        String lastNameError = AuthInputValidator.validateLastName(this, lastName);
        if (lastNameError != null) {
            return lastNameError;
        }

        return AuthInputValidator.validateDni(this, dni);
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
        registerButton.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        firstNameEditText.setEnabled(!isLoading);
        lastNameEditText.setEnabled(!isLoading);
        dniEditText.setEnabled(!isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Snackbar.make(coordinator, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.error, getTheme()))
                .setTextColor(getResources().getColor(R.color.onError, getTheme()))
                .show();
    }
}
