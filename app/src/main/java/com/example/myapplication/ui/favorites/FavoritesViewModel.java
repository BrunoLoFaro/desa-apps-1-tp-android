package com.example.myapplication.ui.favorites;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.repository.TourRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<Long, FavoriteSnapshot> lastSnapshotById = new HashMap<>();

    @Inject
    public FavoritesViewModel(TourRepository tourRepository) {
        this.tourRepository = tourRepository;
        loadFavorites();
    }

    public void loadFavorites() {
        _loading.setValue(true);
        Log.d("FavoritesViewModel", "Loading favorites...");
        tourRepository.getFavorites(new RepositoryCallback<List<TourActivity>>() {
            @Override
            public void onSuccess(List<TourActivity> data) {
                Log.d("FavoritesViewModel", "Favorites loaded successfully: " + (data != null ? data.size() : 0) + " items");
                List<TourActivity> safeData = data != null ? data : new ArrayList<>();
                applyChangeFlags(safeData);
                _favorites.setValue(safeData);
                _loading.setValue(false);
            }

            @Override
            public void onError(UiMessage error) {
                Log.e("FavoritesViewModel", "Error loading favorites: " + error.resolve(null));
                _error.setValue(error);
                _loading.setValue(false);
            }
        });
    }

    public void toggleFavorite(long activityId, boolean isFavorite) {
        List<TourActivity> current = _favorites.getValue() != null
                ? new ArrayList<>(_favorites.getValue())
                : new ArrayList<>();
        List<TourActivity> rollback = new ArrayList<>(current);

        if (isFavorite) {
            // No podemos agregar el tour directamente porque no lo tenemos.
            // La recarga es la única opción.
            Log.d("FavoritesViewModel", "Adding favorite: " + activityId);
        } else {
            current.removeIf(item -> item.getId() != null && item.getId() == activityId);
            Log.d("FavoritesViewModel", "Removing favorite: " + activityId);
        }
        _favorites.setValue(current);

        RepositoryCallback<Void> callback = new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Log.d("FavoritesViewModel", "Toggle favorite successful, reloading favorites");
                loadFavorites();
            }

            @Override
            public void onError(UiMessage error) {
                Log.e("FavoritesViewModel", "Error toggling favorite: " + error.resolve(null));
                _favorites.setValue(rollback);
                _error.setValue(error);
            }
        };

        tourRepository.toggleFavorite(activityId, isFavorite, callback);
    }

    private void applyChangeFlags(List<TourActivity> favorites) {
        Map<Long, FavoriteSnapshot> newSnapshotById = new HashMap<>();
        for (TourActivity item : favorites) {
            if (item.getId() == null) continue;
            FavoriteSnapshot previous = lastSnapshotById.get(item.getId());
            boolean priceChanged = previous != null && !safe(item.getPrice()).equals(previous.price);
            boolean slotsChanged = previous != null && item.getAvailableSlots() != previous.slots;
            item.setPriceChanged(priceChanged);
            item.setSlotsChanged(slotsChanged);
            item.setFavoriteUpdate(priceChanged || slotsChanged);
            newSnapshotById.put(item.getId(), new FavoriteSnapshot(safe(item.getPrice()), item.getAvailableSlots()));
        }
        lastSnapshotById.clear();
        lastSnapshotById.putAll(newSnapshotById);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class FavoriteSnapshot {
        final String price;
        final int slots;

        FavoriteSnapshot(String price, int slots) {
            this.price = price;
            this.slots = slots;
        }
    }
}
