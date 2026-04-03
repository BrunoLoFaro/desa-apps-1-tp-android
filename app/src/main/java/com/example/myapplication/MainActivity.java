package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.model.LoginRequest;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.network.AuthService;
import com.example.myapplication.data.network.RetrofitClient;
import com.example.myapplication.data.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private MaterialButton signUpButton;
    private MaterialButton otpLoginButton;
    private CircularProgressIndicator progressIndicator;
    private View coordinator;
    private ActivityResultLauncher<Intent> registerLauncher;

    private AppConfig appConfig;
    private AuthService authService;
    private ConfigLoader configLoader;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        configLoader = new ConfigLoader(this);
        sessionManager = new SessionManager(this);
        coordinator = findViewById(R.id.coordinator);
        usernameEditText = findViewById(R.id.username_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        signUpButton = findViewById(R.id.sign_up_button);
        otpLoginButton = findViewById(R.id.otp_login_button);
        progressIndicator = findViewById(R.id.progress_indicator);
        Toolbar toolbar = findViewById(R.id.toolbar);

        registerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String registeredEmail = result.getData().getStringExtra("registered_email");
                        if (registeredEmail != null && !registeredEmail.isEmpty()) {
                            usernameEditText.setText(registeredEmail);
                        }
                        Snackbar.make(coordinator, R.string.register_success_login_prompt, Snackbar.LENGTH_LONG).show();
                    }
                }
        );

        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loginButton.setOnClickListener(v -> attemptLogin());
        signUpButton.setOnClickListener(v -> openRegisterScreen());
        otpLoginButton.setOnClickListener(v -> openOtpLoginScreen());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.hasValidSession()) {
            openHomeAndClearBackStack();
            return;
        }
        loadConfiguration();
    }

    private boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return false;
    }

    private void loadConfiguration() {
        try {
            appConfig = configLoader.loadConfig();
            if (appConfig != null && appConfig.baseUrl != null && !appConfig.baseUrl.isEmpty()) {
                authService = RetrofitClient.getClient(appConfig).create(AuthService.class);
                loginButton.setEnabled(true);
            } else {
                showError(getString(R.string.error_config_load));
                loginButton.setEnabled(false);
            }
        } catch (IllegalArgumentException e) {
            showError(getString(R.string.error_invalid_config));
            loginButton.setEnabled(false);
            authService = null;
        } catch (Exception e) {
            showError(getString(R.string.error_config_load));
            loginButton.setEnabled(false);
            authService = null;
        }
    }

    private void attemptLogin() {
        String email = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_empty_fields));
            return;
        }

        // --- BYPASS LOGIN PARA DESARROLLO ---
        if (BuildConfig.DEBUG && "admin".equals(email) && "admin".equals(password)) {
            LoginResponse bypassResponse = new LoginResponse();
            bypassResponse.token = "fake-dev-token";
            bypassResponse.userId = 1L;
            handleLoginSuccess(bypassResponse);
            return;
        }
        // ------------------------------------

        if (authService == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);

        LoginRequest request = new LoginRequest(email, password);
        authService.login(appConfig.loginEndpoint, request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    handleLoginSuccess(response.body());
                } else {
                    String errorMsg = response.message().isEmpty() ? "Invalid credentials" : response.message();
                    showError(getString(R.string.login_failed, errorMsg));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                String message = t != null && t.getLocalizedMessage() != null
                        ? t.getLocalizedMessage()
                        : "Network error";
                showError(getString(R.string.generic_error, message));
            }
        });
    }

    private void openRegisterScreen() {
        Intent intent = new Intent(this, RegisterActivity.class);
        registerLauncher.launch(intent);
    }

    private void openOtpLoginScreen() {
        startActivity(new Intent(this, OtpLoginActivity.class));
    }

    private void handleLoginSuccess(LoginResponse response) {
        if (response != null && response.token != null && !response.token.trim().isEmpty()) {
            sessionManager.saveAccessToken(response.token);
        }
        openHomeAndClearBackStack();
    }

    private void openHomeAndClearBackStack() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        signUpButton.setEnabled(!isLoading);
        otpLoginButton.setEnabled(!isLoading);
        usernameEditText.setEnabled(!isLoading);
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
