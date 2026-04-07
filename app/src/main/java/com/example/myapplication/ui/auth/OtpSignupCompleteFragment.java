package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.R;
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

public class OtpSignupCompleteFragment extends BaseAuthFragment {

    private TextInputEditText passwordEditText;
    private TextInputEditText firstNameEditText;
    private TextInputEditText lastNameEditText;
    private TextInputEditText dniEditText;
    private MaterialButton completeButton;
    private CircularProgressIndicator progressIndicator;

    private String email;
    private String code;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp_signup_complete, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            email = getArguments().getString("email");
            code = getArguments().getString("code");
        }

        passwordEditText = view.findViewById(R.id.otp_signup_complete_password_edit_text);
        firstNameEditText = view.findViewById(R.id.otp_signup_complete_first_name_edit_text);
        lastNameEditText = view.findViewById(R.id.otp_signup_complete_last_name_edit_text);
        dniEditText = view.findViewById(R.id.otp_signup_complete_dni_edit_text);
        completeButton = view.findViewById(R.id.otp_signup_complete_button);
        progressIndicator = view.findViewById(R.id.otp_signup_complete_progress_indicator);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(requireActivity(), toolbar);
        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        completeButton.setOnClickListener(v -> completeRegistration());

        super.onViewCreated(view, savedInstanceState);

        if (email == null || code == null) {
            navController.navigateUp();
        }
    }

    @Override
    protected void onConfigReady() {
        if (completeButton != null) completeButton.setEnabled(true);
    }

    private void completeRegistration() {
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
        String firstName = firstNameEditText.getText() != null ? firstNameEditText.getText().toString().trim() : "";
        String lastName = lastNameEditText.getText() != null ? lastNameEditText.getText().toString().trim() : "";
        String dni = dniEditText.getText() != null ? dniEditText.getText().toString().trim() : "";

        String error = validateFields(password, firstName, lastName, dni);
        if (error != null) {
            showError(error);
            return;
        }

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
                        showError(NetworkErrorParser.getFailureMessage(t, getString(R.string.error_network_generic)));
                    }
                });
    }

    private String validateFields(String password, String firstName, String lastName, String dni) {
        String err = AuthInputValidator.validatePassword(requireContext(), password);
        if (err != null) return err;
        err = AuthInputValidator.validateFirstName(requireContext(), firstName);
        if (err != null) return err;
        err = AuthInputValidator.validateLastName(requireContext(), lastName);
        if (err != null) return err;
        return AuthInputValidator.validateDni(requireContext(), dni);
    }

    @Override
    protected void navigateToHome() {
        navController.navigate(R.id.action_otpSignupCompleteFragment_to_homeFragment);
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
