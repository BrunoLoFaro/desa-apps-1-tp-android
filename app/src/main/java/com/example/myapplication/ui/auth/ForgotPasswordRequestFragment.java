package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;
import com.example.myapplication.ui.auth.viewmodel.ForgotPasswordViewModel;
import com.example.myapplication.util.AuthInputValidator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ForgotPasswordRequestFragment extends BaseAuthFragment {

    private TextInputLayout emailInputLayout;
    private TextInputEditText emailEditText;
    private MaterialButton sendCodeButton;
    private CircularProgressIndicator progressIndicator;

    private ForgotPasswordViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        emailInputLayout = view.findViewById(R.id.forgot_request_email_layout);
        emailEditText = view.findViewById(R.id.forgot_request_email_edit_text);
        sendCodeButton = view.findViewById(R.id.forgot_request_send_code_button);
        progressIndicator = view.findViewById(R.id.forgot_request_progress_indicator);

        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String prefill = getArguments().getString("prefill_email");
            if (prefill != null && !prefill.isEmpty()) {
                emailEditText.setText(prefill);
            }
        }

        viewModel = new ViewModelProvider(requireActivity()).get(ForgotPasswordViewModel.class);

        sendCodeButton.setOnClickListener(v -> requestCode());

        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                emailInputLayout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        viewModel.getRequestState().observe(getViewLifecycleOwner(), state -> {
            sendCodeButton.setEnabled(!state.isLoading);
            emailEditText.setEnabled(!state.isLoading);
            progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);

            if (state.error != null) {
                emailInputLayout.setError(state.error.resolve(requireContext()));
                viewModel.requestErrorConsumed();
            }
            if (state.navigateToCode) {
                String email = emailEditText.getText() != null
                        ? emailEditText.getText().toString().trim() : "";
                Bundle args = new Bundle();
                args.putString("email", email);
                navController.navigate(
                        R.id.action_forgotPasswordRequestFragment_to_forgotPasswordCodeFragment, args);
                viewModel.requestNavigationConsumed();
            }
        });

        Toolbar localToolbar = view.findViewById(R.id.local_toolbar);
        if (localToolbar != null) {
            localToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
            localToolbar.setNavigationOnClickListener(v -> navController.popBackStack(R.id.loginFragment, false));
        }
    }

    private void requestCode() {
        String email = emailEditText.getText() != null
                ? emailEditText.getText().toString().trim() : "";
        String emailError = AuthInputValidator.validateEmail(requireContext(), email);
        if (emailError != null) {
            emailInputLayout.setError(emailError);
            return;
        }
        emailInputLayout.setError(null);
        viewModel.requestReset(email);
    }

    @Override
    public void onDestroyView() {
        emailInputLayout = null;
        emailEditText = null;
        sendCodeButton = null;
        progressIndicator = null;
        super.onDestroyView();
    }
}