package com.example.myapplication.di;

import com.example.myapplication.data.network.ProfileService;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;
import retrofit2.Retrofit;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {
    @Provides
    @Singleton
    public ProfileService provideProfileService(Retrofit retrofit) {
        return retrofit.create(ProfileService.class);
    }
}
