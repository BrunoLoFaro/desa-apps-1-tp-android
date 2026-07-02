package com.example.myapplication.util;

import android.util.Log;
import com.example.myapplication.R;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.ApiErrorResponse;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.ResponseBody;
import retrofit2.Response;

@Singleton
public class NetworkErrorParser {

    private final JsonAdapter<ApiErrorResponse> adapter;
    private final ErrorTranslator errorTranslator;

    @Inject
    public NetworkErrorParser(Moshi moshi, ErrorTranslator errorTranslator) {
        this.adapter = moshi.adapter(ApiErrorResponse.class);
        this.errorTranslator = errorTranslator;
    }

    /**
     * Parses an HTTP error response into a UiMessage.
     */
    public UiMessage getErrorMessage(Response<?> response, int fallbackResId) {
        if (response == null) {
            return UiMessage.from(fallbackResId);
        }

        // 1. Handle by HTTP Status Code first (Global Policy)
        switch (response.code()) {
            case 401:
                return UiMessage.from(R.string.error_invalid_credentials);
            case 403:
                return UiMessage.from(R.string.error_invalid_config);
            case 500:
                return UiMessage.from(R.string.error_internal_server);
        }

        // 2. Try to parse and translate the server's specific message
        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) {
                String errorJson = errorBody.string();

                if (response.code() >= 400) {
                    Log.e("API_ERROR", "Status: " + response.code() + " | Body: " + errorJson);
                }

                if (errorJson != null && !errorJson.isEmpty()) {
                    ApiErrorResponse apiError = adapter.fromJson(errorJson);
                    if (apiError != null) {
                        String serverMsg = apiError.message != null ? apiError.message.trim() : apiError.error;
                        
                        if (serverMsg != null && !serverMsg.isEmpty()) {
                            String url = response.raw().request().url().toString();
                            int translatedResId = errorTranslator.translate(serverMsg, url);

                            if (translatedResId != -1) {
                                return UiMessage.from(translatedResId);
                            }
                            return UiMessage.from(serverMsg);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("NetworkErrorParser", "Error parsing error body", e);
        }

        String message = response.message();
        return (message == null || message.trim().isEmpty())
                ? UiMessage.from(fallbackResId)
                : UiMessage.from(message.trim());
    }

    /**
     * Converts a network failure throwable into a UiMessage.
     */
    public UiMessage getFailureMessage(Throwable t, int fallbackResId) {
        if (t == null) return UiMessage.from(fallbackResId);

        Log.e("API_FAILURE", "Error de red/petición", t);

        if (t instanceof java.net.UnknownHostException || t instanceof java.net.ConnectException) {
            return UiMessage.from(R.string.error_no_connection);
        } else if (t instanceof java.net.SocketTimeoutException) {
            return UiMessage.from(R.string.error_timeout);
        }

        String msg = t.getLocalizedMessage();
        return (msg == null || msg.isEmpty()) ? UiMessage.from(fallbackResId) : UiMessage.from(msg);
    }
}
