package com.example.myapplication.di;

import android.util.Log;
import com.example.myapplication.BuildConfig;
import com.example.myapplication.data.session.SessionManager;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    private static final String TAG = "OkHttpClient";

    /**
     * Provides a singleton OkHttpClient shared across all network calls.
     * The auth interceptor uses the injected SessionManager (true singleton),
     * so the same token state is used everywhere — no duplicate instances.
     */
    @Provides
    @Singleton
    static OkHttpClient provideOkHttpClient(SessionManager sessionManager) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // 1. HTTP Logging Interceptor
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                message -> Log.d(TAG, "HTTP: " + message)
        );
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);
        builder.addInterceptor(logging);

        // 2. Authentication Interceptor — attaches JWT from the singleton SessionManager
        builder.addInterceptor(chain -> {
            Request original = chain.request();
            String token = sessionManager.getAccessToken();
            if (token != null && !token.isEmpty()) {
                Log.d(TAG, "Adjuntando token JWT al header Authorization");
                return chain.proceed(original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .build());
            }
            return chain.proceed(original);
        });

        // 3. Request/Response timing inspector (debug only)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(chain -> {
                long start = System.currentTimeMillis();
                Request request = chain.request();
                Log.d(TAG, "→ Enviando: " + request.method() + " " + request.url());
                Response response = chain.proceed(request);
                Log.d(TAG, "← Respuesta: " + response.code() + " en "
                        + (System.currentTimeMillis() - start) + "ms");
                return response;
            });
        }

        return builder.build();
    }
}
