package com.example.myapplication.ui.auth;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.ui.auth.viewmodel.LoginViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginFragment extends BaseAuthFragment {

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private MaterialButton forgotPasswordButton;
    private MaterialButton otpLoginButton;
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
        emailLayout = view.findViewById(R.id.email_layout);
        passwordLayout = view.findViewById(R.id.password_layout);
        emailEditText = view.findViewById(R.id.email_edit_text);
        passwordEditText = view.findViewById(R.id.password_edit_text);
        loginButton = view.findViewById(R.id.login_button);
        forgotPasswordButton = view.findViewById(R.id.forgot_password_button);
        otpLoginButton = view.findViewById(R.id.otp_login_button);
        signUpButton = view.findViewById(R.id.sign_up_button);
        progressIndicator = view.findViewById(R.id.progress_indicator);

        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        loginButton.setOnClickListener(v -> attemptLogin());
        forgotPasswordButton.setOnClickListener(v ->
                navController.navigate(R.id.action_loginFragment_to_forgotPasswordRequestFragment));
        otpLoginButton.setOnClickListener(v ->
                navController.navigate(R.id.action_loginFragment_to_signupFragment));
        signUpButton.setOnClickListener(v ->
                navController.navigate(R.id.action_loginFragment_to_classicRegisterFragment));

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean notLoading = !state.isLoading;
            loginButton.setEnabled(notLoading);
            forgotPasswordButton.setEnabled(notLoading);
            otpLoginButton.setEnabled(notLoading);
            signUpButton.setEnabled(notLoading);
            emailEditText.setEnabled(notLoading);
            passwordEditText.setEnabled(notLoading);
            progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);

            if (state.error != null) {
                handleError(state.error);
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

    private void handleError(UiMessage error) {
        if (error instanceof UiMessage.ResMessage) {
            int resId = ((UiMessage.ResMessage) error).resId;
            if (resId == R.string.error_invalid_email) {
                emailLayout.setError(getString(resId));
                return;
            } else if (resId == R.string.error_invalid_credentials) {
                showError(getString(resId));
                return;
            }
        }
        showError(error.resolve(requireContext()));
    }

    @Override
    protected void navigateToHome() {
        navController.navigate(R.id.action_loginFragment_to_homeFragment);
    }

    private void attemptLogin() {
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String email = emailEditText.getText() != null
                ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null
                ? passwordEditText.getText().toString() : "";

        if (email.isEmpty()) {
            emailLayout.setError(getString(R.string.error_empty_fields));
            return;
        }
        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.error_empty_fields));
            return;
        }

        // Cerrar el teclado antes de hacer la llamada para que el Snackbar de error sea visible
        hideKeyboard();
        viewModel.login(email, password);
    }

    private void hideKeyboard() {
        View focused = requireActivity().getCurrentFocus();
        if (focused != null) {
            InputMethodManager imm = (InputMethodManager)
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroyView() {
        emailLayout = null;
        passwordLayout = null;
        emailEditText = null;
        passwordEditText = null;
        loginButton = null;
        forgotPasswordButton = null;
        otpLoginButton = null;
        signUpButton = null;
        progressIndicator = null;
        super.onDestroyView();
    }
}