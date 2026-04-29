package com.example.myapplication.ui.favorites;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.repository.TourRepository;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class FavoritesViewModel extends ViewModel {

    private final TourRepository tourRepository;
    private final MutableLiveData<List<TourActivity>> _favorites = new MutableLiveData<>();
    public LiveData<List<TourActivity>> favorites = _favorites;
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    public LiveData<UiMessage> error = _error;
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public LiveData<Boolean> loading = _loading;

    @Inject
    public FavoritesViewModel(TourRepository tourRepository) {
        this.tourRepository = tourRepository;
    }

    public void loadFavorites() {
        _loading.setValue(true);
        Log.d("FavoritesViewModel", "Loading favorites...");
        tourRepository.getFavorites(new RepositoryCallback<List<TourActivity>>() {
            @Override
            public void onSuccess(List<TourActivity> data) {
                Log.d("FavoritesViewModel", "Favorites loaded: " + (data != null ? data.size() : 0));
                _favorites.setValue(data != null ? data : new ArrayList<>());
                _loading.setValue(false);
            }

            @Override
            public void onError(UiMessage error) {
                Log.e("FavoritesViewModel", "Error loading favorites");
                _error.setValue(error);
                _loading.setValue(false);
            }
        });
    }

    public void toggleFavorite(long activityId, boolean isFavorite) {
        List<TourActivity> current = _favorites.getValue() != null
                ? new ArrayList<>(_favorites.getValue())
                : new ArrayList<>();

        TourActivity affectedItem = null;
        for (TourActivity item : current) {
            if (item.getId() != null && item.getId() == activityId) {
                affectedItem = item;
                break;
            }
        }
        final TourActivity itemToRestore = affectedItem;
        final boolean previousFavoriteState = !isFavorite;
        final List<TourActivity> rollback = new ArrayList<>(current);

        if (!isFavorite) {
            current.removeIf(item -> item.getId() != null && item.getId() == activityId);
            Log.d("FavoritesViewModel", "Removing favorite: " + activityId);
        }
        _favorites.setValue(current);

        tourRepository.toggleFavorite(activityId, isFavorite, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Log.d("FavoritesViewModel", "Toggle favorite successful, reloading");
                loadFavorites();
            }

            @Override
            public void onError(UiMessage error) {
                Log.e("FavoritesViewModel", "Error toggling favorite");
                if (itemToRestore != null) {
                    itemToRestore.setFavorite(previousFavoriteState);
                }
                _favorites.setValue(rollback);
                _error.setValue(error);
            }
        });
    }
}