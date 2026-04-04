package com.example.myapplication;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.RegisterRequest;
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

public class ClassicRegisterActivity extends BaseAuthActivity {

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText firstNameEditText;
    private TextInputEditText lastNameEditText;
    private TextInputEditText dniEditText;
    private MaterialButton registerButton;
    private CircularProgressIndicator progressIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_classic_register);

        emailEditText     = findViewById(R.id.email_edit_text);
        passwordEditText  = findViewById(R.id.password_edit_text);
        firstNameEditText = findViewById(R.id.first_name_edit_text);
        lastNameEditText  = findViewById(R.id.last_name_edit_text);
        dniEditText       = findViewById(R.id.dni_edit_text);
        registerButton    = findViewById(R.id.register_button);
        progressIndicator = findViewById(R.id.register_progress_indicator);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(this, toolbar);

        // FIX: antes se llamaba findViewById(R.id.classic_register_coordinator) dos veces —
        // una al asignar 'coordinator' y otra al pasar a ViewCompat. Ahora se usa getRootView().
        ViewCompat.setOnApplyWindowInsetsListener(getRootView(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        registerButton.setOnClickListener(v -> attemptClassicRegister());
    }

    @Override
    protected View getRootView() {
        return findViewById(R.id.classic_register_coordinator);
    }

    @Override
    protected void onConfigReady() {
        registerButton.setEnabled(true);
    }

    @Override
    protected void onConfigError(String error) {
        registerButton.setEnabled(false);
        super.onConfigError(error);
    }

    private void attemptClassicRegister() {
        String email     = emailEditText.getText()     != null ? emailEditText.getText().toString().trim() : "";
        String password  = passwordEditText.getText()  != null ? passwordEditText.getText().toString() : "";
        String firstName = firstNameEditText.getText() != null ? firstNameEditText.getText().toString().trim() : "";
        String lastName  = lastNameEditText.getText()  != null ? lastNameEditText.getText().toString().trim() : "";
        String dni       = dniEditText.getText()       != null ? dniEditText.getText().toString().trim() : "";

        String error = validateFields(email, password, firstName, lastName, dni);
        if (error != null) { showError(error); return; }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_register_service_not_initialized));
            return;
        }

        setLoading(true);
        authService.register(AuthEndpoints.register(appConfig), new RegisterRequest(email, password, firstName, lastName, dni))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            handleLoginSuccess(response.body());
                        } else {
                            String msg = NetworkErrorParser.getErrorMessage(
                                    response, getString(R.string.error_register_failed_default));
                            showError(getString(R.string.register_failed, msg));
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

    private String validateFields(String email, String password, String firstName, String lastName, String dni) {
        String err = AuthInputValidator.validateEmail(this, email);
        if (err != null) return err;
        err = AuthInputValidator.validatePassword(this, password);
        if (err != null) return err;
        err = AuthInputValidator.validateFirstName(this, firstName);
        if (err != null) return err;
        err = AuthInputValidator.validateLastName(this, lastName);
        if (err != null) return err;
        return AuthInputValidator.validateDni(this, dni);
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
}
