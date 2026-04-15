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

    @Inject
    public NetworkErrorParser(Moshi moshi) {
        this.adapter = moshi.adapter(ApiErrorResponse.class);
    }

    /**
     * Parses an HTTP error response into a UiMessage.
     * Returns a StringMessage with the server's error text when available,
     * or a ResMessage with the fallback resource ID otherwise.
     */
    public UiMessage getErrorMessage(Response<?> response, int fallbackResId) {
        if (response == null) {
            return UiMessage.from(fallbackResId);
        }

        // Si es un error 500 (Internal Server Error), usamos un mensaje amigable
        if (response.code() == 500) {
            return UiMessage.from(R.string.error_internal_server);
        }

        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) {
                String errorJson = errorBody.string();

                if (response.code() >= 400) {
                    Log.e("API_ERROR", "Status: " + response.code() + " | Body: " + errorJson);
                }

                if (errorJson != null && !errorJson.isEmpty()) {
                    ApiErrorResponse apiError = adapter.fromJson(errorJson);
                    if (apiError != null && apiError.message != null && !apiError.message.trim().isEmpty()) {
                        return UiMessage.from(apiError.message.trim());
                    }
                }
            }
        } catch (Exception e) {
            Log.e("NetworkErrorParser", "Error al parsear el cuerpo del error", e);
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
