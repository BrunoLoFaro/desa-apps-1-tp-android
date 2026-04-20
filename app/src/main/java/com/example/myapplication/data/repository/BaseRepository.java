package com.example.myapplication.data.repository;

import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.util.NetworkErrorParser;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import retrofit2.Call;
import retrofit2.Response;

public abstract class BaseRepository {

    protected final NetworkErrorParser errorParser;
    protected final List<Call<?>> activeCalls = new CopyOnWriteArrayList<>();

    protected BaseRepository(NetworkErrorParser errorParser) {
        this.errorParser = errorParser;
    }

    public void cancelAll() {
        for (Call<?> call : activeCalls) {
            if (!call.isCanceled()) call.cancel();
        }
        activeCalls.clear();
    }

    protected <T> void enqueue(Call<T> call, RepositoryCallback<T> callback, int fallbackResId) {
        activeCalls.add(call);
        call.enqueue(new retrofit2.Callback<T>() {
            @Override
            public void onResponse(Call<T> c, Response<T> response) {
                activeCalls.remove(c);
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(errorParser.getErrorMessage(response, fallbackResId));
                }
            }

            @Override
            public void onFailure(Call<T> c, Throwable t) {
                activeCalls.remove(c);
                callback.onError(errorParser.getFailureMessage(t, fallbackResId));
            }
        });
    }
}
