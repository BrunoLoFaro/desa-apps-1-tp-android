package com.example.myapplication.glide;

import android.content.Context;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import java.io.InputStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.example.myapplication.data.session.SessionManager;
import dagger.hilt.android.qualifiers.ApplicationContext;
import okhttp3.OkHttpClient;

@GlideModule
public class CustomGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        // Obtiene el OkHttpClient singleton con el interceptor de token
        OkHttpClient okHttpClient = GlideOkHttpProvider.getClient(context);
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(okHttpClient));
    }
}
