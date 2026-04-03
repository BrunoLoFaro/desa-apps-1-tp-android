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

public class ForgotPasswordRequestActivity extends AppCompatActivity {

    public static final String EXTRA_PREFILL_EMAIL = "prefill_email";

    private TextInputEditText emailEditText;
    private MaterialButton sendCodeButton;
    private CircularProgressIndicator progressIndicator;
    private View coordinator;

    private AppConfig appConfig;
    private AuthService authService;
    private ConfigLoader configLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password_request);

        configLoader = new ConfigLoader(this);
        coordinator = findViewById(R.id.forgot_request_coordinator);
        emailEditText = findViewById(R.id.forgot_request_email_edit_text);
        sendCodeButton = findViewById(R.id.forgot_request_send_code_button);
        progressIndicator = findViewById(R.id.forgot_request_progress_indicator);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(this, toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgot_request_coordinator), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String prefillEmail = getIntent().getStringExtra(EXTRA_PREFILL_EMAIL);
        if (prefillEmail != null && !prefillEmail.isEmpty()) {
            emailEditText.setText(prefillEmail);
        }

        sendCodeButton.setOnClickListener(v -> requestCode());
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
                sendCodeButton.setEnabled(true);
            } else {
                authService = null;
                sendCodeButton.setEnabled(false);
                showError(getString(R.string.error_config_load));
            }
        } catch (Exception e) {
            authService = null;
            sendCodeButton.setEnabled(false);
            showError(getString(R.string.error_invalid_config));
        }
    }

    private void requestCode() {
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";

        String emailError = AuthInputValidator.validateEmail(this, email);
        if (emailError != null) {
            showError(emailError);
            return;
        }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        authService.requestPasswordReset(AuthEndpoints.passwordResetRequest(appConfig), new OtpRequest(email)).enqueue(new Callback<OtpResponse>() {
            @Override
            public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    openCodeStep(email);
                } else {
                    String errorMessage = NetworkErrorParser.getErrorMessage(response, getString(R.string.error_password_reset_request_default));
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

    private void openCodeStep(String email) {
        Intent intent = new Intent(this, ForgotPasswordCodeActivity.class);
        intent.putExtra(ForgotPasswordCodeActivity.EXTRA_EMAIL, email);
        startActivity(intent);
    }

    private void setLoading(boolean isLoading) {
        sendCodeButton.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Snackbar.make(coordinator, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.error, getTheme()))
                .setTextColor(getResources().getColor(R.color.onError, getTheme()))
                .show();
    }
}
