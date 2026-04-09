package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.R;
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

public class ClassicRegisterFragment extends BaseAuthFragment {

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText firstNameEditText;
    private TextInputEditText lastNameEditText;
    private TextInputEditText dniEditText;
    private MaterialButton registerButton;
    private CircularProgressIndicator progressIndicator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_classic_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        emailEditText = view.findViewById(R.id.email_edit_text);
        passwordEditText = view.findViewById(R.id.password_edit_text);
        firstNameEditText = view.findViewById(R.id.first_name_edit_text);
        lastNameEditText = view.findViewById(R.id.last_name_edit_text);
        dniEditText = view.findViewById(R.id.dni_edit_text);
        registerButton = view.findViewById(R.id.register_button);
        progressIndicator = view.findViewById(R.id.register_progress_indicator);

        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(requireActivity(), toolbar);
        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        registerButton.setOnClickListener(v -> attemptClassicRegister());
    }

    @Override
    protected void onConfigReady() {
        registerButton.setEnabled(true);
    }

    private void attemptClassicRegister() {
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
        String firstName = firstNameEditText.getText() != null ? firstNameEditText.getText().toString().trim() : "";
        String lastName = lastNameEditText.getText() != null ? lastNameEditText.getText().toString().trim() : "";
        String dni = dniEditText.getText() != null ? dniEditText.getText().toString().trim() : "";

        String error = validateFields(email, password, firstName, lastName, dni);
        if (error != null) {
            showError(error);
            return;
        }

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
                        showError(NetworkErrorParser.getFailureMessage(t, getString(R.string.error_network_generic)));
                    }
                });
    }

    private String validateFields(String email, String password, String firstName, String lastName, String dni) {
        String err = AuthInputValidator.validateEmail(requireContext(), email);
        if (err != null) return err;
        err = AuthInputValidator.validatePassword(requireContext(), password);
        if (err != null) return err;
        err = AuthInputValidator.validateFirstName(requireContext(), firstName);
        if (err != null) return err;
        err = AuthInputValidator.validateLastName(requireContext(), lastName);
        if (err != null) return err;
        return AuthInputValidator.validateDni(requireContext(), dni);
    }

    @Override
    protected void navigateToHome() {
        navController.navigate(R.id.action_classicRegisterFragment_to_homeFragment);
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
