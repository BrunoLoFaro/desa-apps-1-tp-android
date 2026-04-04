package com.example.myapplication;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.PasswordResetConfirmRequest;
import com.example.myapplication.util.AuthEndpoints;
import com.example.myapplication.util.AuthInputValidator;
import com.example.myapplication.util.NetworkErrorParser;
import com.example.myapplication.util.ToolbarHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordNewPasswordActivity extends BaseAuthActivity {

    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_CODE  = "code";

    private TextInputEditText passwordEditText;
    private MaterialButton savePasswordButton;
    private CircularProgressIndicator progressIndicator;

    private String email;
    private String code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password_new_password);

        passwordEditText  = findViewById(R.id.forgot_new_password_edit_text);
        savePasswordButton = findViewById(R.id.forgot_save_password_button);
        progressIndicator = findViewById(R.id.forgot_new_password_progress_indicator);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(this, toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(getRootView(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = getIntent().getStringExtra(EXTRA_EMAIL);
        code  = getIntent().getStringExtra(EXTRA_CODE);
        if (email == null || email.trim().isEmpty() || code == null || code.trim().isEmpty()) {
            finish();
            return;
        }

        savePasswordButton.setOnClickListener(v -> confirmNewPassword());
    }

    @Override
    protected View getRootView() {
        return findViewById(R.id.forgot_new_password_coordinator);
    }

    @Override
    protected void onConfigReady() {
        savePasswordButton.setEnabled(true);
    }

    @Override
    protected void onConfigError(String error) {
        savePasswordButton.setEnabled(false);
        super.onConfigError(error);
    }

    private void confirmNewPassword() {
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
        String passwordError = AuthInputValidator.validatePassword(this, password);
        if (passwordError != null) { showError(passwordError); return; }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        authService.confirmPasswordReset(
                AuthEndpoints.passwordResetConfirm(appConfig),
                new PasswordResetConfirmRequest(email, code, password))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            handleLoginSuccess(response.body());
                        } else {
                            showError(NetworkErrorParser.getErrorMessage(
                                    response, getString(R.string.error_password_reset_confirm_default)));
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

    private void setLoading(boolean isLoading) {
        savePasswordButton.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
