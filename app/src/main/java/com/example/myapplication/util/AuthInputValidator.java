package com.example.myapplication.util;

import android.content.Context;
import android.util.Patterns;

import com.example.myapplication.R;

import java.util.regex.Pattern;

public final class AuthInputValidator {

    // Strong password: at least 8 characters, one uppercase, one lowercase, one number, 
    // and one special character (@#$%^&+=!._-*)
    private static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._\\-*])(?=\\S+$).{8,}$";
    private static final Pattern PATTERN = Pattern.compile(PASSWORD_PATTERN);

    private AuthInputValidator() {
    }

    public static String validateEmail(Context context, String email) {
        if (email == null || email.trim().isEmpty()) {
            return context.getString(R.string.error_email_required);
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return context.getString(R.string.error_invalid_email);
        }
        return null;
    }

    public static String validatePassword(Context context, String password) {
        if (password == null || password.isEmpty()) {
            return context.getString(R.string.error_password_required);
        }
        if (!PATTERN.matcher(password).matches()) {
            return context.getString(R.string.error_invalid_password);
        }
        return null;
    }

    public static String validateFirstName(Context context, String firstName) {
        if (firstName == null || firstName.trim().isEmpty()) {
            return context.getString(R.string.error_first_name_required);
        }
        if (firstName.trim().length() < 2 || firstName.trim().length() > 80) {
            return context.getString(R.string.error_invalid_first_name);
        }
        return null;
    }

    public static String validateLastName(Context context, String lastName) {
        if (lastName == null || lastName.trim().isEmpty()) {
            return context.getString(R.string.error_last_name_required);
        }
        if (lastName.trim().length() < 2 || lastName.trim().length() > 80) {
            return context.getString(R.string.error_invalid_last_name);
        }
        return null;
    }

    public static String validateDni(Context context, String dni) {
        if (dni == null || !dni.trim().matches("^[0-9]{7,10}$")) {
            return context.getString(R.string.error_invalid_dni);
        }
        return null;
    }

    public static String validateOtp(Context context, String code) {
        if (code == null || !code.trim().matches("^\\d{6}$")) {
            return context.getString(R.string.error_invalid_otp);
        }
        return null;
    }
}
