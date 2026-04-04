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

public class OtpSignupCodeActivity extends BaseAuthActivity {

    public static final String EXTRA_EMAIL = "email";

    private TextInputEditText codeEditText;
    private MaterialButton verifyButton;
    private MaterialButton resendButton;
    private CircularProgressIndicator progressIndicator;

    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp_signup_code);

        codeEditText = findViewById(R.id.otp_signup_code_edit_text);
        verifyButton = findViewById(R.id.otp_signup_verify_button);
        resendButton = findViewById(R.id.otp_signup_resend_button);
        progressIndicator = findViewById(R.id.otp_signup_code_progress_indicator);

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

        verifyButton.setOnClickListener(v -> verifyCode());
        resendButton.setOnClickListener(v -> resendCode());
    }

    @Override
    protected View getRootView() {
        return findViewById(R.id.otp_signup_code_coordinator);
    }

    @Override
    protected void onConfigReady() {
        verifyButton.setEnabled(true);
        resendButton.setEnabled(true);
    }

    @Override
    protected void onConfigError(String error) {
        verifyButton.setEnabled(false);
        resendButton.setEnabled(false);
        super.onConfigError(error);
    }

    /**
     * FIX: antes navegaba directamente a pantalla 3 sin verificar el código en el backend.
     * Ahora llama a /signup/otp/verify primero y solo navega si el servidor confirma el código.
     * Esto garantiza que el usuario no llegue a la pantalla de datos con un código incorrecto.
     */
    private void verifyCode() {
        String code = codeEditText.getText() != null ? codeEditText.getText().toString().trim() : "";
        String codeError = AuthInputValidator.validateOtp(this, code);
        if (codeError != null) { showError(codeError); return; }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        authService.verifySignupOtp(
                AuthEndpoints.signupOtpVerify(appConfig),
                new OtpCodeVerificationRequest(email, code))
                .enqueue(new Callback<OtpResponse>() {
                    @Override
                    public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            openCompleteStep(code);
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

    private void openCompleteStep(String verifiedCode) {
        Intent intent = new Intent(this, OtpSignupCompleteActivity.class);
        intent.putExtra(OtpSignupCompleteActivity.EXTRA_EMAIL, email);
        intent.putExtra(OtpSignupCompleteActivity.EXTRA_CODE, verifiedCode);
        startActivity(intent);
    }

    private void resendCode() {
        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        authService.resendSignupOtp(AuthEndpoints.signupOtpResend(appConfig), new OtpRequest(email))
                .enqueue(new Callback<OtpResponse>() {
                    @Override
                    public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            showInfo(getString(R.string.signup_otp_resent_message));
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
        verifyButton.setEnabled(!isLoading);
        resendButton.setEnabled(!isLoading);
        codeEditText.setEnabled(!isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
