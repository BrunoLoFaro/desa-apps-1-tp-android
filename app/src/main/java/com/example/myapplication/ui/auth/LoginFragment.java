package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;
import com.example.myapplication.ui.auth.viewmodel.LoginViewModel;
import com.example.myapplication.util.AuthInputValidator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginFragment extends BaseAuthFragment {

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private MaterialButton forgotPasswordButton;
    private MaterialButton signUpButton;
    private CircularProgressIndicator progressIndicator;

    private LoginViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        emailEditText = view.findViewById(R.id.email_edit_text);
        passwordEditText = view.findViewById(R.id.password_edit_text);
        loginButton = view.findViewById(R.id.login_button);
        forgotPasswordButton = view.findViewById(R.id.forgot_password_button);
        signUpButton = view.findViewById(R.id.sign_up_button);
        progressIndicator = view.findViewById(R.id.progress_indicator);

        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        loginButton.setOnClickListener(v -> attemptLogin());
        signUpButton.setOnClickListener(v ->
                navController.navigate(R.id.action_loginFragment_to_signupFragment));
        forgotPasswordButton.setOnClickListener(v ->
                navController.navigate(R.id.action_loginFragment_to_forgotPasswordRequestFragment));

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            loginButton.setEnabled(!state.isLoading);
            forgotPasswordButton.setEnabled(!state.isLoading);
            signUpButton.setEnabled(!state.isLoading);
            emailEditText.setEnabled(!state.isLoading);
            passwordEditText.setEnabled(!state.isLoading);
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

        if (viewModel.hasValidSession()) {
            navigateToHome();
        }
    }

    @Override
    protected void navigateToHome() {
        navController.navigate(R.id.action_loginFragment_to_homeFragment);
    }

    private void attemptLogin() {
        String email = emailEditText.getText() != null
                ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null
                ? passwordEditText.getText().toString() : "";

        String emailError = AuthInputValidator.validateEmail(requireContext(), email);
        if (emailError != null) { showError(emailError); return; }

        String passwordError = AuthInputValidator.validatePassword(requireContext(), password);
        if (passwordError != null) { showError(passwordError); return; }

        viewModel.login(email, password);
    }

    @Override
    public void onDestroyView() {
        emailEditText = null;
        passwordEditText = null;
        loginButton = null;
        forgotPasswordButton = null;
        signUpButton = null;
        progressIndicator = null;
        super.onDestroyView();
    }
}
