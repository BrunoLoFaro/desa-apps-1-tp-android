package com.example.myapplication.data.common;

import android.content.Context;
import androidx.annotation.StringRes;

/**
 * Represents a UI message that can be either a raw string (e.g., from the server)
 * or a string resource ID. This allows ViewModels to expose error/info messages
 * without referencing Android Context or calling getString().
 * The UI layer resolves the actual string via resolve(context).
 */
public abstract class UiMessage {

    private UiMessage() {
    }

    /** Resolves the message to a display string using the given context. */
    public abstract String resolve(Context context);

    /** A message backed by a plain string (e.g., a server-provided error). */
    public static final class StringMessage extends UiMessage {
        public final String value;

        public StringMessage(String value) {
            this.value = value;
        }

        @Override
        public String resolve(Context context) {
            return value;
        }
    }

    /** A message backed by a string resource ID (localized at the UI layer). */
    public static final class ResMessage extends UiMessage {
        @StringRes
        public final int resId;

        public ResMessage(@StringRes int resId) {
            this.resId = resId;
        }

        @Override
        public String resolve(Context context) {
            return context.getString(resId);
        }
    }

    public static UiMessage from(String value) {
        return new StringMessage(value);
    }

    public static UiMessage from(@StringRes int resId) {
        return new ResMessage(resId);
    }
}
