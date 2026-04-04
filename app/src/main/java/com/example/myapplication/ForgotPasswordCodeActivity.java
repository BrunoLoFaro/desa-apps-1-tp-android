package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.data.model.OtpCodeVerificationRequest;
import com.example.myapplication.data.model.OtpRequest;
import com.example.myapplication.data.model.OtpResponse;
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

public class ForgotPasswordCodeActivity extends BaseAuthActivity {

    public static final String EXTRA_EMAIL = "email";

    private TextInputEditText codeEditText;
    private MaterialButton verifyCodeButton;
    private MaterialButton resendCodeButton;
    private CircularProgressIndicator progressIndicator;

    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password_code);

        codeEditText = findViewById(R.id.forgot_code_edit_text);
        verifyCodeButton = findViewById(R.id.forgot_verify_code_button);
        resendCodeButton = findViewById(R.id.forgot_resend_code_button);
        progressIndicator = findViewById(R.id.forgot_code_progress_indicator);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(this, toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(getRootView(), (v, insets) -> {
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
    protected View getRootView() {
        return findViewById(R.id.forgot_code_coordinator);
    }

    @Override
    protected void onConfigReady() {
        verifyCodeButton.setEnabled(true);
        resendCodeButton.setEnabled(true);
    }

    @Override
    protected void onConfigError(String error) {
        verifyCodeButton.setEnabled(false);
        resendCodeButton.setEnabled(false);
        super.onConfigError(error);
    }

    private void verifyCode() {
        String code = codeEditText.getText() != null
                ? codeEditText.getText().toString().trim() : "";
        String codeError = AuthInputValidator.validateOtp(this, code);
        if (codeError != null) { showError(codeError); return; }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        authService.verifyPasswordResetCode(
                AuthEndpoints.passwordResetVerify(appConfig),
                new OtpCodeVerificationRequest(email, code))
                .enqueue(new Callback<OtpResponse>() {
                    @Override
                    public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            Intent intent = new Intent(ForgotPasswordCodeActivity.this,
                                    ForgotPasswordNewPasswordActivity.class);
                            intent.putExtra(ForgotPasswordNewPasswordActivity.EXTRA_EMAIL, email);
                            intent.putExtra(ForgotPasswordNewPasswordActivity.EXTRA_CODE, code);
                            startActivity(intent);
                        } else {
                            showError(NetworkErrorParser.getErrorMessage(
                                    response, getString(R.string.error_verify_code_default)));
                        }
                    }

                    @Override
                    public void onFailure(Call<OtpResponse> call, Throwable t) {
                        setLoading(false);
                        String msg = t != null && t.getLocalizedMessage() != null
                                ? t.getLocalizedMessage() : getString(R.string.error_network_generic);
                        showError(getString(R.string.generic_error, msg));
                    }
                });
    }

    private void resendCode() {
        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        authService.resendPasswordReset(AuthEndpoints.passwordResetResend(appConfig), new OtpRequest(email))
                .enqueue(new Callback<OtpResponse>() {
                    @Override
                    public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            showInfo(getString(R.string.password_reset_resent_message));
                        } else {
                            showError(NetworkErrorParser.getErrorMessage(
                                    response, getString(R.string.error_resend_code_default)));
                        }
                    }

                    @Override
                    public void onFailure(Call<OtpResponse> call, Throwable t) {
                        setLoading(false);
                        String msg = t != null && t.getLocalizedMessage() != null
                                ? t.getLocalizedMessage() : getString(R.string.error_network_generic);
                        showError(getString(R.string.generic_error, msg));
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        verifyCodeButton.setEnabled(!isLoading);
        resendCodeButton.setEnabled(!isLoading);
        codeEditText.setEnabled(!isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
