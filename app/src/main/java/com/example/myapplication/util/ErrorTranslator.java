package com.example.myapplication.util;

import com.example.myapplication.R;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Centralized dictionary to translate technical backend error messages
 * into user-friendly Android string resources using regex-based rules.
 */
@Singleton
public class ErrorTranslator {

    private final List<TranslationRule> rules = new ArrayList<>();

    @Inject
    public ErrorTranslator() {
        setupRules();
    }

    private void setupRules() {
        // --- Authentication Errors ---
        addRule(".*invalid credentials.*|unauthorized", R.string.error_invalid_credentials);
        addRule(".*token.*expired.*", R.string.error_invalid_config);

        // --- Validation Errors (Generic or Framework specific) ---
        // Matches common Micronaut/Spring validation patterns
        addRule(".*validation error.*|.*loginrequestdto.*|.*field 'email'.*", R.string.error_invalid_email);

        // --- Connection / Server Errors ---
        addRule(".*internal server error.*", R.string.error_internal_server);
        addRule(".*no route to host.*|.*failed to connect.*", R.string.error_no_connection);
    }

    private void addRule(String regex, int resId) {
        rules.add(new TranslationRule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), resId));
    }

    /**
     * Translates a server message into a localized string resource ID.
     */
    public int translate(String serverMessage, String url) {
        if (serverMessage == null || serverMessage.trim().isEmpty()) return -1;

        // Note: You can also add URL-specific logic here if needed
        // e.g., if (url.contains("/bookings")) { ... }

        String normalizedMsg = serverMessage.trim().toLowerCase();

        if (url.contains("auth/login")) {
            if (normalizedMsg.contains("formato válido") || normalizedMsg.contains("correo electrónico no tiene")) {
                return R.string.error_invalid_email;
            }
            if (normalizedMsg.contains("credenciales inválidas")) {
                return R.string.error_invalid_credentials;
            }
        }

        return -1;
    }
}
