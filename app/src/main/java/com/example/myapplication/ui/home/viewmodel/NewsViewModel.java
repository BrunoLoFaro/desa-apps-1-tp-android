package com.example.myapplication.ui.home.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.NewsDetailResponse;
import com.example.myapplication.data.model.NewsItem;
import com.example.myapplication.data.repository.NewsRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public class NewsViewModel extends ViewModel {

    private final NewsRepository newsRepository;

    private final MutableLiveData<List<NewsItem>> _newsList = new MutableLiveData<>();
    private final MutableLiveData<NewsDetailResponse> _newsDetail = new MutableLiveData<>();
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);

    @Inject
    public NewsViewModel(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    public LiveData<List<NewsItem>> getNewsList() { return _newsList; }
    public LiveData<NewsDetailResponse> getNewsDetail() { return _newsDetail; }
    public LiveData<UiMessage> getError() { return _error; }
    public LiveData<Boolean> isLoading() { return _loading; }

    public void loadNews(Integer page, Integer size) {
        _loading.setValue(true);
        newsRepository.getNewsList(page, size, new RepositoryCallback<List<NewsItem>>() {
            @Override
            public void onSuccess(List<NewsItem> data) {
                _newsList.setValue(data);
                _loading.setValue(false);
            }

            @Override
            public void onError(UiMessage error) {
                _error.setValue(error);
                _loading.setValue(false);
            }
        });
    }

    public void loadNewsDetail(Long newsId) {
        _loading.setValue(true);
        newsRepository.getNewsDetail(newsId, new RepositoryCallback<NewsDetailResponse>() {
            @Override
            public void onSuccess(NewsDetailResponse data) {
                _newsDetail.setValue(data);
                _loading.setValue(false);
            }

            @Override
            public void onError(UiMessage error) {
                _error.setValue(error);
                _loading.setValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        newsRepository.cancelAll();
        super.onCleared();
    }
}
