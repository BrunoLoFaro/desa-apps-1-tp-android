package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ClassicRegisterFragment extends BaseAuthFragment {

    private TextInputLayout emailInputLayout;
    private TextInputEditText emailEditText;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText passwordEditText;
    private TextInputLayout firstNameInputLayout;
    private TextInputEditText firstNameEditText;
    private TextInputLayout lastNameInputLayout;
    private TextInputEditText lastNameEditText;
    private TextInputLayout dniInputLayout;
    private TextInputEditText dniEditText;
    private MaterialButton registerButton;
    private CircularProgressIndicator progressIndicator;

    private SignupViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_classic_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        emailInputLayout = view.findViewById(R.id.email_input_layout);
        emailEditText = view.findViewById(R.id.email_edit_text);
        passwordInputLayout = view.findViewById(R.id.password_input_layout);
        passwordEditText = view.findViewById(R.id.password_edit_text);
        firstNameInputLayout = view.findViewById(R.id.first_name_input_layout);
        firstNameEditText = view.findViewById(R.id.first_name_edit_text);
        lastNameInputLayout = view.findViewById(R.id.last_name_input_layout);
        lastNameEditText = view.findViewById(R.id.last_name_edit_text);
        dniInputLayout = view.findViewById(R.id.dni_input_layout);
        dniEditText = view.findViewById(R.id.dni_edit_text);
        registerButton = view.findViewById(R.id.register_button);
        progressIndicator = view.findViewById(R.id.register_progress_indicator);

        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(requireActivity(), toolbar);
        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        // Activity-scoped to reuse the same OTP state used by SignupFragment.
        viewModel = new ViewModelProvider(requireActivity()).get(SignupViewModel.class);

        registerButton.setOnClickListener(v -> attemptClassicRegister());
        setupTextWatchers();

        viewModel.getRequestOtpState().observe(getViewLifecycleOwner(), state -> {
            registerButton.setEnabled(!state.isLoading);
            emailEditText.setEnabled(!state.isLoading);
            passwordEditText.setEnabled(!state.isLoading);
            firstNameEditText.setEnabled(!state.isLoading);
            lastNameEditText.setEnabled(!state.isLoading);
            dniEditText.setEnabled(!state.isLoading);
            progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);

            if (state.isLoading) {
                clearAllErrors();
            }

            if (state.error != null) {
                emailInputLayout.setError(state.error.resolve(requireContext()));
                viewModel.requestOtpErrorConsumed();
            }

            if (state.navigateToOtpCode) {
                Bundle args = new Bundle();
                args.putString("email", safeText(emailEditText));
                args.putString("password", safeText(passwordEditText));
                args.putString("firstName", safeText(firstNameEditText));
                args.putString("lastName", safeText(lastNameEditText));
                args.putString("dni", safeText(dniEditText));
                navController.navigate(R.id.action_classicRegisterFragment_to_otpSignupCodeFragment, args);
                viewModel.requestOtpNavigationConsumed();
            }
        });
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearAllErrors();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        emailEditText.addTextChangedListener(watcher);
        passwordEditText.addTextChangedListener(watcher);
        firstNameEditText.addTextChangedListener(watcher);
        lastNameEditText.addTextChangedListener(watcher);
        dniEditText.addTextChangedListener(watcher);
    }

    private void clearAllErrors() {
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        firstNameInputLayout.setError(null);
        lastNameInputLayout.setError(null);
        dniInputLayout.setError(null);
    }

    private void attemptClassicRegister() {
        String email = safeText(emailEditText);
        String password = safeText(passwordEditText);
        String firstName = safeText(firstNameEditText);
        String lastName = safeText(lastNameEditText);
        String dni = safeText(dniEditText);

        clearAllErrors();
        if (validateFields(email, password, firstName, lastName, dni)) {
            viewModel.requestSignupOtp(email);
        }
    }

    private String safeText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private boolean validateFields(String email, String password, String firstName,
                                   String lastName, String dni) {
        String errEmail = AuthInputValidator.validateEmail(requireContext(), email);
        if (errEmail != null) {
            emailInputLayout.setError(errEmail);
            return false;
        }

        String errPass = AuthInputValidator.validatePassword(requireContext(), password);
        if (errPass != null) {
            passwordInputLayout.setError(errPass);
            return false;
        }

        String errFirst = AuthInputValidator.validateFirstName(requireContext(), firstName);
        if (errFirst != null) {
            firstNameInputLayout.setError(errFirst);
            return false;
        }

        String errLast = AuthInputValidator.validateLastName(requireContext(), lastName);
        if (errLast != null) {
            lastNameInputLayout.setError(errLast);
            return false;
        }

        String errDni = AuthInputValidator.validateDni(requireContext(), dni);
        if (errDni != null) {
            dniInputLayout.setError(errDni);
            return false;
        }

        return true;
    }

    @Override
    public void onDestroyView() {
        emailInputLayout = null;
        emailEditText = null;
        passwordInputLayout = null;
        passwordEditText = null;
        firstNameInputLayout = null;
        firstNameEditText = null;
        lastNameInputLayout = null;
        lastNameEditText = null;
        dniInputLayout = null;
        dniEditText = null;
        registerButton = null;
        progressIndicator = null;
        super.onDestroyView();
    }
}
