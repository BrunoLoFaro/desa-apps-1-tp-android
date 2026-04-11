package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;
import com.example.myapplication.ui.auth.viewmodel.ForgotPasswordViewModel;
import com.example.myapplication.util.AuthInputValidator;
import com.example.myapplication.util.ToolbarHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ForgotPasswordCodeFragment extends BaseAuthFragment {

    private TextInputEditText codeEditText;
    private MaterialButton verifyCodeButton;
    private MaterialButton resendCodeButton;
    private CircularProgressIndicator progressIndicator;

    private String email;
    private ForgotPasswordViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password_code, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            email = getArguments().getString("email");
        }

        codeEditText = view.findViewById(R.id.forgot_code_edit_text);
        verifyCodeButton = view.findViewById(R.id.forgot_verify_code_button);
        resendCodeButton = view.findViewById(R.id.forgot_resend_code_button);
        progressIndicator = view.findViewById(R.id.forgot_code_progress_indicator);

        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(requireActivity(), toolbar);
        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        // Same Activity-scoped instance as ForgotPasswordRequestFragment
        viewModel = new ViewModelProvider(requireActivity()).get(ForgotPasswordViewModel.class);

        verifyCodeButton.setOnClickListener(v -> verifyCode());
        resendCodeButton.setOnClickListener(v -> resendCode());

        viewModel.getCodeState().observe(getViewLifecycleOwner(), state -> {
            verifyCodeButton.setEnabled(!state.isLoading);
            resendCodeButton.setEnabled(!state.isLoading);
            codeEditText.setEnabled(!state.isLoading);
            progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);

            if (state.error != null) {
                showError(state.error.resolve(requireContext()));
                viewModel.codeErrorConsumed();
            }
            if (state.codeResent) {
                showInfo(getString(R.string.password_reset_resent_message));
                viewModel.codeResentConsumed();
            }
            if (state.navigateToNewPassword) {
                String code = codeEditText.getText() != null
                        ? codeEditText.getText().toString().trim() : "";
                Bundle args = new Bundle();
                args.putString("email", email);
                args.putString("code", code);
                navController.navigate(
                        R.id.action_forgotPasswordCodeFragment_to_forgotPasswordNewPasswordFragment, args);
                viewModel.codeNavigationConsumed();
            }
        });

        if (email == null) {
            navController.navigateUp();
        }
    }

    private void verifyCode() {
        String code = codeEditText.getText() != null
                ? codeEditText.getText().toString().trim() : "";
        String codeError = AuthInputValidator.validateOtp(requireContext(), code);
        if (codeError != null) { showError(codeError); return; }
        viewModel.verifyResetCode(email, code);
    }

    private void resendCode() {
        viewModel.resendReset(email);
    }

    @Override
    public void onDestroyView() {
        codeEditText = null;
        verifyCodeButton = null;
        resendCodeButton = null;
        progressIndicator = null;
        super.onDestroyView();
    }
}
