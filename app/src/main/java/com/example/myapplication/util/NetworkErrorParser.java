package com.example.myapplication.util;

import com.example.myapplication.data.model.ApiErrorResponse;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;

public final class NetworkErrorParser {

    private static final JsonAdapter<ApiErrorResponse> ADAPTER = new Moshi.Builder()
            .build()
            .adapter(ApiErrorResponse.class);

    private NetworkErrorParser() {
    }

    public static String getErrorMessage(Response<?> response, String fallbackMessage) {
        if (response == null) {
            return fallbackMessage;
        }

        ResponseBody errorBody = response.errorBody();
        if (errorBody != null) {
            try {
                ApiErrorResponse apiError = ADAPTER.fromJson(errorBody.string());
                if (apiError != null && apiError.message != null && !apiError.message.trim().isEmpty()) {
                    return apiError.message.trim();
                }
            } catch (IOException ignored) {
            }
        }

        String message = response.message();
        return message == null || message.trim().isEmpty() ? fallbackMessage : message.trim();
    }
}