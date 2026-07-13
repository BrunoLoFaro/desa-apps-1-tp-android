package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;
import com.example.myapplication.ui.auth.viewmodel.ForgotPasswordViewModel;
import com.example.myapplication.util.AuthInputValidator;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ForgotPasswordCodeFragment extends BaseAuthFragment {

    private static final long RESEND_COOLDOWN_SECONDS = 60L;

    private TextInputEditText codeEditText;
    private TextInputLayout codeInputLayout;
    private MaterialButton verifyCodeButton;
    private MaterialButton resendCodeButton;
    private CircularProgressIndicator progressIndicator;
    private MaterialTextView subtitleText;
    private MaterialTextView resendTimerText;
    private MaterialTextView[] otpDigitViews;

    private CountDownTimer resendTimer;
    private long resendSecondsLeft;
    private TextWatcher codeWatcher;
    private boolean otpHasError;
    private boolean isProgrammaticallyClearing;

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
        codeInputLayout = view.findViewById(R.id.forgot_code_input_layout);
        verifyCodeButton = view.findViewById(R.id.forgot_verify_code_button);
        resendCodeButton = view.findViewById(R.id.forgot_resend_code_button);
        progressIndicator = view.findViewById(R.id.forgot_code_progress_indicator);
        subtitleText = view.findViewById(R.id.forgot_code_subtitle);
        resendTimerText = view.findViewById(R.id.forgot_code_resend_timer_text);

        otpDigitViews = new MaterialTextView[]{
                view.findViewById(R.id.forgot_digit_1),
                view.findViewById(R.id.forgot_digit_2),
                view.findViewById(R.id.forgot_digit_3),
                view.findViewById(R.id.forgot_digit_4),
                view.findViewById(R.id.forgot_digit_5),
                view.findViewById(R.id.forgot_digit_6)
        };

        super.onViewCreated(view, savedInstanceState);

        if (subtitleText != null && email != null) {
            subtitleText.setText(getString(R.string.forgot_password_code_subtitle_format, email));
        }

        viewModel = new ViewModelProvider(requireActivity()).get(ForgotPasswordViewModel.class);

        verifyCodeButton.setOnClickListener(v -> verifyCode());
        resendCodeButton.setOnClickListener(v -> resendCode());

        setupOtpInputUx();
        startResendCooldown();

        viewModel.getCodeState().observe(getViewLifecycleOwner(), state -> {
            resendCodeButton.setEnabled(!state.isLoading && resendSecondsLeft <= 0L);
            codeEditText.setEnabled(!state.isLoading);
            progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);
            updateVerifyButtonState(!state.isLoading);

            if (state.error != null) {
                showOtpError(state.error.resolve(requireContext()));
                clearOtpInputPreservingError();
                viewModel.codeErrorConsumed();
            }
            if (state.codeResent) {
                clearOtpInput();
                startResendCooldown();
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

        Toolbar localToolbar = view.findViewById(R.id.local_toolbar);
        if (localToolbar != null) {
            localToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
            localToolbar.setNavigationOnClickListener(v -> navController.navigateUp());
        }

        if (email == null) {
            navController.navigateUp();
        }
    }

    private void verifyCode() {
        String code = codeEditText.getText() != null
                ? codeEditText.getText().toString().trim() : "";
        String codeError = AuthInputValidator.validateOtp(requireContext(), code);
        if (codeError != null) {
            showOtpError(codeError);
            return;
        }
        viewModel.verifyResetCode(email, code);
    }

    private void resendCode() {
        if (resendSecondsLeft > 0L) return;
        viewModel.resendReset(email);
    }

    private void setupOtpInputUx() {
        View.OnClickListener focusInput = v -> {
            codeEditText.requestFocus();
            showKeyboard();
        };
        for (MaterialTextView digitView : otpDigitViews) {
            digitView.setOnClickListener(focusInput);
        }

        codeWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String raw = s == null ? "" : s.toString();
                String digits = raw.replaceAll("[^0-9]", "");
                if (!raw.equals(digits)) {
                    codeEditText.setText(digits);
                    codeEditText.setSelection(digits.length());
                    return;
                }
                if (digits.length() > 6) {
                    String trimmed = digits.substring(0, 6);
                    codeEditText.setText(trimmed);
                    codeEditText.setSelection(trimmed.length());
                    return;
                }
                if (!isProgrammaticallyClearing) {
                    codeInputLayout.setError(null);
                    otpHasError = false;
                }
                updateOtpBoxes(digits);
                updateVerifyButtonState(true);
            }
        };
        codeEditText.addTextChangedListener(codeWatcher);
        verifyCodeButton.setEnabled(false);

        codeEditText.post(() -> {
            codeEditText.requestFocus();
            showKeyboard();
        });
        updateOtpBoxes("");
    }

    private void updateOtpBoxes(String digits) {
        int currentIndex = Math.min(digits.length(), otpDigitViews.length - 1);
        for (int i = 0; i < otpDigitViews.length; i++) {
            MaterialTextView box = otpDigitViews[i];
            box.setText(i < digits.length() ? String.valueOf(digits.charAt(i)) : "");
            boolean active = (i == currentIndex && digits.length() < 6) || (digits.length() == 6 && i == 5);
            if (otpHasError) {
                box.setBackgroundResource(R.drawable.otp_digit_box_error);
            } else {
                box.setBackgroundResource(active ? R.drawable.otp_digit_box_active : R.drawable.otp_digit_box_default);
            }
        }
    }

    private void clearOtpInput() {
        codeEditText.setText("");
        codeInputLayout.setError(null);
        otpHasError = false;
        updateOtpBoxes("");
        verifyCodeButton.setEnabled(false);
    }

    private void clearOtpInputPreservingError() {
        isProgrammaticallyClearing = true;
        codeEditText.setText("");
        isProgrammaticallyClearing = false;
        updateOtpBoxes("");
        updateVerifyButtonState(true);
    }

    private void showOtpError(String message) {
        otpHasError = true;
        codeInputLayout.setError(message);
        String code = codeEditText.getText() == null ? "" : codeEditText.getText().toString();
        updateOtpBoxes(code);
    }

    private void updateVerifyButtonState(boolean notLoading) {
        String code = codeEditText.getText() == null ? "" : codeEditText.getText().toString().trim();
        verifyCodeButton.setEnabled(notLoading && code.length() == 6);
    }

    private void startResendCooldown() {
        if (resendTimer != null) resendTimer.cancel();
        resendSecondsLeft = RESEND_COOLDOWN_SECONDS;
        resendCodeButton.setEnabled(false);
        updateResendTimerText();

        resendTimer = new CountDownTimer(RESEND_COOLDOWN_SECONDS * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                resendSecondsLeft = Math.max(0L, millisUntilFinished / 1000L);
                updateResendTimerText();
                resendCodeButton.setEnabled(false);
            }

            @Override
            public void onFinish() {
                resendSecondsLeft = 0L;
                if (resendTimerText != null) resendTimerText.setText(R.string.otp_resend_available);
                if (resendCodeButton != null) resendCodeButton.setEnabled(true);
            }
        }.start();
    }

    private void updateResendTimerText() {
        if (resendTimerText != null) {
            resendTimerText.setText(getString(R.string.otp_resend_in_seconds, resendSecondsLeft));
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = requireContext().getSystemService(InputMethodManager.class);
        if (imm != null) imm.showSoftInput(codeEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void onDestroyView() {
        if (resendTimer != null) {
            resendTimer.cancel();
            resendTimer = null;
        }
        if (codeEditText != null && codeWatcher != null) {
            codeEditText.removeTextChangedListener(codeWatcher);
        }
        codeEditText = null;
        codeInputLayout = null;
        verifyCodeButton = null;
        resendCodeButton = null;
        progressIndicator = null;
        subtitleText = null;
        resendTimerText = null;
        otpDigitViews = null;
        codeWatcher = null;
        super.onDestroyView();
    }
}