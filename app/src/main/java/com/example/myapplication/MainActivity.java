package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.data.model.LoginRequest;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.util.AuthEndpoints;
import com.example.myapplication.util.AuthInputValidator;
import com.example.myapplication.util.NetworkErrorParser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseAuthActivity {

    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private MaterialButton forgotPasswordButton;
    private MaterialButton signUpButton;
    private CircularProgressIndicator progressIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // sessionManager es inicializado en BaseAuthActivity.onCreate()
        usernameEditText  = findViewById(R.id.username_edit_text);
        passwordEditText  = findViewById(R.id.password_edit_text);
        loginButton       = findViewById(R.id.login_button);
        forgotPasswordButton = findViewById(R.id.forgot_password_button);
        signUpButton      = findViewById(R.id.sign_up_button);
        progressIndicator = findViewById(R.id.progress_indicator);
        Toolbar toolbar   = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        ViewCompat.setOnApplyWindowInsetsListener(getRootView(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loginButton.setOnClickListener(v -> attemptLogin());
        forgotPasswordButton.setOnClickListener(v -> openPasswordResetScreen());
        signUpButton.setOnClickListener(v -> startActivity(new Intent(this, SignupActivity.class)));
    }

    @Override
    protected void onResume() {
        // Si ya hay sesión válida (token no expirado), ir directo a Home sin cargar config.
        // sessionManager se inicializa en BaseAuthActivity.onCreate(), así que siempre es non-null aquí.
        if (sessionManager.hasValidSession()) {
            navigateToHome();
            return;
        }
        super.onResume(); // carga config
    }

    @Override
    protected View getRootView() {
        return findViewById(R.id.coordinator);
    }

    @Override
    protected void onConfigReady() {
        loginButton.setEnabled(true);
        forgotPasswordButton.setEnabled(true);
    }

    @Override
    protected void onConfigError(String error) {
        loginButton.setEnabled(false);
        forgotPasswordButton.setEnabled(false);
        super.onConfigError(error);
    }

    private boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return false;
    }

    private void attemptLogin() {
        String email    = usernameEditText.getText() != null ? usernameEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";

        String emailError = AuthInputValidator.validateEmail(this, email);
        if (emailError != null) { showError(emailError); return; }

        String passwordError = AuthInputValidator.validatePassword(this, password);
        if (passwordError != null) { showError(passwordError); return; }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        authService.login(AuthEndpoints.login(appConfig), new LoginRequest(email, password))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            handleLoginSuccess(response.body());
                        } else {
                            String msg = NetworkErrorParser.getErrorMessage(
                                    response, getString(R.string.error_login_failed_default));
                            showError(getString(R.string.login_failed, msg));
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        setLoading(false);
                        String msg = t != null && t.getLocalizedMessage() != null
                                ? t.getLocalizedMessage() : getString(R.string.error_network_generic);
                        showError(getString(R.string.generic_error, msg));
                    }
                });
    }

    private void openPasswordResetScreen() {
        Intent intent = new Intent(this, ForgotPasswordRequestActivity.class);
        String email = usernameEditText.getText() != null
                ? usernameEditText.getText().toString().trim() : "";
        if (!email.isEmpty()) {
            intent.putExtra(ForgotPasswordRequestActivity.EXTRA_PREFILL_EMAIL, email);
        }
        startActivity(intent);
    }

    private void setLoading(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        forgotPasswordButton.setEnabled(!isLoading);
        signUpButton.setEnabled(!isLoading);
        usernameEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
