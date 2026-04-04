package com.example.myapplication;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.OtpRegistrationCompleteRequest;
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

public class OtpSignupCompleteActivity extends BaseAuthActivity {

    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_CODE  = "code";

    private TextInputEditText passwordEditText;
    private TextInputEditText firstNameEditText;
    private TextInputEditText lastNameEditText;
    private TextInputEditText dniEditText;
    private MaterialButton completeButton;
    private CircularProgressIndicator progressIndicator;

    private String email;
    private String code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp_signup_complete);

        passwordEditText  = findViewById(R.id.otp_signup_complete_password_edit_text);
        firstNameEditText = findViewById(R.id.otp_signup_complete_first_name_edit_text);
        lastNameEditText  = findViewById(R.id.otp_signup_complete_last_name_edit_text);
        dniEditText       = findViewById(R.id.otp_signup_complete_dni_edit_text);
        completeButton    = findViewById(R.id.otp_signup_complete_button);
        progressIndicator = findViewById(R.id.otp_signup_complete_progress_indicator);

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

        completeButton.setOnClickListener(v -> completeRegistration());
    }

    @Override
    protected View getRootView() {
        return findViewById(R.id.otp_signup_complete_coordinator);
    }

    @Override
    protected void onConfigReady() {
        completeButton.setEnabled(true);
    }

    @Override
    protected void onConfigError(String error) {
        completeButton.setEnabled(false);
        super.onConfigError(error);
    }

    private void completeRegistration() {
        String password  = passwordEditText.getText()  != null ? passwordEditText.getText().toString() : "";
        String firstName = firstNameEditText.getText() != null ? firstNameEditText.getText().toString().trim() : "";
        String lastName  = lastNameEditText.getText()  != null ? lastNameEditText.getText().toString().trim() : "";
        String dni       = dniEditText.getText()       != null ? dniEditText.getText().toString().trim() : "";

        String error = validateFields(password, firstName, lastName, dni);
        if (error != null) { showError(error); return; }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        OtpRegistrationCompleteRequest request =
                new OtpRegistrationCompleteRequest(email, code, password, firstName, lastName, dni);
        authService.completeSignupWithOtp(AuthEndpoints.signupOtpComplete(appConfig), request)
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            handleLoginSuccess(response.body());
                        } else {
                            showError(NetworkErrorParser.getErrorMessage(
                                    response, getString(R.string.error_signup_complete_default)));
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

    private String validateFields(String password, String firstName, String lastName, String dni) {
        String err = AuthInputValidator.validatePassword(this, password);
        if (err != null) return err;
        err = AuthInputValidator.validateFirstName(this, firstName);
        if (err != null) return err;
        err = AuthInputValidator.validateLastName(this, lastName);
        if (err != null) return err;
        return AuthInputValidator.validateDni(this, dni);
    }

    private void setLoading(boolean isLoading) {
        completeButton.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        firstNameEditText.setEnabled(!isLoading);
        lastNameEditText.setEnabled(!isLoading);
        dniEditText.setEnabled(!isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
