package com.example.myapplication.di;

import android.content.Context;
import android.util.Log;
import androidx.room.Room;
import com.example.myapplication.BuildConfig;
import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.CachedActivityDao;
import com.example.myapplication.data.local.OfflineBookingDao;
import com.example.myapplication.data.network.ActivityService;
import com.example.myapplication.data.network.AuthService;
import com.example.myapplication.data.network.BookingService;
import com.example.myapplication.data.network.CatalogMetaService;
import com.example.myapplication.data.network.NewsService;
import com.example.myapplication.data.network.ProfileService;
import com.example.myapplication.data.network.ReviewService;
import com.example.myapplication.data.session.SessionManager;
import com.example.myapplication.util.NetworkMonitor;
import dagger.hilt.android.qualifiers.ApplicationContext;
import com.squareup.moshi.Moshi;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

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
    public static OkHttpClient provideOkHttpClient(SessionManager sessionManager, ConfigLoader configLoader) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        // 1. HTTP Logging Interceptor
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                message -> Log.d(TAG, "HTTP: " + message)
        );
        // BODY para debug de favorites (ver raw JSON). Revertir a BASIC si hay EOFException en multipart.
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

        // 3. Authenticator — maneja 401: intenta refresh token, luego reintenta la request original.
        //    Si el refresh falla, o ya se reintentó una vez, fuerza logout.
        builder.authenticator((route, response) -> {
            // Si la request que falló ya tenía el header X-Auth-Retried, ya reintentamos → logout.
            if (response.request().header("X-Auth-Retried") != null) {
                Log.d(TAG, "401 tras retry — refresh token inválido. Forzando logout.");
                sessionManager.triggerForceLogout();
                return null;
            }

            String refreshToken = sessionManager.getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                if (sessionManager.getAccessToken() != null) {
                    Log.d(TAG, "401 sin refresh token disponible. Forzando logout.");
                    sessionManager.triggerForceLogout();
                }
                return null;
            }

            // Llamada sincrónica al endpoint de refresh usando un OkHttpClient sin interceptores
            // para evitar dependencia circular y loops.
            try {
                AppConfig config = configLoader.loadConfig();
                if (config == null || config.baseUrl == null || config.refreshEndpoint == null) {
                    sessionManager.triggerForceLogout();
                    return null;
                }
                String refreshUrl = config.baseUrl + config.refreshEndpoint;
                String jsonBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
                RequestBody body = RequestBody.create(
                        jsonBody.getBytes(), MediaType.parse("application/json"));
                Request refreshRequest = new Request.Builder()
                        .url(refreshUrl)
                        .post(body)
                        .build();

                OkHttpClient plainClient = new OkHttpClient();
                try (okhttp3.Response refreshResponse = plainClient.newCall(refreshRequest).execute()) {
                    if (!refreshResponse.isSuccessful() || refreshResponse.body() == null) {
                        Log.d(TAG, "Refresh fallido (" + refreshResponse.code() + "). Forzando logout.");
                        sessionManager.triggerForceLogout();
                        return null;
                    }
                    String responseBody = refreshResponse.body().string();

                    Matcher tokenMatcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"")
                            .matcher(responseBody);
                    Matcher refreshMatcher = Pattern.compile("\"refreshToken\"\\s*:\\s*\"([^\"]+)\"")
                            .matcher(responseBody);
                    if (!tokenMatcher.find()) {
                        sessionManager.triggerForceLogout();
                        return null;
                    }
                    String newAccessToken = tokenMatcher.group(1);
                    String newRefreshToken = refreshMatcher.find() ? refreshMatcher.group(1) : refreshToken;

                    sessionManager.saveTokens(newAccessToken, newRefreshToken);
                    Log.d(TAG, "Token refrescado exitosamente. Reintentando request original.");

                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + newAccessToken)
                            .header("X-Auth-Retried", "1")
                            .build();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al refrescar token", e);
                sessionManager.triggerForceLogout();
                return null;
            }
        });

        // 4. Request/Response timing inspector (debug only)
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

    @Provides
    @Singleton
    static Moshi provideMoshi() {
        return new Moshi.Builder().build();
    }

    @Provides
    @Singleton
    static Retrofit provideRetrofit(OkHttpClient okHttpClient, Moshi moshi, ConfigLoader configLoader) {
        AppConfig config = configLoader.loadConfig();
        if (config == null || !config.hasValidBaseUrl()) {
            throw new IllegalStateException(
                    "Config inválida: verificar que config.json tenga un base_url válido");
        }
        return new Retrofit.Builder()
                .baseUrl(config.baseUrl)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(okHttpClient)
                .build();
    }

    @Provides
    @Singleton
    static AuthService provideAuthService(Retrofit retrofit) {
        return retrofit.create(AuthService.class);
    }

    @Provides
    @Singleton
    static ActivityService provideActivityService(Retrofit retrofit) {
        return retrofit.create(ActivityService.class);
    }

    @Provides
    @Singleton
    static CatalogMetaService provideCatalogMetaService(Retrofit retrofit) {
        return retrofit.create(CatalogMetaService.class);
    }

    @Provides
    @Singleton
    static BookingService provideBookingService(Retrofit retrofit) {
        return retrofit.create(BookingService.class);
    }

    @Provides
    @Singleton
    static ProfileService provideProfileService(Retrofit retrofit) {
        return retrofit.create(ProfileService.class);
    }

    @Provides
    @Singleton
    static ReviewService provideReviewService(Retrofit retrofit) {
        return retrofit.create(ReviewService.class);
    }

    @Provides
    @Singleton
    static NewsService provideNewsService(Retrofit retrofit) {
        return retrofit.create(NewsService.class);
    }

    @Provides
    @Singleton
    static AppDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, "xplorenow_db")
                .addMigrations(AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    @Singleton
    static OfflineBookingDao provideOfflineBookingDao(AppDatabase db) {
        return db.offlineBookingDao();
    }

    @Provides
    @Singleton
    static CachedActivityDao provideCachedActivityDao(AppDatabase db) {
        return db.cachedActivityDao();
    }

    @Provides
    @Singleton
    static NetworkMonitor provideNetworkMonitor(@ApplicationContext Context context) {
        return new NetworkMonitor(context);
    }
}
