package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;
import com.example.myapplication.ui.auth.viewmodel.SignupViewModel;
import com.example.myapplication.util.AuthInputValidator;
import com.example.myapplication.util.ToolbarHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class OtpSignupCompleteFragment extends BaseAuthFragment {

    private TextInputEditText passwordEditText;
    private TextInputEditText firstNameEditText;
    private TextInputEditText lastNameEditText;
    private TextInputEditText dniEditText;
    private MaterialButton completeButton;
    private CircularProgressIndicator progressIndicator;

    private String email;
    private String code;
    private String prefillPassword;
    private String prefillFirstName;
    private String prefillLastName;
    private String prefillDni;
    private SignupViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp_signup_complete, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            email = getArguments().getString("email");
            code = getArguments().getString("code");
            prefillPassword = getArguments().getString("password");
            prefillFirstName = getArguments().getString("firstName");
            prefillLastName = getArguments().getString("lastName");
            prefillDni = getArguments().getString("dni");
        }

        passwordEditText = view.findViewById(R.id.otp_signup_complete_password_edit_text);
        firstNameEditText = view.findViewById(R.id.otp_signup_complete_first_name_edit_text);
        lastNameEditText = view.findViewById(R.id.otp_signup_complete_last_name_edit_text);
        dniEditText = view.findViewById(R.id.otp_signup_complete_dni_edit_text);
        completeButton = view.findViewById(R.id.otp_signup_complete_button);
        progressIndicator = view.findViewById(R.id.otp_signup_complete_progress_indicator);

        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(requireActivity(), toolbar);
        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        // Same Activity-scoped instance as SignupFragment and OtpSignupCodeFragment
        viewModel = new ViewModelProvider(requireActivity()).get(SignupViewModel.class);

        completeButton.setOnClickListener(v -> completeRegistration());

        if (prefillPassword != null) passwordEditText.setText(prefillPassword);
        if (prefillFirstName != null) firstNameEditText.setText(prefillFirstName);
        if (prefillLastName != null) lastNameEditText.setText(prefillLastName);
        if (prefillDni != null) dniEditText.setText(prefillDni);

        viewModel.getOtpCompleteState().observe(getViewLifecycleOwner(), state -> {
            completeButton.setEnabled(!state.isLoading);
            passwordEditText.setEnabled(!state.isLoading);
            firstNameEditText.setEnabled(!state.isLoading);
            lastNameEditText.setEnabled(!state.isLoading);
            dniEditText.setEnabled(!state.isLoading);
            progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);

            if (state.error != null) {
                showError(state.error.resolve(requireContext()));
                viewModel.otpCompleteErrorConsumed();
            }
            if (state.navigateToHome) {
                navigateToHome();
                viewModel.otpCompleteNavigationConsumed();
            }
        });

        if (email == null || code == null) {
            navController.navigateUp();
        }
    }

    @Override
    protected void navigateToHome() {
        navController.navigate(R.id.action_otpSignupCompleteFragment_to_homeFragment);
    }

    private void completeRegistration() {
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
        String firstName = firstNameEditText.getText() != null ? firstNameEditText.getText().toString().trim() : "";
        String lastName = lastNameEditText.getText() != null ? lastNameEditText.getText().toString().trim() : "";
        String dni = dniEditText.getText() != null ? dniEditText.getText().toString().trim() : "";

        String error = validateFields(password, firstName, lastName, dni);
        if (error != null) { showError(error); return; }

        viewModel.completeSignupWithOtp(email, code, password, firstName, lastName, dni);
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
    public void onDestroyView() {
        passwordEditText = null;
        firstNameEditText = null;
        lastNameEditText = null;
        dniEditText = null;
        completeButton = null;
        progressIndicator = null;
        super.onDestroyView();
    }
}
