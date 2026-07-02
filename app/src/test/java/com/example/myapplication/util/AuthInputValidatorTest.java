package com.example.myapplication.util;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;

import com.example.myapplication.R;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AuthInputValidatorTest {

    @Mock
    private Context context;

    private static final String ERROR_INVALID_PASSWORD = "Invalid Password";
    private static final String ERROR_PASSWORD_REQUIRED = "Password Required";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(context.getString(R.string.error_invalid_password)).thenReturn(ERROR_INVALID_PASSWORD);
        when(context.getString(R.string.error_password_required)).thenReturn(ERROR_PASSWORD_REQUIRED);
    }

    @Test
    public void validatePassword_validPassword_returnsNull() {
        assertNull(AuthInputValidator.validatePassword(context, "Strong123!"));
        assertNull(AuthInputValidator.validatePassword(context, "Abcdef1@"));
    }

    @Test
    public void validatePassword_tooShort_returnsError() {
        assertEquals(ERROR_INVALID_PASSWORD, AuthInputValidator.validatePassword(context, "Short1!"));
    }

    @Test
    public void validatePassword_noUppercase_returnsError() {
        assertEquals(ERROR_INVALID_PASSWORD, AuthInputValidator.validatePassword(context, "weakpassword1!"));
    }

    @Test
    public void validatePassword_noLowercase_returnsError() {
        assertEquals(ERROR_INVALID_PASSWORD, AuthInputValidator.validatePassword(context, "WEAKPASSWORD1!"));
    }

    @Test
    public void validatePassword_noNumber_returnsError() {
        assertEquals(ERROR_INVALID_PASSWORD, AuthInputValidator.validatePassword(context, "NoNumber!"));
    }

    @Test
    public void validatePassword_noSpecialChar_returnsError() {
        assertEquals(ERROR_INVALID_PASSWORD, AuthInputValidator.validatePassword(context, "NoSpecialChar1"));
    }

    @Test
    public void validatePassword_null_returnsError() {
        assertEquals(ERROR_PASSWORD_REQUIRED, AuthInputValidator.validatePassword(context, null));
    }

    @Test
    public void validatePassword_empty_returnsError() {
        assertEquals(ERROR_PASSWORD_REQUIRED, AuthInputValidator.validatePassword(context, ""));
    }

    @Test
    public void validatePassword_withSpaces_returnsError() {
        // Regex includes (?=\S+$) to disallow spaces
        assertEquals(ERROR_INVALID_PASSWORD, AuthInputValidator.validatePassword(context, "Strong 123!"));
    }
}
