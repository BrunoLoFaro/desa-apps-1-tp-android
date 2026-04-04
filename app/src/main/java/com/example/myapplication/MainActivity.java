package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;

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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private CircularProgressIndicator progressIndicator;
    private View coordinator;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AppConfig appConfig;
    private AuthService authService;
    private ConfigLoader configLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        configLoader = new ConfigLoader(this);
        coordinator = findViewById(R.id.coordinator);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        progressIndicator = findViewById(R.id.progress_indicator);
        Toolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loginButton.setOnClickListener(v -> attemptLogin());
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_empty_fields));
            return;
        }

        // --- BYPASS LOGIN PARA DESARROLLO ---
        if (BuildConfig.DEBUG && "admin".equals(email) && "admin".equals(password)) {
            LoginResponse bypassResponse = new LoginResponse();
            bypassResponse.token = "fake-dev-token";
            bypassResponse.userId = "dev-user-id";
            handleLoginSuccess(bypassResponse);
            return;
        }
        // ------------------------------------

        if (authService == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);

        executorService.execute(() -> {
            try {
                LoginRequest request = new LoginRequest(email, password);
                Response<LoginResponse> response = authService.login(appConfig.loginEndpoint, request).execute();

                mainHandler.post(() -> {
                    setLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        handleLoginSuccess(response.body());
                    } else {
                        String errorMsg = response.message().isEmpty() ? "Invalid credentials" : response.message();
                        showError(getString(R.string.login_failed, errorMsg));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    showError(getString(R.string.error_network_generic));
                });
            }
        });
    }

    private void handleLoginSuccess(LoginResponse response) {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("TOKEN", response.token);
        intent.putExtra("USER_ID", response.userId);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Snackbar.make(coordinator, message, Snackbar.LENGTH_LONG).show();
    }
}
