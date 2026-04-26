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
        addRule(".*invalid credentials.*|unauthorized|.*credenciales inválidas.*", R.string.error_invalid_credentials);
        addRule(".*token.*expired.*", R.string.error_invalid_config);

        // --- Validation Errors (Generic or Framework specific) ---
        // Matches common Micronaut/Spring validation patterns
        // Catching specific field errors and default messages from Spring
        addRule(".*validation failed.*|.*loginrequestdto.*|.*field 'email'.*|.*well-formed email address.*|.*formato válido.*|.*correo electrónico no tiene.*", R.string.error_invalid_email);

        // --- Connection / Server Errors ---
        addRule(".*internal server error.*", R.string.error_internal_server);
        addRule(".*no route to host.*|.*failed to connect.*", R.string.error_no_connection);
    }

    private void addRule(String regex, int resId) {
        rules.add(new TranslationRule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL), resId));
    }

    /**
     * Translates a server message into a localized string resource ID.
     */
    public int translate(String serverMessage, String url) {
        if (serverMessage == null || serverMessage.trim().isEmpty()) return -1;

        for (TranslationRule rule : rules) {
            if (rule.pattern.matcher(serverMessage).matches()) {
                return rule.resId;
            }
        }

        return -1; // No translation found
    }

    private static class TranslationRule {
        final Pattern pattern;
        final int resId;

        TranslationRule(Pattern pattern, int resId) {
            this.pattern = pattern;
            this.resId = resId;
        }
    }
}
