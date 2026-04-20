package com.example.myapplication.util;

import com.example.myapplication.R;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Centralized dictionary to translate technical backend error messages
 * into user-friendly Android string resources.
 */
@Singleton
public class ErrorTranslator {

    @Inject
    public ErrorTranslator() {
    }

    /**
     * Translates a server message into a string resource ID.
     * @param serverMessage The raw message from the API.
     * @param url The endpoint URL to provide context.
     * @return The resource ID of the localized message, or -1 if no mapping exists.
     */
    public int translate(String serverMessage, String url) {
        if (serverMessage == null || url == null) return -1;

        String normalizedMsg = serverMessage.toLowerCase().trim();

        // Specific mapping for Login errors
        if (url.contains("auth/login")) {
            // Validation errors (DTO)
            if (normalizedMsg.contains("loginrequestdto") && normalizedMsg.contains("field 'email'")) {
                return R.string.error_invalid_email;
            }
            
            // Authentication errors
            if (normalizedMsg.contains("invalid credentials")) {
                return R.string.error_invalid_credentials;
            }
        }

        // Generic Micronaut/Spring validation fallback
        if (normalizedMsg.contains("validation error for argument [0]")) {
            return R.string.error_invalid_email;
        }

        return -1;
    }
}
