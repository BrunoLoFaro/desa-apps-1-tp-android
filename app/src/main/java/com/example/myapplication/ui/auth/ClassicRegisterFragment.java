package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;
import com.example.myapplication.util.AuthInputValidator;
import com.example.myapplication.util.ToolbarHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

public class ClassicRegisterFragment extends BaseAuthFragment {

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText firstNameEditText;
    private TextInputEditText lastNameEditText;
    private TextInputEditText dniEditText;
    private MaterialButton registerButton;
    private CircularProgressIndicator progressIndicator;

    private ClassicRegisterViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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

        viewModel = new ViewModelProvider(this).get(ClassicRegisterViewModel.class);

        registerButton.setOnClickListener(v -> attemptClassicRegister());

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            registerButton.setEnabled(!state.isLoading && state.configValid);
            emailEditText.setEnabled(!state.isLoading);
            passwordEditText.setEnabled(!state.isLoading);
            firstNameEditText.setEnabled(!state.isLoading);
            lastNameEditText.setEnabled(!state.isLoading);
            dniEditText.setEnabled(!state.isLoading);
            progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);

            if (state.error != null) {
                showError(state.error.resolve(requireContext()));
                viewModel.errorConsumed();
            }
            if (state.navigateToHome) {
                navigateToHome();
                viewModel.navigationConsumed();
            }
        });
    }

    @Override
    protected void navigateToHome() {
        navController.navigate(R.id.action_classicRegisterFragment_to_homeFragment);
    }

    private void attemptClassicRegister() {
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
        String firstName = firstNameEditText.getText() != null ? firstNameEditText.getText().toString().trim() : "";
        String lastName = lastNameEditText.getText() != null ? lastNameEditText.getText().toString().trim() : "";
        String dni = dniEditText.getText() != null ? dniEditText.getText().toString().trim() : "";

        String error = validateFields(email, password, firstName, lastName, dni);
        if (error != null) { showError(error); return; }

        viewModel.register(email, password, firstName, lastName, dni);
    }

    private String validateFields(String email, String password, String firstName,
                                  String lastName, String dni) {
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
}
