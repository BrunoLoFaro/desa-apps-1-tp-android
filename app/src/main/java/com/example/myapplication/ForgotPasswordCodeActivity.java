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
import com.example.myapplication.data.model.OtpCodeVerificationRequest;
import com.example.myapplication.data.model.OtpRequest;
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.data.network.AuthService;
import com.example.myapplication.data.network.RetrofitClient;
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

public class ForgotPasswordCodeActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "email";

    private TextInputEditText codeEditText;
    private MaterialButton verifyCodeButton;
    private MaterialButton resendCodeButton;
    private CircularProgressIndicator progressIndicator;
    private View coordinator;

    private String email;
    private AppConfig appConfig;
    private AuthService authService;
    private ConfigLoader configLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password_code);

        configLoader = new ConfigLoader(this);
        coordinator = findViewById(R.id.forgot_code_coordinator);
        codeEditText = findViewById(R.id.forgot_code_edit_text);
        verifyCodeButton = findViewById(R.id.forgot_verify_code_button);
        resendCodeButton = findViewById(R.id.forgot_resend_code_button);
        progressIndicator = findViewById(R.id.forgot_code_progress_indicator);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(this, toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgot_code_coordinator), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email == null || email.trim().isEmpty()) {
            finish();
            return;
        }

        verifyCodeButton.setOnClickListener(v -> verifyCode());
        resendCodeButton.setOnClickListener(v -> resendCode());
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
                verifyCodeButton.setEnabled(true);
                resendCodeButton.setEnabled(true);
            } else {
                authService = null;
                verifyCodeButton.setEnabled(false);
                resendCodeButton.setEnabled(false);
                showError(getString(R.string.error_config_load));
            }
        } catch (Exception e) {
            authService = null;
            verifyCodeButton.setEnabled(false);
            resendCodeButton.setEnabled(false);
            showError(getString(R.string.error_invalid_config));
        }
    }

    private void verifyCode() {
        String code = codeEditText.getText() != null ? codeEditText.getText().toString().trim() : "";
        String codeError = AuthInputValidator.validateOtp(this, code);
        if (codeError != null) {
            showError(codeError);
            return;
        }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        OtpCodeVerificationRequest request = new OtpCodeVerificationRequest(email, code);
        authService.verifyPasswordResetCode(AuthEndpoints.passwordResetVerify(appConfig), request).enqueue(new Callback<OtpResponse>() {
            @Override
            public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    openNewPasswordStep(code);
                } else {
                    String errorMessage = NetworkErrorParser.getErrorMessage(response, getString(R.string.error_password_reset_verify_default));
                    showError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<OtpResponse> call, Throwable t) {
                setLoading(false);
                String message = t != null && t.getLocalizedMessage() != null
                        ? t.getLocalizedMessage()
                        : getString(R.string.error_network_generic);
                showError(getString(R.string.generic_error, message));
            }
        });
    }

    private void resendCode() {
        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        authService.resendPasswordReset(AuthEndpoints.passwordResetResend(appConfig), new OtpRequest(email)).enqueue(new Callback<OtpResponse>() {
            @Override
            public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    showInfo(getString(R.string.password_reset_resent_message));
                } else {
                    String errorMessage = NetworkErrorParser.getErrorMessage(response, getString(R.string.error_password_reset_resend_default));
                    showError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<OtpResponse> call, Throwable t) {
                setLoading(false);
                String message = t != null && t.getLocalizedMessage() != null
                        ? t.getLocalizedMessage()
                        : getString(R.string.error_network_generic);
                showError(getString(R.string.generic_error, message));
            }
        });
    }

    private void openNewPasswordStep(String code) {
        Intent intent = new Intent(this, ForgotPasswordNewPasswordActivity.class);
        intent.putExtra(ForgotPasswordNewPasswordActivity.EXTRA_EMAIL, email);
        intent.putExtra(ForgotPasswordNewPasswordActivity.EXTRA_CODE, code);
        startActivity(intent);
    }

    private void setLoading(boolean isLoading) {
        verifyCodeButton.setEnabled(!isLoading);
        resendCodeButton.setEnabled(!isLoading);
        codeEditText.setEnabled(!isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showInfo(String message) {
        Snackbar.make(coordinator, message, Snackbar.LENGTH_LONG).show();
    }

    private void showError(String message) {
        Snackbar.make(coordinator, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.error, getTheme()))
                .setTextColor(getResources().getColor(R.color.onError, getTheme()))
                .show();
    }
}
