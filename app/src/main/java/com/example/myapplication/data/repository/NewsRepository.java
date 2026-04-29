package com.example.myapplication.data.repository;

import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.model.NewsDetailResponse;
import com.example.myapplication.data.model.NewsItem;
import com.example.myapplication.data.model.NewsPageResponse;
import com.example.myapplication.data.network.NewsService;
import com.example.myapplication.util.NetworkErrorParser;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Response;

@Singleton
public class NewsRepository extends BaseRepository {

    private final NewsService newsService;
    private final ConfigLoader configLoader;

    @Inject
    public NewsRepository(NewsService newsService, ConfigLoader configLoader,
                          NetworkErrorParser errorParser) {
        super(errorParser);
        this.newsService = newsService;
        this.configLoader = configLoader;
    }

    public void getNewsList(Integer page, Integer size, RepositoryCallback<List<NewsItem>> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        
        String url = config.baseUrl + config.newsEndpoint;
        enqueueNewsPage(newsService.listNews(url, page, size), callback, R.string.error_network_generic);
    }

    public void getNewsDetail(Long newsId, RepositoryCallback<NewsDetailResponse> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        
        String url = config.baseUrl + config.newsEndpoint + "/" + newsId;
        enqueueNewsDetail(newsService.getNewsDetail(url), callback, R.string.error_network_generic);
    }

    private void enqueueNewsPage(Call<NewsPageResponse> call,
                                 RepositoryCallback<List<NewsItem>> callback,
                                 int fallbackErrorResId) {
        activeCalls.add(call);
        call.enqueue(new retrofit2.Callback<NewsPageResponse>() {
            @Override
            public void onResponse(Call<NewsPageResponse> c, Response<NewsPageResponse> response) {
                activeCalls.remove(c);
                if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                    callback.onSuccess(response.body().items);
                } else {
                    callback.onError(errorParser.getErrorMessage(response, fallbackErrorResId));
                }
            }

            @Override
            public void onFailure(Call<NewsPageResponse> c, Throwable t) {
                activeCalls.remove(c);
                callback.onError(errorParser.getFailureMessage(t, R.string.error_network_generic));
            }
        });
    }

    private void enqueueNewsDetail(Call<NewsDetailResponse> call,
                                    RepositoryCallback<NewsDetailResponse> callback,
                                    int fallbackErrorResId) {
        activeCalls.add(call);
        call.enqueue(new retrofit2.Callback<NewsDetailResponse>() {
            @Override
            public void onResponse(Call<NewsDetailResponse> c, Response<NewsDetailResponse> response) {
                activeCalls.remove(c);
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(errorParser.getErrorMessage(response, fallbackErrorResId));
                }
            }

            @Override
            public void onFailure(Call<NewsDetailResponse> c, Throwable t) {
                activeCalls.remove(c);
                callback.onError(errorParser.getFailureMessage(t, R.string.error_network_generic));
            }
        });
    }

    private <T> AppConfig getConfig(RepositoryCallback<T> callback) {
        AppConfig config = configLoader.loadConfig();
        if (config == null || !config.hasValidBaseUrl()) {
            callback.onError(UiMessage.from(R.string.error_invalid_config));
            return null;
        }
        return config;
    }
}
